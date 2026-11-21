package lol.silencecute.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import lol.silencecute.Client;
import lol.silencecute.api.Operator;
import lol.silencecute.client.C4ThrowClient;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class C4ThrowHandRenderer {


    private static final float SHOULDER_Y = -0.74F;
    private static final float SHOULDER_Z = -0.28F;

    private static final float SWING_START_X = 0.26F;
    private static final float SWING_END_X = -0.32F;

    private static final float SWING_START_YAW = -54.0F;
    private static final float SWING_END_YAW = 38.0F;

    private static final float SWING_END_LIFT = 0.06F;

    private static final float PUNCH_FORWARD = 0.18F;

    private static final float ARM_SCALE = 1.05F;

    private static final float LIFT_DOWN = 38.0F;
    private static final float LIFT_UP = 120.0F;

    private static final float DROP = 0.32F;

    private static final float FORWARD = 0.06F;

    private static final float ELBOW_IN = 10.0F;
    private static final float HAND_IN = 14.0F;

    private static PlayerModel<?> model;
    private static boolean slim;

    private C4ThrowHandRenderer() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!C4ThrowClient.isPlaying()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (ModConfigData.operator() != Operator.WEILONG) return;
        if (player.isInvisible()) return;
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        if (C4ThrowClient.extend() < 0.08F) return;

        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        PlayerModel<?> playerModel = model(player);
        if (playerModel == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        ResourceLocation skin = player.getSkinTextureLocation();
        VertexConsumer skinBuffer = buffers.getBuffer(RenderType.entityTranslucent(skin));

        renderArm(poseStack, skinBuffer, light,
                playerModel.leftArm, playerModel.leftSleeve, -1.0F,
                C4ThrowClient.extend(), C4ThrowClient.swing());
    }

    private static void renderArm(PoseStack poseStack, VertexConsumer skinBuffer,
                                  int light, ModelPart arm, ModelPart sleeve,
                                  float side, float extend, float swing) {
        float lift = Mth.lerp(extend, LIFT_DOWN, LIFT_UP);
        float sink = (1.0F - extend) * DROP;
        float anchorX = Mth.lerp(swing, SWING_START_X, SWING_END_X);
        float yaw = Mth.lerp(swing, SWING_START_YAW, SWING_END_YAW);
        float punch = C4ThrowClient.punch();

        poseStack.pushPose();
        poseStack.translate(anchorX,
                SHOULDER_Y - sink + SWING_END_LIFT * swing,
                SHOULDER_Z - FORWARD * extend - PUNCH_FORWARD * punch);
        poseStack.mulPose(Axis.XP.rotationDegrees(lift));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-side * ELBOW_IN * extend));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + side * HAND_IN * extend));

        poseStack.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        renderPart(poseStack, arm, skinBuffer, light);
        if (sleeve != null) {
            renderPart(poseStack, sleeve, skinBuffer, light);
        }
        poseStack.popPose();
    }

    private static void renderPart(PoseStack poseStack, ModelPart part,
                                   VertexConsumer buffer, int light) {
        part.xRot = 0.0F;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
        poseStack.pushPose();
        poseStack.translate(-part.x / 16.0F, -part.y / 16.0F, -part.z / 16.0F);
        part.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static PlayerModel<?> model(LocalPlayer player) {
        boolean wantSlim = "slim".equals(player.getModelName());
        if (model != null && slim == wantSlim) return model;

        try {
            ModelPart root = Minecraft.getInstance().getEntityModels()
                    .bakeLayer(wantSlim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER);
            model = new PlayerModel<>(root, wantSlim);
            slim = wantSlim;
        } catch (Throwable t) {
            model = null;
        }
        return model;
    }
}
