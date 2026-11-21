package lol.silencecute.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import lol.silencecute.client.render.item.C4Model;
import lol.silencecute.entity.MagneticBombEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class MagneticBombRenderer extends EntityRenderer<MagneticBombEntity> {

    private static final double STUCK_RENDER_OFFSET = 0.15D;

    public MagneticBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.18F;
    }

    @Override
    public void render(MagneticBombEntity bomb, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();

        if (bomb.isStuck()) {
            Direction stuckFace = bomb.getStuckFace();
            if (stuckFace != null) {
                poseStack.translate(
                        -stuckFace.getStepX() * STUCK_RENDER_OFFSET,
                        (1 - stuckFace.getStepY()) * STUCK_RENDER_OFFSET,
                        -stuckFace.getStepZ() * STUCK_RENDER_OFFSET);
            }
        }

        float yaw = Mth.rotLerp(partialTick, bomb.yRotO, bomb.getYRot());
        float pitch = Mth.rotLerp(partialTick, bomb.xRotO, bomb.getXRot());
        poseStack.mulPose(Axis.YN.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        C4Model.render(poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        super.render(bomb, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MagneticBombEntity bomb) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
