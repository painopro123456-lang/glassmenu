package dev.glassmenu;

import dev.glassmenu.api.ModuleManager;
import dev.glassmenu.api.ModuleProvider;
import dev.glassmenu.modules.ClockModule;
import dev.glassmenu.modules.DirectionModule;
import dev.glassmenu.modules.KeysModule;
import dev.glassmenu.modules.MemoryModule;
import dev.glassmenu.modules.SessionModule;
import dev.glassmenu.modules.SpeedModule;

public class ExtraModules implements ModuleProvider {
    @Override
    public void registerModules(ModuleManager manager) {
        manager.add(new ClockModule());
        manager.add(new SpeedModule());
        manager.add(new DirectionModule());
        manager.add(new KeysModule());
        manager.add(new MemoryModule());
        manager.add(new SessionModule());
    }
}
