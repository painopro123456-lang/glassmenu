package dev.glassmenu.modules;

import dev.glassmenu.api.Module;
import net.minecraft.client.Minecraft;

public class SprintModule extends Module {
    public SprintModule() { super("sprint", "Toggle Sprint", "Keeps you sprinting while you move forward.", false); }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.options.keyUp.isDown() && !mc.player.isShiftKeyDown() && !mc.player.horizontalCollision) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public String hudLine(Minecraft mc) { return "Sprint: toggled"; }
}
