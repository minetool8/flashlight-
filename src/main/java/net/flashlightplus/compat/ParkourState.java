package net.flashlightplus.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Smoothed "ParCool is animating this player" weight, 0 = normal, 1 = parkour fully in control of the arms. */
public final class ParkourState {
    private static final Map<UUID, float[]> STATE = new HashMap<>(); // {weight, lastNanos(as float secs)}
    private static final float RATE = 8f; // full blend in ~0.125 s

    private ParkourState() {}

    public static float weight(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0f;
        Player p = mc.level.getPlayerByUUID(id);
        float target = (p != null && Refl.parcoolAnimating(p)) ? 1f : 0f;
        float now = (System.nanoTime() / 1_000_000L) / 1000f;
        float[] s = STATE.computeIfAbsent(id, k -> new float[]{0f, now});
        float dt = Mth.clamp(now - s[1], 0f, 0.1f);
        s[1] = now;
        float step = RATE * dt;
        s[0] += Mth.clamp(target - s[0], -step, step);
        return s[0];
    }
}
