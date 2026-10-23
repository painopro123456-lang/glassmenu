package dev.glassmenu.modules;

import dev.glassmenu.api.Module;
import dev.glassmenu.api.NumberSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Hold-to-zoom module. Its key can be changed in Minecraft's Controls screen. */
public final class ZoomModule extends Module {
    private final NumberSetting targetFov = number("target_fov", "Zoom FOV", 30, 60, 2, 30);
    private boolean zooming;
    private int originalFov = -1;

    public ZoomModule() {
        super("zoom", "Zoom", "Hold C to zoom in; change the key under Controls.", true, Module.Category.RENDER);
    }

    @Override
    public int defaultKeyCode() {
        return GLFW.GLFW_KEY_C;
    }

    @Override
    public boolean isHoldKeybind() {
        return true;
    }

    @Override
    public void onKeybindHeld(Minecraft mc, boolean down) {
        if (!down) {
            restoreFov(mc);
            return;
        }

        if (!zooming) {
            originalFov = mc.options.fov().get();
            zooming = true;
        }

        int zoomFov = Math.min(originalFov, targetFov.getInt());
        if (mc.options.fov().get() != zoomFov) {
            mc.options.fov().set(zoomFov);
        }
    }

    @Override
    public void onDisable() {
        restoreFov(Minecraft.getInstance());
    }

    private void restoreFov(Minecraft mc) {
        if (!zooming) return;
        int fov = originalFov;
        zooming = false;
        originalFov = -1;
        if (mc.options.fov().get() != fov) {
            mc.options.fov().set(fov);
        }
    }
}
