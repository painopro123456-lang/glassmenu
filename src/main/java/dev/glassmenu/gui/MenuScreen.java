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
    private static final int PW = 400, SIDE = 104, PAD = 10, HEAD = 40, ROW = 34, ROWM = 40, SET_H = 26, TAB_H = 22, MAX_VIEW = 216;
    private static final float OPEN_S = 0.22f, CLOSE_S = 0.14f;
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
    private final List<Module> mods = ModuleManager.get().all();
    private final List<Installed> inst = installedMods();
    private final float[] sw = new float[mods.size()];
    private final float[] hov = new float[mods.size()];
    private final float[] instHov = new float[inst.size()];
    private final float[] expand = new float[mods.size()];
    private final boolean[] open = new boolean[mods.size()];
    private final float[] tabAnim = new float[TABS.length];

    private int tab;
    private long openAt, closeAt, last;
    private boolean closing;
    private float scroll, scrollTarget;
    private NumberSetting dragging;
    private int dragX, dragW;

    public MenuScreen(Screen parent) {
        super(Component.literal("Moon Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        openAt = last = System.nanoTime();
        for (int i = 0; i < mods.size(); i++) sw[i] = mods.get(i).isEnabled() ? 1f : 0f;
        tabAnim[tab] = 1f;
    }

    // ---- layout -------------------------------------------------------
    private float rowH(int i) { return ROW + expand[i] * mods.get(i).settings.size() * SET_H; }

    private float contentH() {
        if (tab == 1) return inst.size() * ROWM;
        float h = 0;
        for (int i = 0; i < mods.size(); i++) h += rowH(i);
        return h;
    }

    private int viewH() { return Math.max(70, Math.min(MAX_VIEW, height - 16 - HEAD - PAD)); }
    private int panelH() { return HEAD + viewH() + PAD; }
    private int px() { return (width - PW) / 2; }
    private int py() { return Math.max(4, (height - panelH()) / 2); }
    private float maxScroll() { return Math.max(0, contentH() - viewH()); }

    private int cx() { return px() + SIDE; }                 // content area left edge
    private int lx() { return cx() + PAD; }                  // row left edge
    private int lw() { return PW - SIDE - PAD * 2 - 6; }     // row width (room for scrollbar)
    private int toggleX() { return lx() + lw() - 8 - 30; }

    private float rowTop(int i) {
        float y = py() + HEAD - scroll;
        if (tab == 1) return y + i * ROWM;
        for (int j = 0; j < i; j++) y += rowH(j);
        return y;
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
        // intentionally empty: no blur, no dimming -> the glass sits directly over the game
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
        for (int t = 0; t < TABS.length; t++) tabAnim[t] = Ui.smooth(tabAnim[t], t == tab ? 1f : 0f, dt, 16f);
        scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget));
        scroll = Ui.smooth(scroll, scrollTarget, dt, 18f);
        scroll = Math.max(0, Math.min(maxScroll(), scroll));

        int yOff = (int) ((1f - e) * 12);
        int x = px(), y = py() + yOff, h = panelH();
        Ui.glass(g, x, y, PW, h, 10, e);

        // sidebar: darker strip with a divider
        int side = Ui.mulAlpha(0x40000000, e);
        g.fill(x + 8, y + 1, x + SIDE, y + h - 1, side);
        g.fill(x + 1, y + 8, x + 8, y + h - 8, side);
        g.fill(x + SIDE, y + 8, x + SIDE + 1, y + h - 8, Ui.mulAlpha(0x2EFFFFFF, e));

        Ui.text(g, font, "Moon Menu", x + 14, y + 14, Ui.mulAlpha(Ui.argb(255, Ui.ACCENT), e));
        g.fill(x + 14, y + 29, x + SIDE - 14, y + 30, Ui.mulAlpha(0x22FFFFFF, e));

        for (int t = 0; t < TABS.length; t++) {
            int tx = x + 8, ty = tabY(t) + yOff, tw = SIDE - 16;
            Ui.rect(g, tx, ty, tw, TAB_H, 6, Ui.mulAlpha(Ui.argb((int) (46 * tabAnim[t]), Ui.ACCENT), e));
            Ui.rect(g, tx, ty + 5, 2, TAB_H - 10, 1, Ui.mulAlpha(Ui.argb((int) (255 * tabAnim[t]), Ui.ACCENT), e));
            int col = Ui.lerpColor(0xFFA9B6C8, 0xFFFFFFFF, tabAnim[t]);
            Ui.text(g, font, TABS[t], tx + 10, ty + 7, Ui.mulAlpha(col, e));
        }

        String key = GlassMenu.menuKey.getTranslatedKeyMessage().getString();
        Ui.text(g, font, fit(key, SIDE - 24), x + 14, y + h - 28, Ui.mulAlpha(0xFFC8D4E3, e));
        Ui.text(g, font, "or Esc to close", x + 14, y + h - 17, Ui.mulAlpha(0xFF7D8BA0, e));

        // content header
        Ui.text(g, font, TABS[tab], cx() + PAD, y + 14, Ui.mulAlpha(0xFFF2F6FB, e));
        String cnt = tab == 0 ? mods.size() + " modules" : inst.size() + " mods";
        Ui.badge(g, font, cnt, x + PW - PAD, y + 12, e);
        g.fill(cx() + PAD, y + HEAD - 6, x + PW - PAD, y + HEAD - 5, Ui.mulAlpha(0x22FFFFFF, e));

        // list (clipped)
        int vy = y + HEAD, vh = viewH();
        g.enableScissor(cx() + 1, vy, x + PW - 2, vy + vh);
        if (tab == 0) drawModules(g, mx, my, dt, e, yOff, vy, vh);
        else drawInstalled(g, mx, my, dt, e, yOff, vy, vh);
        g.disableScissor();

        Ui.scrollbar(g, x + PW - 8, vy, vh, scroll, maxScroll(), vh, contentH(), e);
    }

    private boolean inView(int mx, int my) {
        return my >= py() + HEAD && my < py() + HEAD + viewH() && mx >= cx() && mx < px() + PW;
    }

    private void drawModules(GuiGraphicsExtractor g, int mx, int my, float dt, float e, int yOff, int vy, int vh) {
        int lx = lx(), lw = lw(), tx = toggleX();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = Math.round(rowTop(i)) + yOff;
            if (ry > vy + vh || ry + rowH(i) < vy) continue;

            boolean over = !closing && inView(mx, my) && mx >= lx && mx < lx + lw
                    && my >= ry - yOff && my < ry - yOff + ROW - 4;
            hov[i] = Ui.smooth(hov[i], over ? 1f : 0f, dt, 18f);

            Ui.card(g, lx, ry, lw, ROW - 4, hov[i], sw[i], e);
            Ui.text(g, font, m.name, lx + 10, ry + 6, Ui.mulAlpha(0xFFF2F6FB, e));
            Ui.text(g, font, fit(m.description, lw - 90), lx + 10, ry + 17, Ui.mulAlpha(0xFF93A1B5, e));

            Ui.toggle(g, tx, ry + (ROW - 4 - 14) / 2, sw[i], e);

            if (!m.settings.isEmpty()) {
                int col = Ui.lerpColor(0xFF8794A8, 0xFFFFFFFF, Math.max(hov[i], expand[i]));
                Ui.text(g, font, open[i] ? "v" : ">", tx - 14, ry + 11, Ui.mulAlpha(col, e));
            }

            if (expand[i] > 0.02f) {
                float ea = expand[i] * e;
                for (int k = 0; k < m.settings.size(); k++) {
                    NumberSetting s = m.settings.get(k);
                    int sy = ry + ROW + k * SET_H;
                    Ui.text(g, font, s.label, lx + 14, sy + 1, Ui.mulAlpha(0xFFDCE4EE, ea));
                    String v = s.display();
                    Ui.text(g, font, v, lx + lw - 14 - font.width(v), sy + 1, Ui.mulAlpha(Ui.argb(255, Ui.ACCENT), ea));
                    Ui.slider(g, lx + 14, sy + 15, lw - 28, s.fraction(), ea);
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
            Ui.text(g, font, fit(m.name(), lw - 36 - bw), lx + 10, ry + 8, Ui.mulAlpha(0xFFF2F6FB, e));
            String desc = m.description().isEmpty() ? "No description" : m.description();
            Ui.text(g, font, fit(desc, lw - 20), lx + 10, ry + 21, Ui.mulAlpha(0xFF93A1B5, e));
        }
    }

    // ---- input --------------------------------------------------------
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing || event.button() != 0) return true;
        double mx = event.x(), my = event.y();
        int x = px(), y = py();

        if (mx < x || mx > x + PW || my < y || my > y + panelH()) { requestClose(); return true; }

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

        int lx = lx(), lw = lw(), tx = toggleX();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            float ry = rowTop(i);
            if (my >= ry && my < ry + ROW - 4 && mx >= lx && mx < lx + lw) {
                if (!m.settings.isEmpty() && mx >= tx - 22 && mx < tx - 4) {
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
                    if (my >= sy + 10 && my <= sy + SET_H && mx >= lx + 6 && mx <= lx + lw - 6) {
                        dragging = m.settings.get(k);
                        dragX = lx + 14;
                        dragW = lw - 28;
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
        if (dragging != null) updateSlider(event.x());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
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
