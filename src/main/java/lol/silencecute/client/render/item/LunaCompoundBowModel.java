package lol.silencecute.client.render.item;

import lol.silencecute.Client;
import lol.silencecute.client.LunaShockArrowClient;
import lol.silencecute.client.anim.LunaBowAnimation;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexConsumer;

import com.mojang.math.Axis;

import lol.silencecute.registry.ModItems;

import net.minecraft.client.Minecraft;

import net.minecraft.client.model.geom.ModelPart;

import net.minecraft.client.model.geom.PartPose;

import net.minecraft.client.player.AbstractClientPlayer;

import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import net.minecraft.client.renderer.texture.OverlayTexture;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.player.PlayerModelPart;

import net.minecraft.world.item.ItemDisplayContext;

import net.minecraft.world.item.ItemStack;

import org.joml.Quaternionf;

import org.joml.Vector3f;

public final class LunaCompoundBowModel {

    private static final ResourceLocation STRING_TEXTURE = ResourceLocation.fromNamespaceAndPath(

            Client.MOD_ID, "textures/item/luna_bow_green_cord.png");

    private static final float AIM_RIGHT_SHIFT = 0.08F;

    private static final float DRAW_VISUAL_EXPONENT = 0.5F;

    private static ItemStack animatedBow;

    private LunaCompoundBowModel() { }

    

    public static void render(PoseStack stack, MultiBufferSource buffers, int light,

                              AbstractClientPlayer player, LunaBowAnimation.Pose pose, boolean hasArrow) {

        renderInternal(stack, buffers, light, player, pose, hasArrow, true);

    }

    

    public static void renderNoArc(PoseStack stack, MultiBufferSource buffers, int light,

                                   AbstractClientPlayer player, LunaBowAnimation.Pose pose, boolean hasArrow) {

        renderInternal(stack, buffers, light, player, pose, hasArrow, false);

    }

    private static void renderInternal(PoseStack stack, MultiBufferSource buffers, int light,

                                       AbstractClientPlayer player, LunaBowAnimation.Pose pose,

                                       boolean hasArrow, boolean showArc) {

        Minecraft minecraft = Minecraft.getInstance();

        if (animatedBow == null) {

            animatedBow = new ItemStack(ModItems.LUNA_COMPOUND_BOW.get());

            animatedBow.getOrCreateTag().putBoolean("LunaAnimated", true);

        }

        stack.pushPose();

        stack.translate(pose.x() + pose.aimBlend() * AIM_RIGHT_SHIFT, pose.y(), pose.z());

        stack.scale(0.57F, 0.57F, 0.57F);

        Quaternionf bowRotation = Axis.YP.rotationDegrees(pose.yaw())

                .mul(Axis.XP.rotationDegrees(pose.pitch()))

                .mul(Axis.ZP.rotationDegrees(pose.roll()));

        stack.pushPose();

        stack.mulPose(bowRotation);

        minecraft.getItemRenderer().renderStatic(animatedBow, ItemDisplayContext.FIXED, light,

                OverlayTexture.NO_OVERLAY, stack, buffers, minecraft.level, 0);

        float nockY = 2.7F / 16.0F;

        float visualPull = (float) Math.pow(pose.stringPull(), DRAW_VISUAL_EXPONENT);

        float nockZ = (5.71F + 10.2F * visualPull) / 16.0F;

        VertexConsumer string = buffers.getBuffer(RenderType.entityCutoutNoCull(STRING_TEXTURE));

        stringSegment(stack, string, light, 18.4F / 16.0F, 5.71F / 16.0F, nockY, nockZ);

        stringSegment(stack, string, light, -18.4F / 16.0F, 5.71F / 16.0F, nockY, nockZ);

        if (hasArrow && minecraft.level != null) {

            stack.pushPose();

            stack.translate(0.0F, nockY, nockZ);

            LunaElectricArrowModel.render(stack, buffers, light);

            if (showArc) {

                int stage = LunaShockArrowClient.chargeStage();

                float intensity = switch (stage) {

                    case 1 -> 0.34F;

                    case 2 -> 0.67F;

                    case 3 -> 1.0F;

                    default -> 0.14F;

                };

                long sparkSeed = (minecraft.level.getGameTime() / 5L) * 31L + player.getId();

                LunaElectricArcRenderer.renderChargedArrow(stack, buffers,

                        new net.minecraft.world.phys.Vec3(0, 0, -LunaElectricArrowModel.LENGTH),

                        new net.minecraft.world.phys.Vec3(0, 0, -0.18D), sparkSeed, intensity);

            }

            stack.popPose();

        }

        stack.popPose();

        if (!player.isInvisible() && minecraft.getEntityRenderDispatcher().getRenderer(player)

                instanceof PlayerRenderer renderer) {

            float aim = pose.aimBlend();

            renderArm(stack, buffers, light, player, renderer, true,

                    bowRotation.transform(mix(new Vector3f(0.0F, -0.045F, 0.025F),

                            new Vector3f(0.0F, -0.015F, 0.035F), aim)),

                    armRotation(bowRotation, new Vector3f(0.68F, 0.65F, -0.34F),

                            new Vector3f(0.34F, 0.18F, -0.92F), aim),

                    1.0F + aim * 0.55F);

            renderArm(stack, buffers, light, player, renderer, false,

                    bowRotation.transform(mix(new Vector3f(0.08F, nockY - 0.012F, nockZ),

                            new Vector3f(0.08F + pose.drawHandRelease() * 0.24F,

                                    nockY - 0.012F, nockZ + pose.drawHandRelease() * 0.16F), aim)),

                    armRotation(bowRotation, new Vector3f(-0.65F, 0.56F, -0.48F),

                            new Vector3f(-0.56F, 0.14F, -0.82F), aim),

                    0.88F + aim * 0.12F);

        }

        stack.popPose();

    }

    private static Vector3f mix(Vector3f idle, Vector3f aimed, float amount) {

        return idle.lerp(aimed, amount);

    }

    private static Quaternionf armRotation(Quaternionf bowRotation, Vector3f idleDirection,

                                           Vector3f aimedDirection, float aim) {

        Quaternionf idle = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), idleDirection.normalize());

        Quaternionf aimed = new Quaternionf(bowRotation).mul(

                new Quaternionf().rotationTo(new Vector3f(0, 1, 0), aimedDirection.normalize()));

        return idle.slerp(aimed, aim);

    }

    private static void renderArm(PoseStack stack, MultiBufferSource buffers, int light,

                                  AbstractClientPlayer player, PlayerRenderer renderer, boolean left,

                                  Vector3f wrist, Quaternionf rotation, float lengthScale) {

        ModelPart arm = left ? renderer.getModel().leftArm : renderer.getModel().rightArm;

        ModelPart sleeve = left ? renderer.getModel().leftSleeve : renderer.getModel().rightSleeve;

        PartPose oldArm = arm.storePose();

        PartPose oldSleeve = sleeve.storePose();

        boolean armVisible = arm.visible;

        boolean sleeveVisible = sleeve.visible;

        stack.pushPose();

        try {

            stack.translate(wrist.x(), wrist.y(), wrist.z());

            stack.mulPose(rotation);

            stack.scale(1.0F, lengthScale, 1.0F);

            float centerX = "slim".equals(player.getModelName()) ? 0.5F : 1.0F;

            stack.translate((left ? -centerX : centerX) / 16.0F, -9.0F / 16.0F, 0.0F);

            arm.setPos(0, 0, 0);

            arm.setRotation(0, 0, 0);

            sleeve.setPos(0, 0, 0);

            sleeve.setRotation(0, 0, 0);

            arm.visible = true;

            sleeve.visible = player.isModelPartShown(left ? PlayerModelPart.LEFT_SLEEVE : PlayerModelPart.RIGHT_SLEEVE);

            arm.render(stack, buffers.getBuffer(RenderType.entitySolid(player.getSkinTextureLocation())),

                    light, OverlayTexture.NO_OVERLAY);

            sleeve.render(stack, buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation())),

                    light, OverlayTexture.NO_OVERLAY);

        } finally {

            arm.loadPose(oldArm);

            sleeve.loadPose(oldSleeve);

            arm.visible = armVisible;

            sleeve.visible = sleeveVisible;

            stack.popPose();

        }

    }

    private static void stringSegment(PoseStack stack, VertexConsumer consumer, int light,

                                      float y0, float z0, float y1, float z1) {

        float halfWidth = 0.0038F;

        float dy = y1 - y0;

        float dz = z1 - z0;

        float length = (float) Math.sqrt(dy * dy + dz * dz);

        float ny = -dz / length;

        float nz = dy / length;

        stringVertex(stack, consumer, light, -halfWidth, y0, z0, 0, 0, 0, ny, nz);

        stringVertex(stack, consumer, light, halfWidth, y0, z0, 1, 0, 0, ny, nz);

        stringVertex(stack, consumer, light, halfWidth, y1, z1, 1, 1, 0, ny, nz);

        stringVertex(stack, consumer, light, -halfWidth, y1, z1, 0, 1, 0, ny, nz);

        stringVertex(stack, consumer, light, 0, y0 - ny * halfWidth, z0 - nz * halfWidth, 0, 0, 1, 0, 0);

        stringVertex(stack, consumer, light, 0, y0 + ny * halfWidth, z0 + nz * halfWidth, 1, 0, 1, 0, 0);

        stringVertex(stack, consumer, light, 0, y1 + ny * halfWidth, z1 + nz * halfWidth, 1, 1, 1, 0, 0);

        stringVertex(stack, consumer, light, 0, y1 - ny * halfWidth, z1 - nz * halfWidth, 0, 1, 1, 0, 0);

    }

    private static void stringVertex(PoseStack stack, VertexConsumer consumer, int light,

                                     float x, float y, float z, float u, float v,

                                     float nx, float ny, float nz) {

        consumer.vertex(stack.last().pose(), x, y, z).color(255, 255, 255, 255).uv(u, v)

                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)

                .normal(stack.last().normal(), nx, ny, nz).endVertex();

    }

}

