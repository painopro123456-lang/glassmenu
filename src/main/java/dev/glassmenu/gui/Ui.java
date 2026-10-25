package dev.glassmenu.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Drawing + animation helpers, including the glass panel look. */
public final class Ui {
    public static int ACCENT = 0x9B7BFF;
    private static int menuHue = 258;
    private static int menuOpacity = 100;
    private static boolean lightTheme;
    private static final int[] HUE_COLORS = createHueColors();

    private Ui() {}

    public static boolean isLightTheme() { return lightTheme; }
    public static void setLightTheme(boolean value) { lightTheme = value; }
    public static void toggleTheme() { lightTheme = !lightTheme; }
    public static int menuHue() { return menuHue; }
    public static int menuOpacity() { return menuOpacity; }
    public static float menuOpacityFraction() { return (menuOpacity - 25) / 75.0f; }
    public static void setMenuHue(int value) {
        menuHue = Math.floorMod(value, 360);
        ACCENT = hueColor(menuHue);
    }
    public static void setMenuOpacity(int value) {
        menuOpacity = Math.max(25, Math.min(100, value));
    }
    public static int primaryText() { return lightTheme ? 0xFF334155 : 0xFFF2F6FB; }
    public static int secondaryText() { return lightTheme ? 0xFF64748B : 0xFF93A1B5; }
    public static int sidebarOverlay() {
        return mulAlpha(lightTheme ? 0x160A1826 : 0x50100B24, menuOpacity / 100.0f);
    }
    public static int dividerColor() { return lightTheme ? 0x33425468 : 0x3B9B7BFF; }

    private static int[] createHueColors() {
        int[] colors = new int[360];
        for (int hue = 0; hue < colors.length; hue++) colors[hue] = calculateHueColor(hue);
        return colors;
    }

    public static int hueColor(int hue) {
        return HUE_COLORS[Math.floorMod(hue, HUE_COLORS.length)];
    }

    private static int calculateHueColor(int hue) {
        float h = hue / 60f;
        int sector = (int) h;
        float x = 1f - Math.abs(h % 2f - 1f);
        float r = 0, gr = 0, b = 0;
        switch (sector) {
            case 0 -> { r = 1; gr = x; }
            case 1 -> { r = x; gr = 1; }
            case 2 -> { gr = 1; b = x; }
            case 3 -> { gr = x; b = 1; }
            case 4 -> { r = x; b = 1; }
            default -> { r = 1; b = x; }
        }
        return ((int) (r * 255) << 16) | ((int) (gr * 255) << 8) | (int) (b * 255);
    }

    /** Compact continuous hue selector used as the menu's color wheel. */
    public static void colorWheel(GuiGraphicsExtractor g, int x, int y, int w, int h, float e) {
        if (w <= 0 || h <= 0) return;
        int segments = Math.min(w, 72);
        for (int i = 0; i < segments; i++) {
            int left = x + i * w / segments;
            int right = x + (i + 1) * w / segments;
            int hue = Math.round(i * 359f / Math.max(1, segments - 1));
            g.fill(left, y, right, y + h, mulAlpha(argb(255, hueColor(hue)), e));
        }
        int markerX = x + Math.round(menuHue / 359f * (w - 1));
        g.fill(markerX - 1, y - 2, markerX + 2, y + h + 2, mulAlpha(0xFFFFFFFF, e));
        g.fill(markerX, y - 1, markerX + 1, y + h + 1, mulAlpha(0xFF17212B, e));
    }

    public static int argb(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }

    public static int mulAlpha(int color, float f) {
        int a = (int) (((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, f)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF, cb = (b >>> shift) & 0xFF;
            out |= ((int) (ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return out;
    }

    public static float smooth(float cur, float target, float dt, float speed) {
        return cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
    }

    public static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    public static float easeOut(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if ((color >>> 24) == 0 || w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { g.fill(x, y, x + w, y + h, color); return; }
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double d = r - i - 0.5;
            int inset = (int) Math.ceil(r - Math.sqrt(r * r - d * d));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    /**
     * Glass panel: layered soft shadow, deep cool tint, a faint accent glow along the top,
     * a bright sheen fading down from the top edge, and a thin light border.
     * The world stays visible through it.
     */
    public static void glass(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, float e) {
        float surface = e * (menuOpacity / 100.0f);
        rect(g, x - 3, y - 3, w + 6, h + 6, r + 3, mulAlpha(0x14000000, surface));
        rect(g, x - 2, y - 2, w + 4, h + 4, r + 2, mulAlpha(0x1E000000, surface));
        rect(g, x - 1, y - 1, w + 2, h + 2, r + 1, mulAlpha(0x2E000000, surface));
        rect(g, x, y, w, h, r, mulAlpha(lightTheme ? 0xE8F1F5FA : 0xB20B1022, surface));
        rect(g, x, y, w, h, r, mulAlpha(lightTheme ? 0x12FFFFFF : 0x0EFFFFFF, surface));
        rect(g, x, y, w, h, r, mulAlpha(argb(lightTheme ? 10 : 26, ACCENT), surface));
        int n = Math.min(60, h / 3);
        for (int i = 0; i < n; i++) {
            float f = 1f - i / (float) n;
            g.fill(x + r, y + 1 + i, x + w - r, y + 2 + i, mulAlpha(argb((int) (30 * f), ACCENT), surface));
        }
        int m = Math.min(24, h / 4);
        for (int i = 0; i < m; i++) {
            int a = (int) (26 * (1f - i / (float) m));
            g.fill(x + r, y + 1 + i, x + w - r, y + 2 + i, mulAlpha(argb(a, 0xFFFFFF), surface));
        }
        int topEdge = lightTheme ? 0xA7FFFFFF : 0xB5B7A7FF;
        int sideEdge = lightTheme ? 0x55738698 : 0x559B7BFF;
        g.fill(x + r, y, x + w - r, y + 1, mulAlpha(topEdge, surface));
        g.fill(x + r, y + h - 1, x + w - r, y + h, mulAlpha(lightTheme ? 0x33425468 : 0x22FFFFFF, surface));
        g.fill(x, y + r, x + 1, y + h - r, mulAlpha(sideEdge, surface));
        g.fill(x + w - 1, y + r, x + w, y + h - r, mulAlpha(lightTheme ? 0x33425468 : 0x22FFFFFF, surface));
    }

    /** Rounded card used for list rows. {@code tint} is any ARGB colour, drawn over a faint base. */
    public static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, float hover, float on, float e) {
        float surface = e * (menuOpacity / 100.0f);
        rect(g, x, y, w, h, 6, mulAlpha(lightTheme ? 0x2AFFFFFF : 0x4A10152E, surface));
        rect(g, x, y, w, h, 6, mulAlpha(argb((int) (lightTheme ? 18 + 20 * hover : 14 + 34 * hover), lightTheme ? 0x263548 : 0x9B7BFF), surface));
        int edge = argb((int) (12 + 18 * hover + 18 * on), lightTheme ? 0x6B7C8F : 0xB9ACFF);
        g.fill(x + 6, y, x + w - 6, y + 1, mulAlpha(edge, surface));
        g.fill(x + 6, y + h - 1, x + w - 6, y + h, mulAlpha(edge, surface));
        if (on > 0.01f) {
            rect(g, x, y, w, h, 6, mulAlpha(argb((int) (22 * on), ACCENT), surface));
            rect(g, x, y + 5, 2, h - 10, 1, mulAlpha(argb((int) (255 * on), ACCENT), surface));
        }
    }

    /** Toggle switch, 30x14. {@code t} is 0 (off) .. 1 (on). */
    public static void toggle(GuiGraphicsExtractor g, int x, int y, float t, float e) {
        int track = lerpColor(0xA0263646, argb(0xF0, ACCENT), t);
        rect(g, x, y, 30, 14, 7, mulAlpha(track, e));
        rect(g, x + 3 + Math.round(t * 16), y + 3, 8, 8, 4, mulAlpha(0xFFFFFFFF, e));
    }

    /** Horizontal slider, 6px track with a round knob. */
    public static void slider(GuiGraphicsExtractor g, int x, int y, int w, float frac, float e) {
        rect(g, x, y, w, 6, 3, mulAlpha(0xA0263646, e));
        int fw = Math.max(6, Math.round(w * clamp01(frac)));
        rect(g, x, y, fw, 6, 3, mulAlpha(argb(0xE6, ACCENT), e));
        rect(g, x + fw - 7, y - 2, 10, 10, 5, mulAlpha(argb(0x44, ACCENT), e));
        rect(g, x + fw - 6, y - 1, 8, 8, 4, mulAlpha(0xFFFFFFFF, e));
    }

    /** Small rounded label, e.g. a version badge. Returns its width. */
    public static int badge(GuiGraphicsExtractor g, Font font, String s, int right, int y, float e) {
        int w = font.width(s) + 10;
        int x = right - w;
        rect(g, x, y, w, 12, 6, mulAlpha(argb(0x38, ACCENT), e));
        text(g, font, s, x + 5, y + 2, mulAlpha(argb(255, ACCENT), e));
        return w;
    }

    /** Thin scrollbar on the right edge of a list. */
    public static void scrollbar(GuiGraphicsExtractor g, int x, int y, int h, float scroll, float maxScroll, float viewH, float contentH, float e) {
        if (maxScroll <= 0.5f) return;
        rect(g, x, y, 3, h, 1, mulAlpha(0x22FFFFFF, e));
        int th = Math.max(16, Math.round(h * viewH / contentH));
        int ty = y + Math.round((h - th) * (scroll / maxScroll));
        rect(g, x, ty, 3, th, 1, mulAlpha(argb(0xB0, ACCENT), e));
    }

    /** Small HUD pill; hue and opacity are independently adjustable per module. */
    public static void pill(GuiGraphicsExtractor g, Font font, int x, int y, String text, int color) {
        pill(g, font, x, y, text, color, 195, 40);
    }

    public static void pill(GuiGraphicsExtractor g, Font font, int x, int y, String text, int color, int hue, int opacity) {
        int w = font.width(text) + 15;
        int alpha = Math.max(0, Math.min(100, opacity)) * 255 / 100;
        int background = argb(alpha, lightTheme ? 0xF7FAFD : 0x10182A);
        rect(g, x, y, w, 14, 5, background);
        g.fill(x + 5, y, x + w - 5, y + 1, lightTheme ? 0x33738698 : 0x44FFFFFF);
        rect(g, x + 3, y + 3, 2, 8, 1, argb(0xFF, hueColor(hue)));
        text(g, font, text, x + 9, y + 3, color);
    }

    public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        if ((color >>> 24) < 8) return;
        g.text(font, s, x, y, color, !lightTheme);
    }
}
