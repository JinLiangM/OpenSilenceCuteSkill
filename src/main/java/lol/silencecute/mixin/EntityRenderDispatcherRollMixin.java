package lol.silencecute.mixin;

import java.util.ArrayDeque;
import java.util.Deque;

import com.mojang.blaze3d.vertex.PoseStack;

import lol.silencecute.client.TempstRollClient;
import lol.silencecute.client.TempstRollFx;
import lol.silencecute.client.TempstUltPullLock;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherRollMixin {

    private static final double PIVOT_Y = 0.55D;

    private static final double SINK_EDGE = 0.45D;

    private static final double SINK_MID = 0.15D;

    private static final double LIE_PIVOT_Y = 0.9D;
    private static final double LIE_DROP = 0.75D;

    private static final Deque<Integer> STACK = new ArrayDeque<>();

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("silencecute_skill");
    private static boolean probeRendered;
    private static boolean probeRolling;

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void silencecute$beginRoll(Entity entity, double x, double y, double z, float rotationYaw,
                                       float partialTicks, PoseStack poseStack,
                                       MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!probeRendered) {
            probeRendered = true;
            LOGGER.info("[TempstRoll] mixin active");
        }

        lol.silencecute.client.RollPose.clear();

        boolean applied = false;
        try {
            float progress = TempstRollClient.progressFor(entity.getId());
            if (progress > 0.0F && !probeRolling) {
                probeRolling = true;
                LOGGER.info("[TempstRoll] progress=" + progress);
            }

            if (progress > 0.0F && progress < 1.0F
                    && lol.silencecute.client.compat.YsmCompat.useYsmAnimation()) {
                float dirX = TempstRollClient.dirX();
                float dirZ = TempstRollClient.dirZ();

                TempstRollFx.Roll roll = TempstRollFx.roll(entity.getId());
                if (roll != null) {
                    dirX = roll.dirX();
                    dirZ = roll.dirZ();
                }

                double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
                if (Double.isFinite(length) && length > 1.0E-4D) {
                    float axisX = (float) (dirZ / length);
                    float axisZ = (float) (-dirX / length);
                    float angle = (float) Math.toRadians(360.0D * progress);
                    double phase = Math.abs(2.0D * progress - 1.0D);
                    double sink = SINK_MID + SINK_EDGE * phase * phase;

                    poseStack.pushPose();
                    poseStack.translate(0.0D, -sink, 0.0D);
                    poseStack.translate(x, y + PIVOT_Y, z);
                    poseStack.mulPose(new Quaternionf().rotateAxis(angle, axisX, 0.0F, axisZ));
                    poseStack.translate(-x, -(y + PIVOT_Y), -z);
                    applied = true;
                    lol.silencecute.client.RollPose.markApplied(entity.getId());
                }
            }
        } catch (Throwable ignored) {
            applied = false;
        }

        if (!applied) {
            try {
                if (TempstUltPullLock.isLocked() && entity instanceof Player) {
                    double yawRad = Math.toRadians(entity.getYRot());
                    float faceX = (float) -Math.sin(yawRad);
                    float faceZ = (float) Math.cos(yawRad);

                    Quaternionf lie = new Quaternionf().rotationTo(
                            new Vector3f(0.0F, -1.0F, 0.0F),
                            new Vector3f(faceX, 0.0F, faceZ));

                    poseStack.pushPose();
                    poseStack.translate(0.0D, -LIE_DROP, 0.0D);
                    poseStack.translate(x, y + LIE_PIVOT_Y, z);
                    poseStack.mulPose(lie);
                    poseStack.translate(-x, -(y + LIE_PIVOT_Y), -z);
                    applied = true;
                }
            } catch (Throwable ignored) {
                applied = false;
            }
        }

        STACK.push(applied ? entity.getId() : -1);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void silencecute$endRoll(Entity entity, double x, double y, double z, float rotationYaw,
                                     float partialTicks, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (STACK.isEmpty()) return;
        int id = STACK.pop();
        if (id < 0) return;
        try {
            poseStack.popPose();
        } catch (Throwable ignored) {
        }
    }
}
