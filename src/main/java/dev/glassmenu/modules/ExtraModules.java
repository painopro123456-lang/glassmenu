package dev.glassmenu.modules;

import dev.glassmenu.api.Module;
import dev.glassmenu.api.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Extra HUD modules included with Glass Menu. */
public final class ExtraModules {
    private ExtraModules() {}

    public static void register() {
        ModuleManager.register(new ClockModule());
        ModuleManager.register(new SpeedModule());
        ModuleManager.register(new DirectionModule());
        ModuleManager.register(new KeysModule());
        ModuleManager.register(new MemoryModule());
        ModuleManager.register(new SessionModule());
        ModuleManager.register(new HealthModule());
        ModuleManager.register(new HungerModule());
        ModuleManager.register(new TargetHudModule());
        ModuleManager.register(new AttackCooldownModule());
        ModuleManager.register(new PvpSuppliesModule());
    }

    private static final class ClockModule extends Module {
        private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
        private ClockModule() { super("clock", "Clock", "Shows the current time.", false); }
        @Override public String hudLine(Minecraft mc) {
            return "Clock: " + LocalTime.now().format(FORMAT);
        }
    }

    private static final class SpeedModule extends Module {
        private SpeedModule() { super("speed", "Speed", "Shows your horizontal movement speed.", false); }
        @Override public String hudLine(Minecraft mc) {
            if (mc.player == null) return null;
            Vec3 motion = mc.player.getDeltaMovement();
            double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z) * 20.0;
            return String.format(Locale.ROOT, "Speed: %.1f blocks/s", speed);
        }
    }

    private static final class DirectionModule extends Module {
        private static final String[] DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        private DirectionModule() { super("direction", "Direction", "Shows the direction you are facing.", false); }
        @Override public String hudLine(Minecraft mc) {
            if (mc.player == null) return null;
            int index = Math.floorMod((int) Math.round(mc.player.getYRot() / 45.0f), DIRECTIONS.length);
            return "Direction: " + DIRECTIONS[index];
        }
    }

    private static final class KeysModule extends Module {
        private KeysModule() { super("keys", "Keys", "Shows which movement keys are pressed.", false); }
        @Override public String hudLine(Minecraft mc) {
            if (mc.player == null) return null;
            StringBuilder pressed = new StringBuilder();
            append(pressed, mc.options.keyUp.isDown(), "W");
            append(pressed, mc.options.keyLeft.isDown(), "A");
            append(pressed, mc.options.keyDown.isDown(), "S");
            append(pressed, mc.options.keyRight.isDown(), "D");
            return pressed.isEmpty() ? "Keys: -" : "Keys: " + pressed;
        }
        private static void append(StringBuilder out, boolean down, String key) {
            if (!down) return;
            if (!out.isEmpty()) out.append(' ');
            out.append(key);
        }
    }

    private static final class MemoryModule extends Module {
        private MemoryModule() { super("memory", "Memory", "Shows Java memory usage.", false); }
        @Override public String hudLine(Minecraft mc) {
            Runtime runtime = Runtime.getRuntime();
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
            long maxMb = runtime.maxMemory() / (1024L * 1024L);
            return String.format(Locale.ROOT, "Memory: %d/%d MB", usedMb, maxMb);
        }
    }

    private static final class SessionModule extends Module {
        private long startedAt = System.nanoTime();
        private SessionModule() { super("session", "Session", "Shows time since this module was enabled.", false); }
        @Override public void onEnable() { startedAt = System.nanoTime(); }
        @Override public String hudLine(Minecraft mc) {
            long seconds = (System.nanoTime() - startedAt) / 1_000_000_000L;
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long remainder = seconds % 60;
            return String.format(Locale.ROOT, "Session: %02d:%02d:%02d", hours, minutes, remainder);
        }
    }

    private static final class TargetHudModule extends Module {
        private TargetHudModule() {
            super("target_hud", "Target HUD", "Shows the name, health, and distance of the entity you are aiming at.", false);
        }
        @Override public String hudLine(Minecraft mc) {
            if (mc.player == null || !(mc.hitResult instanceof EntityHitResult hit)
                    || !(hit.getEntity() instanceof LivingEntity target)) return null;
            return String.format(Locale.ROOT, "Target: %s %.0f/%.0f HP %.1f blocks",
                    target.getName().getString(), target.getHealth(), target.getMaxHealth(),
                    mc.player.distanceTo(target));
        }
    }

    private static final class AttackCooldownModule extends Module {
        private AttackCooldownModule() {
            super("attack_cooldown", "Attack Cooldown", "Shows how ready your next attack is.", false);
        }
        @Override public String hudLine(Minecraft mc) {
            if (mc.player == null) return null;
            return String.format(Locale.ROOT, "Attack: %.0f%%",
                    mc.player.getAttackStrengthScale(0.0F) * 100.0F);
        }
    }

    private static final class PvpSuppliesModule extends Module {
        private PvpSuppliesModule() {
            super("pvp_supplies", "PvP Supplies", "Counts totems, ender pearls, golden apples, and end crystals.", false);
        }
        @Override public String hudLine(Minecraft mc) {
            if (mc.player == null) return null;
            var inventory = mc.player.getInventory();
            return "PvP Items: Totems " + inventory.countItem(Items.TOTEM_OF_UNDYING)
                    + " Pearls " + inventory.countItem(Items.ENDER_PEARL)
                    + " Gapples " + inventory.countItem(Items.GOLDEN_APPLE)
                    + " Crystals " + inventory.countItem(Items.END_CRYSTAL);
        }
    }

    private static final class HealthModule extends Module {
        private HealthModule() { super("health", "Health", "Shows your current and maximum health.", true); }
        @Override public String hudLine(Minecraft mc) {
            if (mc.player == null) return null;
            return String.format(Locale.ROOT, "Health: %.0f/%.0f", mc.player.getHealth(), mc.player.getMaxHealth());
        }
    }

    private static final class HungerModule extends Module {
        private HungerModule() { super("hunger", "Hunger", "Shows your current hunger level.", true); }
        @Override public String hudLine(Minecraft mc) {
            if (mc.player == null) return null;
            return "Hunger: " + mc.player.getFoodData().getFoodLevel() + "/20";
        }
    }
}
