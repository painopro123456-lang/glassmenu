package dev.glassmenu.api;

/**
 * Optional: other mods can add modules by declaring a "glassmenu" entrypoint in their
 * fabric.mod.json that points at a class implementing this interface.
 */
public interface ModuleProvider {
    void registerModules(ModuleManager manager);
}
