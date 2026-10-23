package dev.glassmenu.modules;

import dev.glassmenu.api.Module;
import dev.glassmenu.api.NumberSetting;
import dev.glassmenu.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Applies an animated translucent rainbow tint across the player's world view. */
public final class RainbowWorldModule extends Module {
    private final NumberSetting speed = number("speed", "Cycle speed", 1, 10, 1, 3);
    private final long startedAt = System.nanoTime();

    public RainbowWorldModule() {
        super("rainbow_world", "Rainbow World", "Tints the world view with a moving rainbow.", false, Module.Category.RENDER);
        hudOpacity.set(20);
    }

    @Override
    public boolean isFullScreenOverlay() { return true; }

    @Override
    public String hudLine(Minecraft mc) { return null; }

    @Override
    public int hudWidth(Font font, Minecraft mc, String displayText) { return 0; }

    @Override
    public int hudHeight(Font font, Minecraft mc, String displayText) { return 0; }

    @Override
    public void renderHud(GuiGraphicsExtractor g, Font font, Minecraft mc, int screenW, int screenH, float dt) {
        if (mc.player == null || screenW <= 0 || screenH <= 0 || hudOpacity.getInt() <= 0) return;

        int strips = Math.max(1, Math.min(160, screenW / 6));
        double elapsed = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        double phase = (hudAccent.getInt() + elapsed * speed.get() * 24.0) % 360.0;
        int alpha = hudOpacity.getInt() * 255 / 100;

        for (int i = 0; i < strips; i++) {
            int left = i * screenW / strips;
            int right = (i + 1) * screenW / strips;
            int hue = (int) ((phase + i * 360.0 / strips) % 360.0);
            g.fill(left, 0, right, screenH, Ui.argb(alpha, Ui.hueColor(hue)));
        }
    }
}