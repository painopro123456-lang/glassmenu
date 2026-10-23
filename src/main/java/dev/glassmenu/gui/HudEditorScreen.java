package dev.glassmenu.gui;

import dev.glassmenu.api.Module;
import dev.glassmenu.api.ModuleManager;
import dev.glassmenu.api.ModuleManager.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Full-screen editor for moving and resizing each module's HUD element. */
public class HudEditorScreen extends Screen {
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 2.5;
    private final Screen parent;
    private final ModuleManager manager = ModuleManager.get();
    private Module selected;
    private Module dragging;
    private double dragOffsetX, dragOffsetY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Layout Editor"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, Ui.isLightTheme() ? 0xA0F1F4F8 : 0xA0101826);
        Ui.text(g, font, "HUD Layout Editor", 16, 14, Ui.primaryText());
        Ui.text(g, font, "Drag to move. Scroll to resize. Right-click to reset.",
                16, 28, Ui.secondaryText());

        Ui.rect(g, width - 92, 10, 76, 22, 6, Ui.isLightTheme() ? 0xD4DDE7EF : 0xB0203244);
        Ui.text(g, font, "Done (Esc)", width - 84, 17, Ui.primaryText());

        List<Module> modules = manager.all();
        Minecraft mc = Minecraft.getInstance();
        for (Module module : modules) {
            HudLayout layout = manager.hudLayout(module, width, height);
            String label = previewText(module, mc);
            double scale = layout.scale();
            double x = elementX(module, layout, label, scale, mc);
            double y = elementY(module, layout, scale, mc);
            double w = module.hudWidth(font, mc, label) * scale;
            double h = module.hudHeight(font, mc, label) * scale;

            g.pose().pushMatrix();
            try {
                g.pose().translate((float) x, (float) y);
                g.pose().scale((float) scale, (float) scale);
                if (module == selected) Ui.rect(g, -3, -3, font.width(label) + 21, 20, 5,
                        Ui.argb(0xCC, Ui.hueColor(module.hudAccent.getInt())));
                int color = module.isEnabled() ? Ui.primaryText() : Ui.secondaryText();
                Ui.pill(g, font, 0, 0, label, color, module.hudAccent.getInt(), module.hudOpacity.getInt());
                if (module.isEnabled() && mc.player != null) module.renderHud(g, font, mc, width, height, delta);
            } finally {
                g.pose().popMatrix();
            }
        }

        if (selected != null) {
            HudLayout layout = manager.hudLayout(selected, width, height);
            String label = previewText(selected, mc);
            String info = selected.name + "  |  " + Math.round(layout.scale() * 100) + "%";
            Ui.text(g, font, info, 16, height - 20, Ui.primaryText());
        } else {
            Ui.text(g, font, "Choose a HUD element to edit its position and size.", 16, height - 20, Ui.primaryText());
        }
    }

    private String previewText(Module module, Minecraft mc) {
        if (module.isEnabled() && mc.player != null) {
            String line = module.hudLine(mc);
            if (line != null) return line;
        }
        return module.name;
    }

    private double elementX(Module module, HudLayout layout, String label, double scale, Minecraft mc) {
        double max = Math.max(0, width - module.hudWidth(font, mc, label) * scale);
        return Math.max(0, Math.min(max, layout.x() * width));
    }

    private double elementY(Module module, HudLayout layout, double scale, Minecraft mc) {
        String label = previewText(module, mc);
        double max = Math.max(0, height - module.hudHeight(font, mc, label) * scale);
        return Math.max(0, Math.min(max, layout.y() * height));
    }

    private Module moduleAt(double mouseX, double mouseY) {
        List<Module> modules = manager.all();
        Minecraft mc = Minecraft.getInstance();
        for (int i = modules.size() - 1; i >= 0; i--) {
            Module module = modules.get(i);
            HudLayout layout = manager.hudLayout(module, width, height);
            String label = previewText(module, mc);
            double scale = layout.scale();
            double x = elementX(module, layout, label, scale, mc);
            double y = elementY(module, layout, scale, mc);
            double w = module.hudWidth(font, mc, label) * scale;
            double h = module.hudHeight(font, mc, label) * scale;
            if (mouseX >= x - 3 && mouseX <= x + w + 3
                    && mouseY >= y - 3 && mouseY <= y + h + 3) return module;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (event.button() == 0 && mx >= width - 96 && my <= 38) {
            closeEditor();
            return true;
        }

        Module hit = moduleAt(mx, my);
        if (hit == null) return true;
        selected = hit;
        if (event.button() == 1) {
            manager.resetHudLayout(hit);
            manager.save();
            dragging = null;
            return true;
        }
        if (event.button() != 0) return true;

        HudLayout layout = manager.hudLayout(hit, width, height);
        String label = previewText(hit, Minecraft.getInstance());
        dragOffsetX = mx - elementX(hit, layout, label, layout.scale(), Minecraft.getInstance());
        dragOffsetY = my - elementY(hit, layout, layout.scale(), Minecraft.getInstance());
        dragging = hit;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging == null) return true;
        HudLayout layout = manager.hudLayout(dragging, width, height);
        String label = previewText(dragging, Minecraft.getInstance());
        Minecraft mc = Minecraft.getInstance();
        double maxX = Math.max(0, width - dragging.hudWidth(font, mc, label) * layout.scale());
        double maxY = Math.max(0, height - dragging.hudHeight(font, mc, label) * layout.scale());
        double x = Math.max(0, Math.min(maxX, event.x() - dragOffsetX));
        double y = Math.max(0, Math.min(maxY, event.y() - dragOffsetY));
        manager.setHudLayout(dragging, width, height, x, y, layout.scale());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            manager.save();
            dragging = null;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Module target = moduleAt(mouseX, mouseY);
        if (target == null) return true;
        selected = target;
        HudLayout layout = manager.hudLayout(target, width, height);
        double scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, layout.scale() + scrollY * 0.1));
        String label = previewText(target, Minecraft.getInstance());
        Minecraft mc = Minecraft.getInstance();
        double x = elementX(target, layout, label, scale, mc);
        double y = elementY(target, layout, scale, mc);
        manager.setHudLayout(target, width, height, x, y, scale);
        manager.save();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        closeEditor();
    }

    private void closeEditor() {
        manager.save();
        minecraft.setScreenAndShow(parent);
    }
}
