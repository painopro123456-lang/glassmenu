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
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public ArmorModule() {
        super("armor", "Armor Display", "Shows worn armor with durability.", true);
    }

    @Override
    public void renderHud(GuiGraphicsExtractor g, Font font, Minecraft mc, int w, int h, float partial) {
        int x = 0;
        int y = 0;

        boolean any = false;
        int textW = 0;
        String[] labels = new String[4];
        for (int i = 0; i < 4; i++) {
            ItemStack s = mc.player.getItemBySlot(SLOTS[i]);
            if (s.isEmpty()) continue;
            any = true;
            int max = s.getMaxDamage();
            if (max > 0) {
                labels[i] = String.valueOf(max - s.getDamageValue());
                textW = Math.max(textW, font.width(labels[i]));
            }
        }
        if (!any) return;

        int extra = textW > 0 ? textW + 4 : 0;
        Ui.rect(g, x - 3 - extra, y - 3, 22 + extra, 76, 5,
                Ui.argb(hudOpacity.getInt() * 255 / 100, Ui.isLightTheme() ? 0xF7FAFD : 0x000000));
        Ui.rect(g, x - 3 - extra, y + 2, 2, 66, 1, Ui.hueColor(hudAccent.getInt()));

        int yy = y;
        for (int i = 0; i < 4; i++) {
            ItemStack s = mc.player.getItemBySlot(SLOTS[i]);
            if (!s.isEmpty()) {
                g.item(s, x, yy);
                g.itemDecorations(font, s, x, yy);
                if (labels[i] != null) {
                    int max = s.getMaxDamage();
                    float frac = Math.max(0f, Math.min(1f, (max - s.getDamageValue()) / (float) max));
                    int r = (int) (255 * Math.min(1f, (1f - frac) * 2f));
                    int gr = (int) (255 * Math.min(1f, frac * 2f));
                    int color = 0xFF000000 | (r << 16) | (gr << 8) | 0x20;
                    Ui.text(g, font, labels[i], x - 3 - font.width(labels[i]), yy + 4, color);
                }
            }
            yy += 18;
        }
    }
}
