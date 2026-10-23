package dev.glassmenu;

import com.mojang.blaze3d.platform.InputConstants;
import dev.glassmenu.api.ModuleManager;
import dev.glassmenu.api.ModuleProvider;
import dev.glassmenu.gui.MenuScreen;
import dev.glassmenu.modules.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class GlassMenu implements ClientModInitializer {
    public static KeyMapping menuKey;
    private static long lastFrame = System.nanoTime();
    private static final Set<Screen> SCREENS_WITH_JUMPSCARE_OVERLAY =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void onInitializeClient() {
        // ---- ADD YOUR MODULES HERE: one line per module -------------------
        ModuleManager.register(new FpsModule());
        ModuleManager.register(new CpsModule());
        ModuleManager.register(new CoordsModule());
        ModuleManager.register(new ArmorModule());
        ModuleManager.register(new SprintModule());
        ExtraModules.register();
        // --------------------------------------------------------------------

        // modules contributed by other mods through the "glassmenu" entrypoint
        for (ModuleProvider p : FabricLoader.getInstance().getEntrypoints("glassmenu", ModuleProvider.class)) {
            p.registerModules(ModuleManager.get());
        }
        ModuleManager.get().load();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("glassmenu", "main"));
        menuKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.glassmenu.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) mc.setScreenAndShow(new MenuScreen(null));
            ModuleManager.get().tick(mc);
        });

        // Add the jumpscare after each screen is extracted so it sits above Moon Menu and other screens.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (SCREENS_WITH_JUMPSCARE_OVERLAY.add(screen)) {
                ScreenEvents.afterExtract(screen).register((renderedScreen, graphics, mouseX, mouseY, tickProgress) ->
                        ModuleManager.get().renderJumpscareOverlay(graphics, Minecraft.getInstance()));
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("glassmenu", "rainbow_world_overlay"), GlassMenu::extractWorldOverlay);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("glassmenu", "hud"), GlassMenu::extractHud);
    }

    private static void extractWorldOverlay(GuiGraphicsExtractor g, DeltaTracker tracker) {
        ModuleManager.get().renderScreenOverlays(g, Minecraft.getInstance());
    }

    private static void extractHud(GuiGraphicsExtractor g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        ModuleManager.get().frame(mc, dt);
        ModuleManager.get().renderHud(g, mc, dt);
    }
}
