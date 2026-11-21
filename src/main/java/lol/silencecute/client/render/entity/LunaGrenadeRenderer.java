package lol.silencecute.client.render.entity;



import lol.silencecute.client.render.item.LunaGrenadeModel;
import lol.silencecute.entity.LunaGrenadeEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.math.Axis;



import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.client.renderer.entity.EntityRenderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.phys.Vec3;





public final class LunaGrenadeRenderer extends EntityRenderer<LunaGrenadeEntity> {

    public LunaGrenadeRenderer(EntityRendererProvider.Context context) {

        super(context);

        this.shadowRadius = 0.13F;

    }



    @Override

    public void render(LunaGrenadeEntity grenade, float entityYaw, float partialTick,

                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {

        poseStack.pushPose();

        Vec3 movement = grenade.getDeltaMovement();

        double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);

        float yaw = (float) Math.toDegrees(Math.atan2(movement.x, movement.z));

        float pitch = (float) -Math.toDegrees(Math.atan2(movement.y, horizontal));

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));




        float rot = grenade.spinRot + (grenade.getDeltaMovement().lengthSqr() > 0.01D ? partialTick * 14.0F : 0.0F);

        poseStack.mulPose(Axis.ZP.rotationDegrees(rot));



        poseStack.translate(0.0D, 0.16D, 0.0D);

        poseStack.scale(1.08F, 1.08F, 1.08F);

        LunaGrenadeModel.render(poseStack, buffers, packedLight, true);

        poseStack.popPose();

        super.render(grenade, entityYaw, partialTick, poseStack, buffers, packedLight);

    }



    @Override

    public ResourceLocation getTextureLocation(LunaGrenadeEntity grenade) {

        return LunaGrenadeModel.TEXTURE;

    }

}


