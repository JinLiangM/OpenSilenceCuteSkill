package lol.silencecute.util;

import lol.silencecute.manager.LunaGrenadeService;
import net.minecraft.util.Mth;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.level.ClipContext;

import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.HitResult;

import net.minecraft.world.phys.Vec3;

public final class LunaGrenadeBallistics {

    public static final float GRAVITY = 0.045F;

    

    private static final double RIGHT_HAND_OFFSET = 0.35D;

    private LunaGrenadeBallistics() {

    }

    public static Vec3 origin(LivingEntity player, float partialTick) {

        Vec3 eye = player.getEyePosition(partialTick);

        Vec3 look = player.getViewVector(partialTick);

        Vec3 right = rightHandHorizontal(look);

        return eye.add(

                look.x * 0.48D + right.x * RIGHT_HAND_OFFSET,

                -0.16D + look.y * 0.35D,

                look.z * 0.48D + right.z * RIGHT_HAND_OFFSET);

    }

    

    private static Vec3 rightHandHorizontal(Vec3 look) {

        double lx = look.x;

        double lz = look.z;

        double length = Math.sqrt(lx * lx + lz * lz);

        if (length < 1.0E-4D) {

            return new Vec3(-1.0D, 0.0D, 0.0D);

        }

        return new Vec3(-lz / length, 0.0D, lx / length);

    }

    

    public static Vec3 velocity(LivingEntity player, float partialTick) {

        float pitch = player.getViewXRot(partialTick);

        float yaw = player.getViewYRot(partialTick);

        Vec3 raw = new Vec3(

                -Mth.sin(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD),

                -Mth.sin((pitch + LunaGrenadeService.THROW_ARC_DEGREES) * Mth.DEG_TO_RAD),

                Mth.cos(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD))

                .normalize();

        double speed = LunaGrenadeService.THROW_VELOCITY;

        double vy = raw.y * speed;

        double vh = Math.sqrt(Math.max(0.0D, speed * speed - vy * vy));

        Vec3 eye = player.getEyePosition(partialTick);

        Vec3 look = player.getViewVector(partialTick);

        double estimatedRange = 100.0D;

        Vec3 far = eye.add(look.scale(128.0D));

        BlockHitResult hit = player.level().clip(new ClipContext(eye, far,

                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.MISS) {

            double dx = hit.getLocation().x - eye.x;

            double dz = hit.getLocation().z - eye.z;

            estimatedRange = Math.sqrt(dx * dx + dz * dz);

        }

        estimatedRange = Math.max(2.0D, Math.min(120.0D, estimatedRange));

        Vec3 lookH = new Vec3(look.x, 0.0D, look.z);

        if (lookH.lengthSqr() < 1.0E-6D) {

            lookH = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0.0D,

                    Mth.cos(yaw * Mth.DEG_TO_RAD));

        } else {

            lookH = lookH.normalize();

        }

        Vec3 origin = origin(player, partialTick);

        Vec3 targetH = new Vec3(eye.x + lookH.x * estimatedRange, 0.0D,

                eye.z + lookH.z * estimatedRange);

        Vec3 vhDir = new Vec3(targetH.x - origin.x, 0.0D, targetH.z - origin.z);

        if (vhDir.lengthSqr() < 1.0E-6D) {

            vhDir = lookH;

        } else {

            vhDir = vhDir.normalize();

        }

        Vec3 movement = player.getDeltaMovement();

        return new Vec3(vhDir.x * vh, vy, vhDir.z * vh)

                .add(movement.x, player.onGround() ? 0.0D : movement.y, movement.z);

    }

}

