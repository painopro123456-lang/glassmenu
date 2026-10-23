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
    private final Map<String, HudLayout> hudLayouts = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** What we store per module in config/glassmenu.json. */
    private static class Saved {
        boolean enabled;
        Map<String, Double> settings = new LinkedHashMap<>();
        Double hudX, hudY, hudScale;
        Boolean lightTheme;
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
        for (Module m : modules) {
            if (!m.isEnabled() || m.isFullScreenOverlay()) continue;
            String line = m.hudLine(mc);
            String displayText = line == null ? m.name : line;
            HudLayout layout = hudLayout(m, w, h);
            float scale = (float) layout.scale();
            int localW = m.id.equals("armor") && line == null ? 32 : m.hudWidth(font, mc, displayText);
            int localH = m.id.equals("armor") && line == null ? 76 : m.hudHeight(font, mc, displayText);
            float x = (float) Math.max(0, Math.min(layout.x() * w, w - localW * scale));
            float y = (float) Math.max(0, Math.min(layout.y() * h, h - localH * scale));
            g.pose().pushMatrix();
            try {
                g.pose().translate(x, y);
                g.pose().scale(scale, scale);
                if (line != null) Ui.pill(g, font, 0, 0, line, Ui.primaryText(),
                        m.hudAccent.getInt(), m.hudOpacity.getInt());
                m.renderHud(g, font, mc, w, h, dt);
            } finally {
                g.pose().popMatrix();
            }
        }
    }

    /** Render screen-wide effects before the vanilla HUD, without applying module layout transforms. */
    public void renderScreenOverlays(GuiGraphicsExtractor g, Minecraft mc) {
        if (mc.player == null) return;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        for (Module m : modules) {
            if (m.isEnabled() && m.isFullScreenOverlay()) {
                m.renderHud(g, mc.font, mc, w, h, 0.0f);
            }
        }
    }

    /** A module's HUD position is stored as a fraction of the current GUI size. */
    public record HudLayout(double x, double y, double scale) {}

    public HudLayout hudLayout(Module module, int screenW, int screenH) {
        HudLayout saved = hudLayouts.get(module.id);
        return saved != null ? saved : defaultHudLayout(module, screenW, screenH);
    }

    public void setHudLayout(Module module, int screenW, int screenH, double x, double y, double scale) {
        if (screenW <= 0 || screenH <= 0) return;
        double px = Math.max(0, Math.min(screenW, x));
        double py = Math.max(0, Math.min(screenH, y));
        double sc = Math.max(0.5, Math.min(2.5, scale));
        hudLayouts.put(module.id, new HudLayout(px / screenW, py / screenH, sc));
    }

    public void resetHudLayout(Module module) {
        hudLayouts.remove(module.id);
    }

    private HudLayout defaultHudLayout(Module module, int screenW, int screenH) {
        int w = Math.max(1, screenW), h = Math.max(1, screenH);
        if (module.id.equals("armor")) {
            return new HudLayout(Math.max(0, w - 24.0) / w, Math.max(0, h / 2.0 - 40.0) / h, 1.0);
        }
        int row = 0;
        for (Module candidate : modules) {
            if (candidate == module) break;
            if (!candidate.id.equals("armor")) row++;
        }
        return new HudLayout(4.0 / w, (4.0 + row * 16.0) / h, 1.0);
    }

    // ---- config -------------------------------------------------------
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("glassmenu.json"); }

    public void load() {
        try {
            Path p = path();
            if (!Files.exists(p)) {
                for (Module m : modules) if (!m.persistsEnabledState()) m.setEnabled(false);
                return;
            }
            Map<String, Saved> data = GSON.fromJson(Files.readString(p), new TypeToken<Map<String, Saved>>() {}.getType());
            if (data == null) {
                for (Module m : modules) if (!m.persistsEnabledState()) m.setEnabled(false);
                return;
            }
            Saved client = data.get("_client");
            if (client != null && client.lightTheme != null) Ui.setLightTheme(client.lightTheme);
            for (Module m : modules) {
                Saved s = data.get(m.id);
                if (!m.persistsEnabledState()) {
                    m.setEnabled(false);
                    continue;
                }
                if (s == null) continue;
                m.setEnabled(s.enabled);
                for (NumberSetting ns : m.settings) {
                    Double v = s.settings.get(ns.key);
                    if (v != null) ns.set(v);
                }
                if (s.hudX != null || s.hudY != null || s.hudScale != null) {
                    hudLayouts.put(m.id, new HudLayout(
                            s.hudX == null ? 0.01 : Math.max(0, Math.min(1, s.hudX)),
                            s.hudY == null ? 0.01 : Math.max(0, Math.min(1, s.hudY)),
                            s.hudScale == null ? 1.0 : Math.max(0.5, Math.min(2.5, s.hudScale))));
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
                s.enabled = m.persistsEnabledState() && m.isEnabled();
                for (NumberSetting ns : m.settings) s.settings.put(ns.key, ns.get());
                HudLayout layout = hudLayouts.get(m.id);
                if (layout != null) {
                    s.hudX = layout.x();
                    s.hudY = layout.y();
                    s.hudScale = layout.scale();
                }
                data.put(m.id, s);
            }
            Saved client = new Saved();
            client.lightTheme = Ui.isLightTheme();
            data.put("_client", client);
            Files.writeString(path(), GSON.toJson(data));
        } catch (Exception e) {
            System.err.println("[glassmenu] could not save config: " + e);
        }
    }
}
