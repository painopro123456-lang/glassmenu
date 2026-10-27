package dev.glassmenu.gui;

import dev.glassmenu.GlassMenu;
import dev.glassmenu.api.Module;
import dev.glassmenu.api.ModuleManager;
import dev.glassmenu.api.NumberSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Transparent glass menu with a sidebar and two tabs:
 *  - Modules: built-in toggles with sliders
 *  - Installed Mods: every mod in your mods folder, listed automatically
 */
public class MenuScreen extends Screen {
    private static final int PW = 900, SIDE = 104, PAD = 10, HEAD = 40, ROW = 48, ROWM = 40, SET_H = 26, TAB_H = 22, MAX_VIEW = 320, GRID_COLS = 5, GRID_GAP = 7;
    private static final float OPEN_S = 0.22f, CLOSE_S = 0.18f;
    private static final String[] TABS = {"Modules", "Installed Mods", "Edit HUD"};

    /** One installed mod, read from its fabric.mod.json. */
    private record Installed(String name, String version, String description) {}

    private static List<Installed> installed;

    private static List<Installed> installedMods() {
        if (installed == null) {
            List<Installed> list = new ArrayList<>();
            for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
                if (c.getContainingMod().isPresent()) continue; // skip bundled sub-mods (e.g. Fabric API parts)
                ModMetadata md = c.getMetadata();
                String id = md.getId();
                if (id.equals("java") || id.equals("minecraft")) continue;
                String desc = md.getDescription() == null ? "" : md.getDescription().replace('\n', ' ').trim();
                list.add(new Installed(md.getName(), md.getVersion().getFriendlyString(), desc));
            }
            list.sort(Comparator.comparing(m -> m.name().toLowerCase()));
            installed = list;
        }
        return installed;
    }

    private final Screen parent;
    private final List<Module> mods = ModuleManager.get().all().stream()
            .sorted(Comparator.comparingInt((Module module) -> module.category().ordinal())
                    .thenComparing(module -> module.name.toLowerCase()))
            .toList();
    private final List<Installed> inst = installedMods();
    private final float[] sw = new float[mods.size()];
    private final float[] hov = new float[mods.size()];
    private final float[] instHov = new float[inst.size()];
    private final float[] expand = new float[mods.size()];
    private final float[] rowOffsets = new float[mods.size()];
    private final boolean[] open = new boolean[mods.size()];
    private final float[] tabAnim = new float[TABS.length];

    private int tab;
    private long openAt, closeAt, last;
    private boolean closing;
    private float scroll, scrollTarget;
    private NumberSetting dragging;
    private boolean draggingMenuHue, draggingMenuOpacity;
    private int dragX, dragW;

    public MenuScreen(Screen parent) {
        super(Component.literal("Moon Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        openAt = last = System.nanoTime();
        for (int i = 0; i < mods.size(); i++) sw[i] = mods.get(i).isEnabled() ? 1f : 0f;
        rebuildRowOffsets();
        tabAnim[tab] = 1f;
    }

    // ---- layout -------------------------------------------------------
    private float rowH(int row) {
        float extra = 0f;
        int start = row * GRID_COLS, end = Math.min(mods.size(), start + GRID_COLS);
        for (int i = start; i < end; i++) extra = Math.max(extra, expand[i] * mods.get(i).settings.size() * SET_H);
        return ROW + extra;
    }

    private void rebuildRowOffsets() {
        float offset = 0f;
        for (int row = 0; row < (mods.size() + GRID_COLS - 1) / GRID_COLS; row++) {
            int start = row * GRID_COLS, end = Math.min(mods.size(), start + GRID_COLS);
            for (int i = start; i < end; i++) rowOffsets[i] = offset;
            offset += rowH(row);
        }
    }

    private float contentH() {
        if (tab == 1) return inst.size() * ROWM;
        float h = 0f;
        for (int row = 0; row < (mods.size() + GRID_COLS - 1) / GRID_COLS; row++) h += rowH(row);
        return h;
    }

    private int viewH() { return Math.max(70, Math.min(MAX_VIEW, height - 16 - HEAD - PAD)); }
    private int panelH() { return HEAD + viewH() + PAD; }
    private int px() { return (width - PW) / 2; }
    private int py() { return Math.max(4, (height - panelH()) / 2); }
    private float maxScroll() { return Math.max(0, contentH() - viewH()); }

    private int cx() { return px() + SIDE; }                 // content area left edge
    private int lx() { return cx() + PAD; }                  // content left edge
    private int lw() { return PW - SIDE - PAD * 2 - 6; }     // full content width
    private int cardW() { return (lw() - GRID_GAP * (GRID_COLS - 1)) / GRID_COLS; }
    private int cardX(int i) { return lx() + (i % GRID_COLS) * (cardW() + GRID_GAP); }
    private int toggleX(int i) { return cardX(i) + cardW() - 38; }

    private float rowTop(int i) {
        float y = py() + HEAD - scroll;
        return tab == 1 ? y + i * ROWM : y + rowOffsets[i];
    }

    private int categoryCount(Module.Category category) {
        int count = 0;
        for (Module module : mods) if (module.category() == category) count++;
        return count;
    }

    private int tabY(int i) { return py() + 46 + i * (TAB_H + 2); }

    private String fit(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "...") > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    // ---- rendering ----------------------------------------------------
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // A light touch of contrast keeps the glass readable without hiding the game.
        g.fill(0, 0, width, height, Ui.isLightTheme() ? 0x0C26384A : 0x18000000);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        if (closing && (System.nanoTime() - closeAt) / 1e9f >= CLOSE_S) {
            ModuleManager.get().save();
            minecraft.setScreenAndShow(parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;

        float p = closing ? 1f - (now - closeAt) / 1e9f / CLOSE_S : (now - openAt) / 1e9f / OPEN_S;
        float e = Ui.easeOut(p);
        if (e <= 0f) return;

        for (int i = 0; i < mods.size(); i++) {
            sw[i] = Ui.smooth(sw[i], mods.get(i).isEnabled() ? 1f : 0f, dt, 16f);
            expand[i] = Ui.smooth(expand[i], open[i] ? 1f : 0f, dt, 14f);
        }
        rebuildRowOffsets();
        for (int t = 0; t < TABS.length; t++) tabAnim[t] = Ui.smooth(tabAnim[t], t == tab ? 1f : 0f, dt, 16f);
        scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget));
        scroll = Ui.smooth(scroll, scrollTarget, dt, 18f);
        scroll = Math.max(0, Math.min(maxScroll(), scroll));

        int yOff = (int) ((1f - e) * 12);
        int x = px(), y = py() + yOff, h = panelH();
        Ui.glass(g, x, y, PW, h, 10, e);

        // sidebar: darker strip with a divider
        int side = Ui.mulAlpha(Ui.sidebarOverlay(), e);
        g.fill(x + 8, y + 1, x + SIDE, y + h - 1, side);
        g.fill(x + 1, y + 8, x + 8, y + h - 8, side);
        g.fill(x + SIDE, y + 8, x + SIDE + 1, y + h - 8, Ui.mulAlpha(Ui.dividerColor(), e));

        Ui.text(g, font, "Moon Menu", x + 14, y + 14, Ui.mulAlpha(Ui.argb(255, Ui.ACCENT), e));
        g.fill(x + 14, y + 29, x + SIDE - 14, y + 30, Ui.mulAlpha(0x22FFFFFF, e));
        Ui.text(g, font, "FABRIC 26.2", x + 14, y + 34, Ui.mulAlpha(Ui.secondaryText(), e));

        for (int t = 0; t < TABS.length; t++) {
            int tx = x + 8, ty = tabY(t) + yOff, tw = SIDE - 16;
            Ui.rect(g, tx, ty, tw, TAB_H, 6, Ui.mulAlpha(Ui.argb((int) (46 * tabAnim[t]), Ui.ACCENT), e));
            Ui.rect(g, tx, ty + 5, 2, TAB_H - 10, 1, Ui.mulAlpha(Ui.argb((int) (255 * tabAnim[t]), Ui.ACCENT), e));
            int col = Ui.lerpColor(Ui.secondaryText(), Ui.primaryText(), tabAnim[t]);
            Ui.text(g, font, TABS[t], tx + 10, ty + 7, Ui.mulAlpha(col, e));
        }

        String hex = String.format("#%06X", Ui.ACCENT & 0xFFFFFF);
        Ui.text(g, font, "Menu color", x + 14, y + h - 107, Ui.mulAlpha(Ui.primaryText(), e));
        Ui.text(g, font, hex, x + SIDE - 14 - font.width(hex), y + h - 107,
                Ui.mulAlpha(Ui.secondaryText(), e));
        Ui.colorWheel(g, x + 14, y + h - 94, SIDE - 28, 8, e);
        String opacity = "Opacity";
        String opacityValue = Ui.menuOpacity() + "%";
        Ui.text(g, font, opacity, x + 14, y + h - 76, Ui.mulAlpha(Ui.primaryText(), e));
        Ui.text(g, font, opacityValue, x + SIDE - 14 - font.width(opacityValue), y + h - 76,
                Ui.mulAlpha(Ui.secondaryText(), e));
        Ui.slider(g, x + 14, y + h - 62, SIDE - 28, Ui.menuOpacityFraction(), e);

        String key = GlassMenu.menuKey.getTranslatedKeyMessage().getString();
        Ui.rect(g, x + 12, y + h - 50, 80, 18, 5,
                Ui.mulAlpha(Ui.isLightTheme() ? 0xD9DDE7EF : 0x55334558, e * Ui.menuOpacity() / 100.0f));
        Ui.text(g, font, Ui.isLightTheme() ? "Theme: Light" : "Theme: Dark",
                x + 17, y + h - 44, Ui.mulAlpha(Ui.primaryText(), e));
        Ui.text(g, font, fit(key, SIDE - 24), x + 14, y + h - 28, Ui.mulAlpha(Ui.secondaryText(), e));
        Ui.text(g, font, "or Esc to close", x + 14, y + h - 17, Ui.mulAlpha(Ui.secondaryText(), e));

        // content header
        Ui.text(g, font, TABS[tab], cx() + PAD, y + 14, Ui.mulAlpha(Ui.primaryText(), e));
        String cnt = tab == 0 ? mods.size() + " modules" : inst.size() + " mods";
        Ui.badge(g, font, cnt, x + PW - PAD, y + 12, e);
        g.fill(cx() + PAD, y + HEAD - 6, x + PW - PAD, y + HEAD - 5, Ui.mulAlpha(0x22FFFFFF, e));

        // list (clipped)
        int vy = y + HEAD, vh = viewH();
        g.enableScissor(cx() + 1, vy, x + PW - 2, vy + vh);
        if (tab == 0) drawModules(g, mx, my, dt, e, yOff, vy, vh);
        else drawInstalled(g, mx, my, dt, e, yOff, vy, vh);
        g.disableScissor();

        if (maxScroll() > 0) Ui.scrollbar(g, x + PW - 8, vy, vh, scroll, maxScroll(), vh, contentH(), e);
    }

    private boolean inView(int mx, int my) {
        return my >= py() + HEAD && my < py() + HEAD + viewH() && mx >= cx() && mx < px() + PW;
    }

    private void drawModules(GuiGraphicsExtractor g, int mx, int my, float dt, float e, int yOff, int vy, int vh) {
        int cardWidth = cardW();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int cardLeft = cardX(i);
            int tx = toggleX(i);
            int ry = Math.round(rowTop(i)) + yOff;
            int cardHeight = ROW - 4;
            if (ry > vy + vh || ry + rowH(i / GRID_COLS) < vy) continue;

            boolean over = !closing && inView(mx, my) && mx >= cardLeft && mx < cardLeft + cardWidth
                    && my >= ry - yOff && my < ry - yOff + cardHeight;
            hov[i] = Ui.smooth(hov[i], over ? 1f : 0f, dt, 18f);

            Ui.card(g, cardLeft, ry, cardWidth, cardHeight, hov[i], sw[i], e);
            Ui.text(g, font, fit(m.name, cardWidth - 48), cardLeft + 7, ry + 6, Ui.mulAlpha(Ui.primaryText(), e));
            Ui.text(g, font, fit(m.description, cardWidth - 14), cardLeft + 7, ry + 20, Ui.mulAlpha(Ui.secondaryText(), e));
            Ui.toggle(g, tx, ry + 7, sw[i], e);

            if (!m.settings.isEmpty()) {
                int col = Ui.lerpColor(0xFF8794A8, 0xFFFFFFFF, Math.max(hov[i], expand[i]));
                Ui.text(g, font, open[i] ? "v" : ">", tx - 13, ry + 25, Ui.mulAlpha(col, e));
            }

            if (expand[i] > 0.02f) {
                float ea = expand[i] * e;
                for (int k = 0; k < m.settings.size(); k++) {
                    NumberSetting setting = m.settings.get(k);
                    int sy = ry + ROW + k * SET_H;
                    Ui.text(g, font, fit(setting.label, cardWidth - 48), cardLeft + 7, sy + 1, Ui.mulAlpha(Ui.primaryText(), ea));
                    String value = setting.display();
                    Ui.text(g, font, value, cardLeft + cardWidth - 7 - font.width(value), sy + 1,
                            Ui.mulAlpha(Ui.argb(255, Ui.ACCENT), ea));
                    Ui.slider(g, cardLeft + 7, sy + 15, cardWidth - 14, setting.fraction(), ea);
                }
            }
        }
    }

    private void drawInstalled(GuiGraphicsExtractor g, int mx, int my, float dt, float e, int yOff, int vy, int vh) {
        int lx = lx(), lw = lw();
        for (int i = 0; i < inst.size(); i++) {
            Installed m = inst.get(i);
            int ry = Math.round(rowTop(i)) + yOff;
            if (ry > vy + vh || ry + ROWM < vy) continue;

            boolean over = !closing && inView(mx, my) && mx >= lx && mx < lx + lw
                    && my >= ry - yOff && my < ry - yOff + ROWM - 4;
            instHov[i] = Ui.smooth(instHov[i], over ? 1f : 0f, dt, 18f);

            Ui.card(g, lx, ry, lw, ROWM - 4, instHov[i], 0f, e);
            int bw = Ui.badge(g, font, "v" + m.version(), lx + lw - 8, ry + 7, e);
            Ui.text(g, font, fit(m.name(), lw - 36 - bw), lx + 10, ry + 8, Ui.mulAlpha(Ui.primaryText(), e));
            String desc = m.description().isEmpty() ? "No description" : m.description();
            Ui.text(g, font, fit(desc, lw - 20), lx + 10, ry + 21, Ui.mulAlpha(Ui.secondaryText(), e));
        }
    }

    // ---- input --------------------------------------------------------
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing || event.button() != 0) return true;
        double mx = event.x(), my = event.y();
        int x = px(), y = py();

        if (mx < x || mx > x + PW || my < y || my > y + panelH()) { requestClose(); return true; }

        int themeTop = y + panelH() - 50;
        if (mx >= x + 12 && mx <= x + 92 && my >= themeTop && my <= themeTop + 18) {
            Ui.toggleTheme();
            ModuleManager.get().save();
            return true;
        }

        int pickerX = x + 14, pickerW = SIDE - 28;
        int pickerTop = y + panelH() - 94;
        if (mx >= pickerX && mx < pickerX + pickerW && my >= pickerTop && my < pickerTop + 8) {
            draggingMenuHue = true;
            updateMenuHue(mx);
            return true;
        }
        int opacityTop = y + panelH() - 62;
        if (mx >= pickerX && mx <= pickerX + pickerW && my >= opacityTop - 2 && my <= opacityTop + 8) {
            draggingMenuOpacity = true;
            updateMenuOpacity(mx);
            return true;
        }

        // sidebar tabs
        for (int t = 0; t < TABS.length; t++) {
            if (mx >= x + 8 && mx < x + SIDE - 8 && my >= tabY(t) && my < tabY(t) + TAB_H) {
                if (t == 2) {
                    minecraft.setScreenAndShow(new HudEditorScreen(this));
                    return true;
                }
                if (t != tab) { tab = t; scroll = scrollTarget = 0; dragging = null; }
                return true;
            }
        }
        if (tab == 1 || mx < cx() || my < y + HEAD || my > y + HEAD + viewH()) return true;

        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int cardLeft = cardX(i), cardWidth = cardW(), tx = toggleX(i);
            float ry = rowTop(i);
            if (my >= ry && my < ry + ROW - 4 && mx >= cardLeft && mx < cardLeft + cardWidth) {
                if (!m.settings.isEmpty() && mx >= tx - 18 && mx < tx - 2) {
                    open[i] = !open[i];
                } else {
                    m.toggle();
                    ModuleManager.get().save();
                }
                return true;
            }
            if (expand[i] > 0.5f) {
                for (int k = 0; k < m.settings.size(); k++) {
                    float sy = ry + ROW + k * SET_H;
                    if (my >= sy + 10 && my <= sy + SET_H && mx >= cardLeft + 4 && mx <= cardLeft + cardWidth - 4) {
                        dragging = m.settings.get(k);
                        dragX = cardLeft + 7;
                        dragW = cardWidth - 14;
                        updateSlider(mx);
                        return true;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingMenuHue) updateMenuHue(event.x());
        else if (draggingMenuOpacity) updateMenuOpacity(event.x());
        else if (dragging != null) updateSlider(event.x());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingMenuHue || draggingMenuOpacity) {
            draggingMenuHue = draggingMenuOpacity = false;
            ModuleManager.get().save();
        }
        if (dragging != null) { dragging = null; ModuleManager.get().save(); }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget - (float) scrollY * 24f));
        return true;
    }

    private void updateSlider(double mx) {
        float frac = Ui.clamp01((float) ((mx - dragX) / dragW));
        dragging.set(dragging.min + frac * (dragging.max - dragging.min));
    }

    private void updateMenuHue(double mx) {
        float frac = Ui.clamp01((float) ((mx - (px() + 14)) / (SIDE - 28.0)));
        Ui.setMenuHue(Math.round(frac * 359));
    }

    private void updateMenuOpacity(double mx) {
        float frac = Ui.clamp01((float) ((mx - (px() + 14)) / (SIDE - 28.0)));
        Ui.setMenuOpacity(25 + Math.round(frac * 75));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (GlassMenu.menuKey.matches(event)) { requestClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() { requestClose(); }

    private void requestClose() {
        if (closing) return;
        closing = true;
        closeAt = System.nanoTime();
    }
}
