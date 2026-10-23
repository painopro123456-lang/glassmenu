package dev.glassmenu.modules;

import dev.glassmenu.api.Module;
import dev.glassmenu.api.NumberSetting;
import dev.glassmenu.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Shows a brief health card above the last living entity the player attacked. */
public final class HitHealthIndicatorModule extends Module {
    private static HitHealthIndicatorModule instance;

    private final NumberSetting displaySeconds = number("display_seconds", "Display time", 1, 6, 0.5, 2.5);
    private LivingEntity target;
    private ClientLevel trackedLevel;
    private Vec3 lastAnchor;
    private String targetName = "";
    private float targetHealth;
    private float targetMaxHealth = 1.0F;
    private long shownAt;

    public HitHealthIndicatorModule() {
        super("hit_health", "Hit Health", "Shows the target's health above them after you attack.", true, Category.COMBAT);
        hudOpacity.set(90);
        instance = this;
    }

    public static void recordAttack(Entity entity) {
        HitHealthIndicatorModule module = instance;
        if (module == null || !module.isEnabled() || !(entity instanceof LivingEntity living)) return;

        module.target = living;
        Minecraft mc = Minecraft.getInstance();
        module.trackedLevel = mc.level;
        module.targetName = living.getName().getString();
        module.targetMaxHealth = Math.max(1.0F, living.getMaxHealth());
        module.targetHealth = Math.max(0.0F, living.getHealth());
        module.lastAnchor = living.position().add(0.0, living.getBbHeight() + 0.45, 0.0);
        module.shownAt = System.nanoTime();
    }

    public static void renderFloating(GuiGraphicsExtractor g, Minecraft mc) {
        HitHealthIndicatorModule module = instance;
        if (module != null) module.renderTarget(g, mc);
    }

    @Override
    public boolean isFullScreenOverlay() { return true; }

    @Override
    public String hudLine(Minecraft mc) { return null; }

    @Override
    public int hudWidth(Font font, Minecraft mc, String displayText) { return 0; }

    @Override
    public int hudHeight(Font font, Minecraft mc, String displayText) { return 0; }

    @Override
    public void onDisable() {
        clear();
    }

    @Override
    public void onTick(Minecraft mc) {
        if (lastAnchor == null) return;
        if (mc.player == null || mc.level == null || mc.level != trackedLevel) {
            clear();
            return;
        }
        if (target != null) {
            if (target.isRemoved()) {
                targetHealth = Math.max(0.0F, target.getHealth());
                targetMaxHealth = Math.max(1.0F, target.getMaxHealth());
                lastAnchor = target.position().add(0.0, target.getBbHeight() + 0.45, 0.0);
                target = null;
            } else {
                targetName = target.getName().getString();
                targetHealth = Math.max(0.0F, target.getHealth());
                targetMaxHealth = Math.max(1.0F, target.getMaxHealth());
                lastAnchor = target.position().add(0.0, target.getBbHeight() + 0.45, 0.0);
            }
        }
        if ((System.nanoTime() - shownAt) / 1_000_000_000.0 >= displaySeconds.get()) clear();
    }

    private void renderTarget(GuiGraphicsExtractor g, Minecraft mc) {
        if (!isEnabled() || lastAnchor == null || mc.player == null || mc.level == null
                || mc.level != trackedLevel) return;

        if (target != null && !target.isRemoved()) {
            targetName = target.getName().getString();
            targetHealth = Math.max(0.0F, target.getHealth());
            targetMaxHealth = Math.max(1.0F, target.getMaxHealth());
            lastAnchor = target.position().add(0.0, target.getBbHeight() + 0.45, 0.0);
        }

        double duration = displaySeconds.get();
        double elapsed = (System.nanoTime() - shownAt) / 1_000_000_000.0;
        if (elapsed >= duration) {
            clear();
            return;
        }
        float fade = (float) Math.max(0.0, Math.min(1.0, (duration - elapsed) / Math.min(0.45, duration)));
        int alpha = Math.round(255 * fade);

        Vec3 projected = mc.gameRenderer.projectPointToScreen(lastAnchor);
        if (!Double.isFinite(projected.x) || !Double.isFinite(projected.y) || !Double.isFinite(projected.z)
                || projected.z < 0.0 || projected.z > 1.0
                || projected.x < -1.0 || projected.x > 1.0 || projected.y < -1.0 || projected.y > 1.0) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        if (screenW <= 0 || screenH <= 0) return;

        Font font = mc.font;
        String healthText = String.format(Locale.ROOT, "%.1f / %.1f HP", targetHealth, targetMaxHealth);
        int cardW = Math.max(112, Math.min(196, Math.max(font.width(targetName), font.width(healthText)) + 22));
        int cardH = 36;
        String name = fit(targetName, cardW - 20);

        int x = (int) Math.round((projected.x * 0.5 + 0.5) * screenW - cardW * 0.5);
        int y = (int) Math.round((0.5 - projected.y * 0.5) * screenH - cardH - 8);
        x = Math.max(4, Math.min(screenW - cardW - 4, x));
        y = Math.max(4, Math.min(screenH - cardH - 4, y));

        int panelAlpha = Math.round(hudOpacity.getInt() * 2.55F * fade);
        Ui.rect(g, x, y, cardW, cardH, 7,
                Ui.argb(panelAlpha, Ui.isLightTheme() ? 0xF5F8FC : 0x101827));
        Ui.rect(g, x + 5, y + 6, 2, cardH - 12, 1,
                Ui.argb(alpha, Ui.hueColor(hudAccent.getInt())));

        float ratio = Math.max(0.0F, Math.min(1.0F, targetHealth / targetMaxHealth));
        int healthHue = ratio > 0.55F ? 125 : ratio > 0.25F ? 44 : 2;
        int healthColor = Ui.argb(alpha, Ui.hueColor(healthHue));
        Ui.text(g, font, name, x + 12, y + 5, Ui.mulAlpha(Ui.primaryText(), fade));
        Ui.text(g, font, healthText, x + 12, y + 17, healthColor);
        Ui.rect(g, x + 12, y + 29, cardW - 20, 4, 2,
                Ui.argb(panelAlpha, Ui.isLightTheme() ? 0xAAB7C5 : 0x3B4A5D));
        int fill = Math.max(0, Math.round((cardW - 20) * ratio));
        if (fill > 0) Ui.rect(g, x + 12, y + 29, fill, 4, 2, healthColor);
    }

    private static String fit(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return "Target";
        while (text.length() > 1 && Minecraft.getInstance().font.width(text) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private void clear() {
        target = null;
        trackedLevel = null;
        lastAnchor = null;
        targetName = "";
        targetHealth = 0.0F;
        targetMaxHealth = 1.0F;
        shownAt = 0L;
    }
}
