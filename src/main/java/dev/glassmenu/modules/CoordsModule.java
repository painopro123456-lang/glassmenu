package dev.glassmenu.modules;

import dev.glassmenu.api.Module;
import dev.glassmenu.api.NumberSetting;
import net.minecraft.client.Minecraft;

public class CoordsModule extends Module {
    private final NumberSetting decimals = number("decimals", "Decimals", 0, 2, 1, 0);

    public CoordsModule() { super("coords", "Coordinates", "Shows your X / Y / Z position.", false, Module.Category.HUD); }

    @Override
    public String hudLine(Minecraft mc) {
        String f = "%." + decimals.getInt() + "f";
        return String.format("XYZ " + f + " " + f + " " + f, mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }
}
