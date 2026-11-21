package lol.silencecute.client.render.world;

import lol.silencecute.Client;
import lol.silencecute.client.LunaRevealClient;
import lol.silencecute.client.render.LunaVisualRenderTypes;
import lol.silencecute.config.ModConfigData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.Team;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class LunaRevealMarkerRenderer {

    private static final double PULSE_HZ = 3.5D;
    private static final float MARKER_BASE_RADIUS = 0.18F;
    private static final float MARKER_PULSE_RADIUS = 0.03F;
    private static final float INNER_SCALE = 0.65F;
    private static final int MARKER_ALPHA = 220;

    private static final double FADE_NEAR = 2.0D;
    private static final double FADE_FAR = 5.0D;

    private LunaRevealMarkerRenderer() { }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        List<Integer> ids = LunaRevealClient.getActiveRevealedEntityIds();
        if (ids.isEmpty()) return;
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        float partialTick = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        renderSilhouette(minecraft, ids, partialTick, poseStack, cameraPos);
        if (ModConfigData.chamsMarker()) renderHeadMarkers(minecraft, ids, partialTick, poseStack, cameraPos);
    }

    private static void renderSilhouette(Minecraft minecraft, List<Integer> ids,
                                         float partialTick, PoseStack poseStack, Vec3 cameraPos) {
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        poseStack.pushPose();
        try {
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            for (Integer entityId : ids) {
                Entity entity = minecraft.level.getEntity(entityId);
                if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

                EntityRenderer<?> untypedRenderer = dispatcher.getRenderer(living);
                if (!(untypedRenderer instanceof LivingEntityRenderer)) continue;

                @SuppressWarnings("unchecked")
                EntityRenderer<LivingEntity> renderer = (EntityRenderer<LivingEntity>) untypedRenderer;

                Vec3 pos = living.getPosition(partialTick);

                ResourceLocation texture = renderer.getTextureLocation(living);
                if (texture == null) texture = net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;

                RenderType chamsType = LunaVisualRenderTypes.getChamsType(texture);

                int r = (ModConfigData.chamsColor() >> 16) & 0xFF;
                int g = (ModConfigData.chamsColor() >> 8) & 0xFF;
                int b = ModConfigData.chamsColor() & 0xFF;
                if (ModConfigData.chamsTeamColor()) {
                    Team team = living.getTeam();
                    if (team != null && team.getColor().getColor() != null) {
                        int teamRgb = team.getColor().getColor();
                        r = (teamRgb >> 16) & 0xFF;
                        g = (teamRgb >> 8) & 0xFF;
                        b = teamRgb & 0xFF;
                    }
                }

                int finalR = r, finalG = g, finalB = b, finalA = ModConfigData.chamsAlpha();

                MultiBufferSource.BufferSource localBuffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

                MultiBufferSource tint = type -> {
                    if (type.format() == DefaultVertexFormat.NEW_ENTITY && type.mode() == VertexFormat.Mode.QUADS
                            && type != RenderType.entityGlint() && type != RenderType.entityGlintDirect()
                            && type != RenderType.armorGlint() && type != RenderType.armorEntityGlint()
                            && !type.toString().contains("item")) {
                        return new RevealTexConsumer(localBuffers.getBuffer(chamsType), finalR, finalG, finalB, finalA);
                    }
                    return RevealConsumer.DISCARD;
                };

                poseStack.pushPose();
                try {
                    poseStack.translate(pos.x, pos.y, pos.z);
                    float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
                    renderer.render(living, bodyYaw, partialTick, poseStack, tint, 0xF000F0);
                } finally {
                    poseStack.popPose();
                }

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                localBuffers.endBatch();
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static final class RevealTexConsumer implements VertexConsumer {
        static final RevealTexConsumer DISCARD = new RevealTexConsumer(null, 0, 0, 0, 0);
        private final VertexConsumer target;
        private final int r, g, b, a;

        RevealTexConsumer(VertexConsumer target, int r, int g, int b, int a) {
            this.target = target;
            this.r = r; this.g = g; this.b = b; this.a = a;
        }
        @Override public VertexConsumer vertex(double x, double y, double z) { if (target != null) target.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int cr, int cg, int cb, int ca) { if (target != null) target.color(r, g, b, a); return this; }
        @Override public VertexConsumer uv(float u, float v) { if (target != null) target.uv(u, v); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { if (target != null) target.overlayCoords(u, v); return this; }
        @Override public VertexConsumer uv2(int u, int v) { if (target != null) target.uv2(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { if (target != null) target.normal(x, y, z); return this; }
        @Override public void endVertex() { if (target != null) target.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) {}
        @Override public void unsetDefaultColor() {}
    }

    private static final class RevealConsumer implements VertexConsumer {
        static final RevealConsumer DISCARD = new RevealConsumer(null, 0);
        private final VertexConsumer target;
        private final int alpha;
        private int vertexAlpha;
        RevealConsumer(VertexConsumer target, int alpha) { this.target = target; this.alpha = alpha; this.vertexAlpha = alpha; }
        @Override public VertexConsumer vertex(double x, double y, double z) { if (target != null) target.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { vertexAlpha = alpha * a / 255; return this; }
        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() { if (target != null) target.color(255, 88, 24, vertexAlpha).endVertex(); vertexAlpha = alpha; }
        @Override public void defaultColor(int r, int g, int b, int a) { color(r, g, b, a); }
        @Override public void unsetDefaultColor() { vertexAlpha = alpha; }
    }

    private static void renderHeadMarkers(Minecraft minecraft, List<Integer> ids,
                                          float partialTick, PoseStack poseStack, Vec3 cameraPos) {
        double time = (minecraft.level.getGameTime() + partialTick) / 20.0D;
        double pulse = (Math.sin(time * PULSE_HZ * Math.PI * 2.0D) + 1.0D) * 0.5D;

        float radius = MARKER_BASE_RADIUS + MARKER_PULSE_RADIUS * (float) pulse;

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        boolean hasVertex = false;

        for (Integer entityId : ids) {
            Entity entity = minecraft.level.getEntity(entityId);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

            Vec3 pos = living.getPosition(partialTick);

            Vec3 center = new Vec3(pos.x, pos.y + living.getBbHeight() + 0.25D, pos.z);

            float distanceAlpha = distanceAlpha(cameraPos, center);
            if (distanceAlpha <= 0.01F) continue;

            Vec3 toCam = cameraPos.subtract(center).normalize();
            Vec3 right = toCam.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
            right = right.normalize();
            Vec3 up = right.cross(toCam).normalize();

            int alpha = (int) (MARKER_ALPHA * distanceAlpha);

            emitDiamond(builder, matrix, center, right, up, radius, 255, 0, 0, alpha);

            emitDiamond(builder, matrix, center, right, up, radius * INNER_SCALE, 0, 0, 0, alpha);

            hasVertex = true;
        }
        poseStack.popPose();
        if (hasVertex) flush(builder);
        else builder.end().release();
    }

    private static float distanceAlpha(Vec3 cameraPos, Vec3 target) {
        return (float) Math.min(1.0D, Math.max(0.0D,
                (cameraPos.distanceTo(target) - FADE_NEAR) / (FADE_FAR - FADE_NEAR)));
    }

    private static void emitDiamond(VertexConsumer consumer, Matrix4f matrix,
                                    Vec3 center, Vec3 right, Vec3 up, float radius,
                                    int r, int g, int b, int a) {
        if (a <= 0) return;

        Vec3 top = center.add(up.scale(radius));
        Vec3 bottom = center.subtract(up.scale(radius));
        Vec3 left = center.subtract(right.scale(radius));
        Vec3 rightV = center.add(right.scale(radius));

        consumer.vertex(matrix, (float)top.x, (float)top.y, (float)top.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float)rightV.x, (float)rightV.y, (float)rightV.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float)bottom.x, (float)bottom.y, (float)bottom.z).color(r, g, b, a).endVertex();

        consumer.vertex(matrix, (float)top.x, (float)top.y, (float)top.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float)bottom.x, (float)bottom.y, (float)bottom.z).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, (float)left.x, (float)left.y, (float)left.z).color(r, g, b, a).endVertex();
    }

    private static void flush(BufferBuilder builder) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            BufferUploader.drawWithShader(builder.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }
}