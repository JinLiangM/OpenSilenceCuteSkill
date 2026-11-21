package lol.silencecute.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class C4ThrowClient {

    public static final double SPEED = 1.0D;

    private static final long RISE_NANOS = (long) (160_000_000L / SPEED);

    private static final long HOLD_NANOS = (long) (120_000_000L / SPEED);

    private static final long FALL_NANOS = (long) (360_000_000L / SPEED);

    private static final long TOTAL_NANOS = RISE_NANOS + HOLD_NANOS + FALL_NANOS;

    private static final long THROW_LOCK_NANOS = 450_000_000L;

    private static final float PUNCH_IN_UNTIL = 0.55F;

    private static final float PUNCH_OUT_UNTIL = 0.55F;

    private static final float APEX_SETTLE = 0.05F;

    private static long startNanos;

    private static int lockedSlot = -1;

    private C4ThrowClient() {
    }

    public static void begin() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        startNanos = System.nanoTime();
        lockedSlot = player.getInventory().selected;
    }

    public static boolean isPlaying() {
        return startNanos != 0L && System.nanoTime() - startNanos < TOTAL_NANOS;
    }

    public static boolean throwLocked() {
        long elapsed = elapsed();
        return elapsed >= 0L && elapsed < THROW_LOCK_NANOS;
    }

    public static int lockedSlot() {
        return isPlaying() ? lockedSlot : -1;
    }

    public static boolean holding() {
        return isPlaying() && elapsed() < RISE_NANOS;
    }

    public static float extend() {
        long elapsed = elapsed();
        if (startNanos == 0L || elapsed < 0L) return 0.0F;

        if (elapsed < RISE_NANOS) {
            return easeOutCubic(elapsed / (float) RISE_NANOS);
        }

        if (elapsed < RISE_NANOS + HOLD_NANOS) {
            float h = (elapsed - RISE_NANOS) / (float) HOLD_NANOS;
            return 1.0F + APEX_SETTLE * easeOutCubic(h);
        }

        long fall = elapsed - RISE_NANOS - HOLD_NANOS;
        if (fall >= FALL_NANOS) return 0.0F;
        float t = fall / (float) FALL_NANOS;
        return (1.0F + APEX_SETTLE) * (1.0F - t * t);
    }

    public static float swing() {
        long elapsed = elapsed();
        if (startNanos == 0L || elapsed < 0L) return 0.0F;

        long swingStart = (long) (RISE_NANOS * 0.15D);
        long swingSpan = RISE_NANOS - swingStart;
        if (elapsed < swingStart) return 0.0F;
        if (elapsed < RISE_NANOS) {
            return easeOutCubic((elapsed - swingStart) / (float) swingSpan);
        }
        return 1.0F;
    }

    public static float punch() {
        long elapsed = elapsed();
        if (startNanos == 0L || elapsed < 0L) return 0.0F;

        long outSpan = (long) (RISE_NANOS * PUNCH_OUT_UNTIL);
        if (elapsed < outSpan) {
            return easeOutCubic(elapsed / (float) outSpan);
        }
        if (elapsed < RISE_NANOS + HOLD_NANOS) return 1.0F;

        long fall = elapsed - RISE_NANOS - HOLD_NANOS;
        long inSpan = (long) (FALL_NANOS * PUNCH_IN_UNTIL);
        if (fall >= inSpan) return 0.0F;

        return 1.0F - easeInOutCubic(fall / (float) inSpan);
    }

    public static void reset() {
        startNanos = 0L;
        lockedSlot = -1;
    }

    private static long elapsed() {
        return startNanos == 0L ? -1L : System.nanoTime() - startNanos;
    }

    private static float easeOutCubic(float t) {
        float value = 1.0F - t;
        return 1.0F - value * value * value;
    }

    private static float easeOutQuint(float t) {
        float value = 1.0F - t;
        return 1.0F - value * value * value * value * value;
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5F
                ? 4.0F * t * t * t
                : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 3.0F) / 2.0F;
    }
}
