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
        g.fill(0, 0, width, height, 0xA0101826);
        Ui.text(g, font, "HUD Layout Editor", 16, 14, 0xFFFFFFFF);
        Ui.text(g, font, "Drag a HUD to move it. Scroll over one to resize. Right-click to reset.",
                16, 28, 0xFFD2DBE8);

        Ui.rect(g, width - 92, 10, 76, 22, 6, 0xB0203244);
        Ui.text(g, font, "Done (Esc)", width - 84, 17, 0xFFFFFFFF);

        List<Module> modules = manager.all();
        Minecraft mc = Minecraft.getInstance();
        for (Module module : modules) {
            HudLayout layout = manager.hudLayout(module, width, height);
            String label = previewText(module, mc);
            double scale = layout.scale();
            double x = elementX(layout, label, scale);
            double y = elementY(layout, scale);
            double w = (font.width(label) + 15) * scale;
            double h = 14 * scale;

            g.pose().pushMatrix();
            try {
                g.pose().translate((float) x, (float) y);
                g.pose().scale((float) scale, (float) scale);
                if (module == selected) Ui.rect(g, -3, -3, font.width(label) + 21, 20, 5, 0xCC6FD3FF);
                int color = module.isEnabled() ? 0xFFFFFFFF : 0xFFBEC9D7;
                Ui.pill(g, font, 0, 0, label, color);
            } finally {
                g.pose().popMatrix();
            }
        }

        if (selected != null) {
            HudLayout layout = manager.hudLayout(selected, width, height);
            String label = previewText(selected, mc);
            String info = selected.name + "  |  " + Math.round(layout.scale() * 100) + "%";
            Ui.text(g, font, info, 16, height - 20, 0xFFFFFFFF);
        } else {
            Ui.text(g, font, "Choose a HUD element to edit its position and size.", 16, height - 20, 0xFFFFFFFF);
        }
    }

    private String previewText(Module module, Minecraft mc) {
        if (module.isEnabled() && mc.player != null) {
            String line = module.hudLine(mc);
            if (line != null) return line;
        }
        return module.name;
    }

    private double elementX(HudLayout layout, String label, double scale) {
        double max = Math.max(0, width - (font.width(label) + 15) * scale);
        return Math.max(0, Math.min(max, layout.x() * width));
    }

    private double elementY(HudLayout layout, double scale) {
        double max = Math.max(0, height - 14 * scale);
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
            double x = elementX(layout, label, scale);
            double y = elementY(layout, scale);
            double w = (font.width(label) + 15) * scale;
            double h = 14 * scale;
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
        dragOffsetX = mx - elementX(layout, label, layout.scale());
        dragOffsetY = my - elementY(layout, layout.scale());
        dragging = hit;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging == null) return true;
        HudLayout layout = manager.hudLayout(dragging, width, height);
        String label = previewText(dragging, Minecraft.getInstance());
        double maxX = Math.max(0, width - (font.width(label) + 15) * layout.scale());
        double maxY = Math.max(0, height - 14 * layout.scale());
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
        double x = elementX(layout, label, scale);
        double y = elementY(layout, scale);
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
