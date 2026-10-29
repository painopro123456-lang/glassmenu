package dev.glassmenu.modules;

import dev.glassmenu.api.Module;
import dev.glassmenu.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

/**
 * PvP-client style keystrokes HUD: live WASD, jump, and mouse buttons.
 * Keycaps brighten while held and use the same accent as the GlassMenu HUD.
 */
public final class KeystrokesModule extends Module {
    private static final int KEY_W = 20, KEY_H = 18, GAP = 3;
    private static final int PANEL_W = 78, PANEL_H = 73;

    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", "Shows live WASD, jump, left-click and right-click keycaps.", true, Module.Category.HUD);
        hudOpacity.setValue(72);
        hudAccent.setValue(205);
    }

    @Override public int hudWidth(Font font, Minecraft mc, String displayText) { return PANEL_W; }
    @Override public int hudHeight(Font font, Minecraft mc, String displayText) { return PANEL_H; }

    @Override
    public void renderHud(GuiGraphicsExtractor g, Font font, Minecraft mc, int screenW, int screenH, float dt) {
        if (mc.player == null) return;
        long window = mc.getWindow().handle();
        int alpha = Math.max(25, Math.min(100, hudOpacity.getInt()));
        int accent = Ui.hueColor(hudAccent.getInt());
        int base = Ui.argb(alpha * 255 / 100, Ui.isLightTheme() ? 0xEAF1F8 : 0x101827);
        Ui.rect(g, 0, 0, PANEL_W, PANEL_H, 7, base);
        Ui.rect(g, 0, 0, PANEL_W, 2, 1, Ui.argb(230, accent));

        drawKey(g, font, 29, 4, KEY_W, KEY_H, "W", mc.options.keyUp.isDown(), accent, alpha);
        drawKey(g, font, 7, 25, KEY_W, KEY_H, "A", mc.options.keyLeft.isDown(), accent, alpha);
        drawKey(g, font, 29, 25, KEY_W, KEY_H, "S", mc.options.keyDown.isDown(), accent, alpha);
        drawKey(g, font, 51, 25, KEY_W, KEY_H, "D", mc.options.keyRight.isDown(), accent, alpha);

        boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        drawKey(g, font, 7, 46, 31, 15, "LMB", left, accent, alpha);
        drawKey(g, font, 40, 46, 31, 15, "RMB", right, accent, alpha);
        drawKey(g, font, 7, 64, 64, 7, "", mc.options.keyJump.isDown(), accent, alpha);
        Ui.text(g, font, "SPACE", 28, 62, Ui.argb(210, Ui.isLightTheme() ? 0x26384B : 0xDCE8FA));
    }

    private static void drawKey(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                                String label, boolean pressed, int accent, int opacity) {
        int a = opacity * 255 / 100;
        int fill = pressed
                ? Ui.argb(Math.min(245, a + 25), accent)
                : Ui.argb(Math.min(190, a), Ui.isLightTheme() ? 0xD6E0EB : 0x273448);
        Ui.rect(g, x, y + (pressed ? 1 : 0), w, h, 4, fill);
        int edge = pressed ? Ui.argb(230, accent) : Ui.argb(90, Ui.isLightTheme() ? 0x7C8EA2 : 0xA9BED8);
        g.fill(x + 3, y, x + w - 3, y + 1, edge);
        if (!label.isEmpty()) {
            int color = pressed ? 0xFFFFFFFF : (Ui.isLightTheme() ? 0xFF26384B : 0xFFE7EEF9);
            Ui.text(g, font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, color);
        }
    }
}
