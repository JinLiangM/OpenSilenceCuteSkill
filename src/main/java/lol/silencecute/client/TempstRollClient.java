package lol.silencecute.client;

import lol.silencecute.api.DashMode;
import lol.silencecute.api.Operator;
import lol.silencecute.client.compat.TaczCompat;
import lol.silencecute.client.compat.YsmCompat;
import lol.silencecute.client.lang.Lang;
import lol.silencecute.client.render.hud.notification.NotificationRea1;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.manager.DashService;
import lol.silencecute.manager.TempstRollService;
import lol.silencecute.network.DashPacket;
import lol.silencecute.registry.ModNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public final class TempstRollClient {

    private static double[] steps = new double[0];
    private static int ticks;
    private static int totalTicks;
    private static int index;
    private static double dirX;
    private static double dirZ;
    private static float originalStepHeight = 0.6F;

    private static long rollStartNanos;
    private static long rollDurationNanos;

    private static boolean pending;

    private static final float EYE_DIP = 0.75F;

    private static java.lang.reflect.Field eyeHeightField;
    private static boolean eyeFieldUnavailable;
    private static float baseEyeHeight = 1.62F;

    private static boolean taczAimSuspended;

    private TempstRollClient() {
    }

    public static boolean isRolling() {
        return ticks > 0;
    }

    public static float dirX() {
        return (float) dirX;
    }

    public static float dirZ() {
        return (float) dirZ;
    }

    public static boolean isRollingFor(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null
                && minecraft.player.getId() == entityId) {
            return ticks > 0;
        }
        return TempstRollFx.roll(entityId) != null;
    }

    public static boolean isPending() {
        return pending;
    }

    public static float localProgress() {
        if (rollDurationNanos <= 0L) return -1.0F;
        long elapsed = System.nanoTime() - rollStartNanos;
        if (elapsed >= rollDurationNanos) return 1.0F;
        return (float) ((double) elapsed / (double) rollDurationNanos);
    }

    public static float progressFor(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null
                && minecraft.player.getId() == entityId) {
            float local = localProgress();
            if (local >= 0.0F) return Math.min(1.0F, local);
            if (ticks > 0 && totalTicks > 0) {
                float done = (totalTicks - ticks) / (float) totalTicks;
                return Math.max(0.0F, Math.min(1.0F, done));
            }
        }
        TempstRollFx.Roll roll = TempstRollFx.roll(entityId);
        return roll == null ? -1.0F : roll.progress();
    }

    public static void tryRoll(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || player.isPassenger()) return;
        if (ModConfigData.operator() != Operator.JIFENG) return;
        if (isRolling()) return;
        if (ClientEvents.cooldownMillis() > 0L) {
            ClientEvents.playSkillUnavailableSound();
            return;
        }

        if (player.isInWater() || player.isSwimming()) {
            pending = false;
            notifyWaterBlocked();
            return;
        }

        if (player.getPose() == Pose.SWIMMING || player.isVisuallyCrawling()) {
            return;
        }


        if (!player.onGround()) {
            pending = true;
            ClientEvents.playSkillUnavailableSound();
            return;
        }
        start(minecraft, player);
    }

    private static void notifyWaterBlocked() {
        ClientNotice.warn(Lang.tr("notif.skill.no_water"));
        ClientEvents.playSkillUnavailableSound();
    }

    private static void start(Minecraft minecraft, LocalPlayer player) {
        Vec3 direction = ClientEvents.dashDirection(player, DashMode.FOUR_WAY);
        double length = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (!Double.isFinite(length) || length < 0.1D) return;

        dirX = direction.x / length;
        dirZ = direction.z / length;

        TempstRollService.Profile profile = TempstRollService.profile(player, dirX, dirZ);
        steps = profile.steps();
        ticks = profile.ticks();
        totalTicks = profile.ticks();
        index = 0;
        pending = false;
        originalStepHeight = player.maxUpStep();
        baseEyeHeight = player.getEyeHeight();

        rollStartNanos = System.nanoTime();
        rollDurationNanos = Math.max(1, ticks) * 50_000_000L;

        player.setDeltaMovement(Vec3.ZERO);
        player.refreshDimensions();

        taczAimSuspended = TaczCompat.suspendAim(player);

        TempstRollFx.track(player.getId(), ticks, (float) dirX, (float) dirZ);
        ModNetwork.CHANNEL.sendToServer(new DashPacket((float) direction.x, (float) direction.z));
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            reset();
            return;
        }
        if (ModConfigData.operator() != Operator.JIFENG) {
            if (ticks > 0) {
                end(player);
            } else {
                reset();
            }
            return;
        }

        if (pending) {
            if (ClientEvents.cooldownMillis() > 0L
                    || player.isInWater() || player.isSwimming()) {
                pending = false;
            } else if (player.onGround() && player.isAlive()) {
                start(minecraft, player);
            }
            return;
        }

        if (ticks <= 0) return;

        if (!player.isAlive()) {
            end(player);
            return;
        }

        double step = steps.length == 0
                ? 0.0D : steps[Math.min(index, steps.length - 1)];
        double stepX = dirX * step;
        double stepZ = dirZ * step;

        boolean stepAhead = DashService.hasDashStepAhead(player, stepX, stepZ);
        player.setMaxUpStep(stepAhead ? DashService.DASH_STEP_HEIGHT : originalStepHeight);
        if (stepAhead) player.setOnGround(true);

        if (player.horizontalCollision && !stepAhead && index >= 2) {
            end(player);
            return;
        }

        double dy = stepAhead ? Math.max(player.getDeltaMovement().y, 0.35D)
                : Math.min(player.getDeltaMovement().y, 0.0D);
        player.setDeltaMovement(stepX, dy, stepZ);

        float progress = localProgress();
        if (progress >= 0.0F && progress < 1.0F) {
            applyEyeDip(player, 1.0F - EYE_DIP * (float) Math.sin(Math.PI * progress));
        }

        index++;
        ticks--;
        if (ticks <= 0) end(player);
    }

    private static void end(LocalPlayer player) {
        ticks = 0;
        pending = false;
        index = 0;
        steps = new double[0];
        totalTicks = 0;
        rollDurationNanos = 0L;
        if (player != null) {
            player.setMaxUpStep(originalStepHeight);
            applyEyeDip(player, 1.0F);
            player.refreshDimensions();
        }
        if (taczAimSuspended) {
            taczAimSuspended = false;
            if (player != null && player.isAlive()) {
                TaczCompat.restoreAim(Minecraft.getInstance(), player);
            }
        }
    }

    private static void applyEyeDip(LocalPlayer player, float factor) {
        if (player == null) return;
        if (eyeHeightField == null && !eyeFieldUnavailable) resolveEyeField(player);
        if (eyeHeightField == null) return;
        try {
            eyeHeightField.setFloat(player, baseEyeHeight * factor);
        } catch (Throwable t) {
            eyeFieldUnavailable = true;
            eyeHeightField = null;
        }
    }

    private static void resolveEyeField(LocalPlayer player) {
        try {
            float expected = player.getEyeHeight();
            for (java.lang.reflect.Field field : net.minecraft.world.entity.Entity.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                if (field.getType() != float.class) continue;
                field.setAccessible(true);
                float value;
                try {
                    value = field.getFloat(player);
                } catch (Throwable t) {
                    continue;
                }
                if (Math.abs(value - expected) > 0.01F) continue;

                field.setFloat(player, expected - 0.5F);
                boolean ok = Math.abs(player.getEyeHeight() - (expected - 0.5F)) < 0.01F;
                field.setFloat(player, expected);
                if (ok) {
                    eyeHeightField = field;
                    return;
                }
            }
            eyeFieldUnavailable = true;
        } catch (Throwable t) {
            eyeFieldUnavailable = true;
        }
    }

    private static void reset() {
        ticks = 0;
        index = 0;
        pending = false;
        totalTicks = 0;
        rollDurationNanos = 0L;
        steps = new double[0];
    }
}
