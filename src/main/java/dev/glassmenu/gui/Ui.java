package dev.glassmenu.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Drawing + animation helpers, including the glass panel look. */
public final class Ui {
    public static final int ACCENT = 0x6FD3FF;

    private Ui() {}

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
     * Glass panel: soft shadow, cool translucent tint, bright sheen fading down from the top
     * edge, and a thin light border. The world stays visible through it.
     */
    public static void glass(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, float e) {
        rect(g, x - 2, y - 2, w + 4, h + 4, r + 2, mulAlpha(0x1E000000, e));
        rect(g, x - 1, y - 1, w + 2, h + 2, r + 1, mulAlpha(0x2E000000, e));
        rect(g, x, y, w, h, r, mulAlpha(0x6616283A, e));
        rect(g, x, y, w, h, r, mulAlpha(0x12FFFFFF, e));
        int n = Math.min(44, h / 3);
        for (int i = 0; i < n; i++) {
            int a = (int) (38 * (1f - i / (float) n));
            g.fill(x + r, y + 1 + i, x + w - r, y + 2 + i, mulAlpha(argb(a, 0xFFFFFF), e));
        }
        g.fill(x + r, y, x + w - r, y + 1, mulAlpha(0x88FFFFFF, e));
        g.fill(x + r, y + h - 1, x + w - r, y + h, mulAlpha(0x22FFFFFF, e));
        g.fill(x, y + r, x + 1, y + h - r, mulAlpha(0x44FFFFFF, e));
        g.fill(x + w - 1, y + r, x + w, y + h - r, mulAlpha(0x22FFFFFF, e));
    }

    /** Small glass pill used for HUD lines. */
    public static void pill(GuiGraphicsExtractor g, Font font, int x, int y, String text, int color) {
        int w = font.width(text) + 10;
        rect(g, x, y, w, 14, 5, 0x55101C28);
        g.fill(x + 4, y, x + w - 4, y + 1, 0x55FFFFFF);
        text(g, font, text, x + 5, y + 3, color);
    }

    public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        if ((color >>> 24) < 8) return;
        g.text(font, s, x, y, color, true);
    }
}
