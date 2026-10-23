package dev.glassmenu.modules;

import dev.glassmenu.api.Module;
import dev.glassmenu.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ArmorModule extends Module {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public ArmorModule() { super("armor", "Armor Display", "Shows worn armor with durability.", true); }

    @Override
    public void renderHud(GuiGraphicsExtractor g, Font font, Minecraft mc, int w, int h, float dt) {
        int ax = w - 24, ay = h / 2 - 40;
        boolean any = false;
        for (EquipmentSlot s : SLOTS) if (!mc.player.getItemBySlot(s).isEmpty()) any = true;
        if (!any) return;
        Ui.rect(g, ax - 3, ay - 3, 22, 18 * 4 + 4, 5, 0x55000000);
        for (EquipmentSlot slot : SLOTS) {
            ItemStack st = mc.player.getItemBySlot(slot);
            if (!st.isEmpty()) {
                g.item(st, ax, ay);
                g.itemDecorations(font, st, ax, ay);
            }
            ay += 18;
        }
    }
}
