package lol.silencecute.client.render;

import lol.silencecute.Client;
import lol.silencecute.api.Operator;
import lol.silencecute.client.TempstUltClient;
import lol.silencecute.config.ModConfigData;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

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
public final class TempstUltHandRenderer {

    private static final float SHOULDER_X = 0.36F;
    private static final float SHOULDER_Y = -0.50F;
    private static final float SHOULDER_Z = -0.45F;

    private static final float ARM_SCALE = 0.72F;

    private static final float LIFT_DOWN = 6.0F;
    private static final float LIFT_UP = 148.0F;

    private static final float DROP = 0.55F;

    private static final float ELBOW_IN = 4.0F;
    private static final float HAND_IN = 28.0F;

    private static final boolean TUNE_STATIC = false;

    private static PlayerModel<?> model;
    private static boolean slim;

    private TempstUltHandRenderer() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!TUNE_STATIC && !TempstUltClient.isPlaying()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (ModConfigData.operator() != Operator.JIFENG) return;
        if (player.isInvisible()) return;
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

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
                playerModel.rightArm, playerModel.rightSleeve, 1.0F,
                TUNE_STATIC ? 1.0F : TempstUltClient.rightRaise());
        renderArm(poseStack, skinBuffer, light,
                playerModel.leftArm, playerModel.leftSleeve, -1.0F,
                TUNE_STATIC ? 1.0F : TempstUltClient.leftRaise());
    }

    private static void renderArm(PoseStack poseStack, VertexConsumer skinBuffer,
                                  int light, ModelPart arm, ModelPart sleeve, float side, float raise) {
        float lift = Mth.lerp(raise, LIFT_DOWN, LIFT_UP);
        float sink = (1.0F - raise) * DROP;

        poseStack.pushPose();
        poseStack.translate(side * SHOULDER_X, SHOULDER_Y - sink, SHOULDER_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(lift));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-side * ELBOW_IN));
        poseStack.mulPose(Axis.YP.rotationDegrees(-side * HAND_IN));

        poseStack.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        renderPart(poseStack, arm, skinBuffer, light);
        if (sleeve != null) {
            renderPart(poseStack, sleeve, skinBuffer, light);
        }
        poseStack.popPose();
    }

    private static void renderPart(PoseStack poseStack, ModelPart part, VertexConsumer buffer, int light) {
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
