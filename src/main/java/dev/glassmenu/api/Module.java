package dev.glassmenu.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every module. Subclass it, override what you need, and register it with
 * {@link ModuleManager#register(Module)}. It then appears in the menu automatically.
 */
public abstract class Module {
    public final String id, name, description;
    public final List<NumberSetting> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String id, String name, String description, boolean defaultEnabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = defaultEnabled;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean v) {
        if (v == enabled) return;
        enabled = v;
        if (v) onEnable(); else onDisable();
    }

    public void toggle() { setEnabled(!enabled); }

    /** Declares a slider that shows up under the module in the menu. */
    protected NumberSetting number(String key, String label, double min, double max, double step, double def) {
        NumberSetting s = new NumberSetting(key, label, min, max, step, def);
        settings.add(s);
        return s;
    }

    public void onEnable() {}
    public void onDisable() {}

    /** Called every client tick (20/s) while enabled and in a world. */
    public void onTick(Minecraft mc) {}

    /** Called every rendered frame while enabled and in a world. dt is seconds. */
    public void onFrame(Minecraft mc, float dt) {}

    /** One line of text shown as a HUD pill (stacked top-left). Return null to show nothing. */
    public String hudLine(Minecraft mc) { return null; }

    /** Custom HUD drawing. Coordinates are in GUI-scaled pixels. */
    public void renderHud(GuiGraphicsExtractor g, Font font, Minecraft mc, int screenW, int screenH, float dt) {}
}
