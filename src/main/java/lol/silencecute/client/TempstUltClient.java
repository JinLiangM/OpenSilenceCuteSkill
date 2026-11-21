package lol.silencecute.client;

import lol.silencecute.api.Operator;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.network.TempstUltPacket;
import lol.silencecute.registry.ModNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class TempstUltClient {

    public static final boolean ENABLED = false;

    private static final long RISE_NANOS = 360_000_000L;

    private static final long HOLD_NANOS = 170_000_000L;

    private static final long FALL_NANOS = 560_000_000L;

    public static final long HAND_DELAY_NANOS = 60_000_000L;

    private static final float RIGHT_FALL_SCALE = 0.62F;

    private static final long TOTAL_NANOS =
            RISE_NANOS + HOLD_NANOS + FALL_NANOS + HAND_DELAY_NANOS;

    private static long startNanos;
    private static boolean pendingCast;

    private TempstUltClient() {
    }

    public static boolean isPlaying() {
        return startNanos != 0L && System.nanoTime() - startNanos < TOTAL_NANOS;
    }

    public static boolean isCasting() {
        return isPlaying();
    }

    public static float rightRaise() {
        return raiseAmount(0L, RIGHT_FALL_SCALE);
    }

    public static float leftRaise() {
        return raiseAmount(HAND_DELAY_NANOS, 1.0F);
    }

    private static float raiseAmount(long delayNanos, float fallScale) {
        if (startNanos == 0L) return 0.0F;

        long elapsed = System.nanoTime() - startNanos - delayNanos;
        if (elapsed <= 0L) return 0.0F;

        if (elapsed < RISE_NANOS) {
            return easeOutCubic(elapsed / (float) RISE_NANOS);
        }

        long afterRise = elapsed - RISE_NANOS;
        if (afterRise < HOLD_NANOS) return 1.0F;

        long fallLength = (long) (FALL_NANOS * fallScale);
        long fall = afterRise - HOLD_NANOS;
        if (fall >= fallLength) return 0.0F;

        float t = fall / (float) fallLength;
        return 1.0F - t * t;
    }

    public static void tryCast(Minecraft minecraft) {
        if (!ENABLED) {
            pendingCast = false;
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive()) return;
        if (!ModConfigData.operatorEnabled()) return;
        if (ModConfigData.operator() != Operator.JIFENG) return;

        if (TempstUltAnchorClient.isActive()) {
            TempstUltAnchorClient.reset();
            ModNetwork.CHANNEL.sendToServer(new TempstUltPacket());
            return;
        }

        if (ClientEvents.ultimateCooldownMillis() > 0L) {
            ClientEvents.playSkillUnavailableSound();
            return;
        }
        if (isPlaying()) return;

        if (!player.onGround()) {
            pendingCast = true;
            ClientEvents.playSkillUnavailableSound();
            return;
        }

        cast(player);
    }

    public static void tickPending(Minecraft minecraft) {
        if (!ENABLED) {
            pendingCast = false;
            return;
        }
        if (!pendingCast) return;

        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive()
                || ModConfigData.operator() != Operator.JIFENG
                || ClientEvents.ultimateCooldownMillis() > 0L) {
            pendingCast = false;
            return;
        }
        if (!player.onGround()) return;
        cast(player);
    }

    private static void cast(LocalPlayer player) {
        pendingCast = false;
        startNanos = System.nanoTime();
        TempstUltAnchorClient.begin(player);
        ModNetwork.CHANNEL.sendToServer(new TempstUltPacket());
    }

    public static void reset() {
        startNanos = 0L;
        pendingCast = false;
    }

    private static float easeOutCubic(float t) {
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv;
    }
}
