package dev.glassmenu;

import dev.glassmenu.api.Module;
import dev.glassmenu.api.ModuleManager;
import dev.glassmenu.api.ModuleProvider;
import dev.glassmenu.modules.ClockModule;
import dev.glassmenu.modules.DirectionModule;
import dev.glassmenu.modules.KeysModule;
import dev.glassmenu.modules.MemoryModule;
import dev.glassmenu.modules.SessionModule;
import dev.glassmenu.modules.SpeedModule;
import net.minecraft.client.Minecraft;

/** Registers optional HUD modules included with Glass Menu. */
public class ExtraModules implements ModuleProvider {
    @Override
    public void registerModules(ModuleManager manager) {
        manager.add(new ClockModule());
        manager.add(new SpeedModule());
        manager.add(new DirectionModule());
        manager.add(new KeysModule());
        manager.add(new MemoryModule());
        manager.add(new SessionModule());
        manager.add(new HealthModule());
        manager.add(new HungerModule());
    }

    private static final class HealthModule extends Module {
        private HealthModule() {
            super("health", "Health", "Shows your current and maximum health.", true);
        }

        @Override
        public String hudLine(Minecraft minecraft) {
            if (minecraft.player == null) return "Health: --";
            return String.format("Health: %.0f/%.0f", minecraft.player.getHealth(), minecraft.player.getMaxHealth());
        }
    }

    private static final class HungerModule extends Module {
        private HungerModule() {
            super("hunger", "Hunger", "Shows your current hunger level.", true);
        }

        @Override
        public String hudLine(Minecraft minecraft) {
            if (minecraft.player == null) return "Hunger: --";
            return "Hunger: " + minecraft.player.getFoodData().getFoodLevel() + "/20";
        }
    }
}
