package dev.glassmenu.modules;

import dev.glassmenu.api.Module;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;

public class CpsModule extends Module {
    private final ArrayDeque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();
    private boolean prevAttack, prevUse;

    public CpsModule() { super("cps", "CPS", "Clicks per second (left | right).", true); }

    @Override
    public void onFrame(Minecraft mc, float dt) {
        boolean a = mc.options.keyAttack.isDown(), u = mc.options.keyUse.isDown();
        long now = System.currentTimeMillis();
        if (a && !prevAttack) left.addLast(now);
        if (u && !prevUse) right.addLast(now);
        prevAttack = a;
        prevUse = u;
    }

    private static int count(ArrayDeque<Long> q) {
        long cutoff = System.currentTimeMillis() - 1000;
        while (!q.isEmpty() && q.peekFirst() < cutoff) q.pollFirst();
        return q.size();
    }

    @Override
    public String hudLine(Minecraft mc) { return "CPS " + count(left) + " | " + count(right); }
}
