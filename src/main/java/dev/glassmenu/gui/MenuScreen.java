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
 * Transparent glass menu with two tabs:
 *  - Modules: built-in toggles with sliders
 *  - Installed Mods: every mod in your mods folder, listed automatically
 */
public class MenuScreen extends Screen {
    private static final int PW = 280, HEAD = 56, ROW = 28, ROWM = 30, SET_H = 26, FOOT = 24;
    private static final float OPEN_S = 0.22f, CLOSE_S = 0.14f;
    private static final String[] TABS = {"Modules", "Installed Mods"};

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
        super(Component.literal("Glass Menu"));
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

    private int viewH() { return (int) Math.min(Math.max(contentH(), 40), Math.max(80, height - 110)); }
    private int panelH() { return HEAD + viewH() + FOOT; }
    private int px() { return (width - PW) / 2; }
    private int py() { return Math.max(4, (height - panelH()) / 2); }
    private float maxScroll() { return Math.max(0, contentH() - viewH()); }

    private float rowTop(int i) {
        float y = py() + HEAD - scroll;
        if (tab == 1) return y + i * ROWM;
        for (int j = 0; j < i; j++) y += rowH(j);
        return y;
    }

    private int tabX(int i) {
        int x = px() + 14;
        for (int j = 0; j < i; j++) x += font.width(TABS[j]) + 16 + 6;
        return x;
    }

    private int tabW(int i) { return font.width(TABS[i]) + 16; }

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
        Ui.glass(g, x, y, PW, h, 8, e);

        // header + tabs
        Ui.text(g, font, "Glass Menu", x + 14, y + 10, Ui.mulAlpha(Ui.argb(255, Ui.ACCENT), e));
        String cnt = tab == 0 ? mods.size() + " modules" : inst.size() + " mods";
        Ui.text(g, font, cnt, x + PW - 14 - font.width(cnt), y + 10, Ui.mulAlpha(0xFFA9B6C8, e));
        for (int t = 0; t < TABS.length; t++) {
            int tx = tabX(t), tw = tabW(t), ty = y + 26;
            Ui.rect(g, tx, ty, tw, 16, 6, Ui.mulAlpha(Ui.argb((int) (70 * tabAnim[t]), Ui.ACCENT), e));
            int col = Ui.lerpColor(0xFFA9B6C8, 0xFFFFFFFF, tabAnim[t]);
            Ui.text(g, font, TABS[t], tx + 8, ty + 4, Ui.mulAlpha(col, e));
        }
        g.fill(x + 14, y + HEAD - 6, x + PW - 14, y + HEAD - 5, Ui.mulAlpha(0x33FFFFFF, e));

        // list (clipped)
        int vy = y + HEAD, vh = viewH();
        g.enableScissor(x + 2, vy, x + PW - 2, vy + vh);
        if (tab == 0) drawModules(g, mx, my, dt, e, x, yOff, vy, vh);
        else drawInstalled(g, mx, my, dt, e, x, yOff, vy, vh);
        g.disableScissor();

        String hint = GlassMenu.menuKey.getTranslatedKeyMessage().getString() + " or Esc to close";
        Ui.text(g, font, hint, x + (PW - font.width(hint)) / 2, y + h - 16, Ui.mulAlpha(0xFF7D8BA0, e));
    }

    private void drawModules(GuiGraphicsExtractor g, int mx, int my, float dt, float e, int x, int yOff, int vy, int vh) {
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = Math.round(rowTop(i)) + yOff;
            if (ry > vy + vh || ry + rowH(i) < vy) continue;

            boolean over = !closing && mx >= x + 6 && mx < x + PW - 6 && my >= ry - yOff && my < ry - yOff + ROW - 2
                    && my >= py() + HEAD && my < py() + HEAD + viewH();
            hov[i] = Ui.smooth(hov[i], over ? 1f : 0f, dt, 18f);

            Ui.rect(g, x + 6, ry, PW - 12, ROW - 2, 5, Ui.mulAlpha(Ui.argb((int) (40 * hov[i]), 0xFFFFFF), e));
            Ui.text(g, font, m.name, x + 14, ry + 5, Ui.mulAlpha(0xFFF2F6FB, e));
            Ui.text(g, font, fit(m.description, PW - 100), x + 14, ry + 15, Ui.mulAlpha(0xFF93A1B5, e));

            int tx = x + PW - 16 - 26, ty = ry + (ROW - 2 - 12) / 2;
            int track = Ui.lerpColor(0x90304052, Ui.argb(0xE0, Ui.ACCENT), sw[i]);
            Ui.rect(g, tx, ty, 26, 12, 6, Ui.mulAlpha(track, e));
            Ui.rect(g, tx + 2 + Math.round(sw[i] * 14), ty + 2, 8, 8, 4, Ui.mulAlpha(0xFFFFFFFF, e));

            if (!m.settings.isEmpty()) {
                Ui.text(g, font, open[i] ? "v" : ">", tx - 14, ry + 9, Ui.mulAlpha(0xFFC8D4E3, e));
            }

            if (expand[i] > 0.02f) {
                float ea = expand[i] * e;
                for (int k = 0; k < m.settings.size(); k++) {
                    NumberSetting s = m.settings.get(k);
                    int sy = ry + ROW + k * SET_H;
                    Ui.text(g, font, s.label, x + 22, sy + 1, Ui.mulAlpha(0xFFDCE4EE, ea));
                    String v = s.display();
                    Ui.text(g, font, v, x + PW - 22 - font.width(v), sy + 1, Ui.mulAlpha(Ui.argb(255, Ui.ACCENT), ea));
                    int sx = x + 22, sw2 = PW - 44, sty = sy + 14;
                    Ui.rect(g, sx, sty, sw2, 4, 2, Ui.mulAlpha(0x90304052, ea));
                    int fw = Math.max(4, Math.round(sw2 * Ui.clamp01(s.fraction())));
                    Ui.rect(g, sx, sty, fw, 4, 2, Ui.mulAlpha(Ui.argb(0xE0, Ui.ACCENT), ea));
                    Ui.rect(g, sx + fw - 4, sty - 2, 8, 8, 4, Ui.mulAlpha(0xFFFFFFFF, ea));
                }
            }
        }
    }

    private void drawInstalled(GuiGraphicsExtractor g, int mx, int my, float dt, float e, int x, int yOff, int vy, int vh) {
        for (int i = 0; i < inst.size(); i++) {
            Installed m = inst.get(i);
            int ry = Math.round(rowTop(i)) + yOff;
            if (ry > vy + vh || ry + ROWM < vy) continue;

            boolean over = !closing && mx >= x + 6 && mx < x + PW - 6 && my >= ry - yOff && my < ry - yOff + ROWM - 2
                    && my >= py() + HEAD && my < py() + HEAD + viewH();
            instHov[i] = Ui.smooth(instHov[i], over ? 1f : 0f, dt, 18f);

            Ui.rect(g, x + 6, ry, PW - 12, ROWM - 2, 5, Ui.mulAlpha(Ui.argb((int) (40 * instHov[i]), 0xFFFFFF), e));
            String ver = "v" + m.version();
            int verW = font.width(ver);
            Ui.text(g, font, fit(m.name(), PW - 40 - verW), x + 14, ry + 6, Ui.mulAlpha(0xFFF2F6FB, e));
            Ui.text(g, font, ver, x + PW - 14 - verW, ry + 6, Ui.mulAlpha(Ui.argb(255, Ui.ACCENT), e));
            String desc = m.description().isEmpty() ? "No description" : m.description();
            Ui.text(g, font, fit(desc, PW - 28), x + 14, ry + 17, Ui.mulAlpha(0xFF93A1B5, e));
        }
    }

    // ---- input --------------------------------------------------------
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing || event.button() != 0) return true;
        double mx = event.x(), my = event.y();
        int x = px(), y = py();

        if (mx < x || mx > x + PW || my < y || my > y + panelH()) { requestClose(); return true; }

        // tabs
        for (int t = 0; t < TABS.length; t++) {
            if (mx >= tabX(t) && mx < tabX(t) + tabW(t) && my >= y + 26 && my < y + 42) {
                if (t != tab) { tab = t; scroll = scrollTarget = 0; dragging = null; }
                return true;
            }
        }
        if (tab == 1 || my < y + HEAD || my > y + HEAD + viewH()) return true;

        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            float ry = rowTop(i);
            if (my >= ry && my < ry + ROW - 2 && mx >= x + 6 && mx < x + PW - 6) {
                int arrowX = x + PW - 16 - 26 - 18;
                if (!m.settings.isEmpty() && mx >= arrowX && mx < arrowX + 16) {
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
                    if (my >= sy + 8 && my <= sy + SET_H && mx >= x + 16 && mx <= x + PW - 16) {
                        dragging = m.settings.get(k);
                        dragX = x + 22;
                        dragW = PW - 44;
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
