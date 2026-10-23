package dev.glassmenu.modules;

import dev.glassmenu.api.Module;
import dev.glassmenu.gui.Ui;
import net.minecraft.client.Minecraft;

public class FpsModule extends Module {
    private float fps = 60f;

    public FpsModule() { super("fps", "FPS", "Shows your frame rate.", true); }

    @Override
    public void onFrame(Minecraft mc, float dt) {
        fps = Ui.smooth(fps, 1f / Math.max(dt, 0.0005f), dt, 3f);
    }

    @Override
    public String hudLine(Minecraft mc) { return Math.round(fps) + " FPS"; }
}
