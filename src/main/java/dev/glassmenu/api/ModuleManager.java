package dev.glassmenu.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.glassmenu.gui.Ui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    public static ModuleManager get() { return INSTANCE; }

    private final List<Module> modules = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** What we store per module in config/glassmenu.json. */
    private static class Saved {
        boolean enabled;
        Map<String, Double> settings = new LinkedHashMap<>();
    }

    public static void register(Module m) { INSTANCE.add(m); }

    public void add(Module m) {
        for (Module o : modules) {
            if (o.id.equals(m.id)) throw new IllegalArgumentException("Duplicate module id: " + m.id);
        }
        modules.add(m);
    }

    public List<Module> all() { return Collections.unmodifiableList(modules); }

    // ---- per tick / frame / HUD --------------------------------------
    public void tick(Minecraft mc) {
        if (mc.player == null) return;
        for (Module m : modules) if (m.isEnabled()) m.onTick(mc);
    }

    public void frame(Minecraft mc, float dt) {
        if (mc.player == null) return;
        for (Module m : modules) if (m.isEnabled()) m.onFrame(mc, dt);
    }

    public void renderHud(GuiGraphicsExtractor g, Minecraft mc, float dt) {
        if (mc.player == null) return;
        Font font = mc.font;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        int y = 4;
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            String line = m.hudLine(mc);
            if (line != null) { Ui.pill(g, font, 4, y, line, 0xFFFFFFFF); y += 16; }
        }
        for (Module m : modules) if (m.isEnabled()) m.renderHud(g, font, mc, w, h, dt);
    }

    // ---- config -------------------------------------------------------
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("glassmenu.json"); }

    public void load() {
        try {
            Path p = path();
            if (!Files.exists(p)) return;
            Map<String, Saved> data = GSON.fromJson(Files.readString(p), new TypeToken<Map<String, Saved>>() {}.getType());
            if (data == null) return;
            for (Module m : modules) {
                Saved s = data.get(m.id);
                if (s == null) continue;
                m.setEnabled(s.enabled);
                for (NumberSetting ns : m.settings) {
                    Double v = s.settings.get(ns.key);
                    if (v != null) ns.set(v);
                }
            }
        } catch (Exception e) {
            System.err.println("[glassmenu] could not read config, using defaults: " + e);
        }
    }

    public void save() {
        try {
            Map<String, Saved> data = new LinkedHashMap<>();
            for (Module m : modules) {
                Saved s = new Saved();
                s.enabled = m.isEnabled();
                for (NumberSetting ns : m.settings) s.settings.put(ns.key, ns.get());
                data.put(m.id, s);
            }
            Files.writeString(path(), GSON.toJson(data));
        } catch (Exception e) {
            System.err.println("[glassmenu] could not save config: " + e);
        }
    }
}
