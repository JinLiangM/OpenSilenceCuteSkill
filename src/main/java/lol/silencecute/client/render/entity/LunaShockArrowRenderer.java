package lol.silencecute.client.render.entity;

import lol.silencecute.Client;
import lol.silencecute.client.render.LunaVisualRenderTypes;
import lol.silencecute.client.render.item.LunaElectricArcRenderer;
import lol.silencecute.client.render.item.LunaElectricArrowModel;
import lol.silencecute.entity.LunaShockArrowEntity;
import lol.silencecute.manager.LunaShockArrowService;
import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexConsumer;

import com.mojang.math.Axis;

import lol.silencecute.config.ModConfigData;

import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.client.renderer.entity.EntityRenderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.util.Mth;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

public final class LunaShockArrowRenderer extends EntityRenderer<LunaShockArrowEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(

            Client.MOD_ID, "textures/item/luna_compound_bow_carbon.png");

    private static final float WORLD_SCALE = 0.72F;

    private static final float TIP_OFFSET = 0.22F;

    private static final int TRAIL_SAMPLES = 10;

    private static final Map<Integer, Trail> TRAILS = new HashMap<>();

    private static Level trailLevel;

    private static long lastCleanupTick;

    public LunaShockArrowRenderer(EntityRendererProvider.Context context) {

        super(context);

        this.shadowRadius = 0.0F;

    }

    @Override

    public void render(LunaShockArrowEntity arrow, float entityYaw, float partialTick,

                       PoseStack stack, MultiBufferSource buffers, int light) {

        Vec3 attached = arrow.attachedRenderPosition(partialTick);

        if (arrow.isAttached() && attached == null) return;

        Vec3 center = attached == null ? arrow.getPosition(partialTick) : attached;

        float yaw = attached == null ? Mth.rotLerp(partialTick, arrow.yRotO, arrow.getYRot())

                : arrow.attachedRenderYaw(partialTick);

        float pitch = Mth.lerp(partialTick, arrow.xRotO, arrow.getXRot());

        long seed = (arrow.level().getGameTime() / 5L) * 31L + arrow.getId() * 7919L;

        if (attached == null && !arrow.isLanded()) {

            sampleTrail(arrow, center);

        }

        stack.pushPose();

        try {

            if (attached != null) {

                Vec3 offset = attached.subtract(arrow.getPosition(partialTick));

                stack.translate(offset.x, offset.y, offset.z);

            }

            if (attached == null && !arrow.isLanded()) {

                renderFlyingTrail(stack, buffers, arrow, center);

            }

            stack.pushPose();

            try {

                stack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));

                stack.mulPose(Axis.XP.rotationDegrees(pitch));

                stack.translate(0, 0, LunaElectricArrowModel.LENGTH * WORLD_SCALE - TIP_OFFSET);

                stack.scale(WORLD_SCALE, WORLD_SCALE, WORLD_SCALE);

                LunaElectricArrowModel.render(stack, buffers, light);

                LunaElectricArcRenderer.renderChargedArrow(stack, buffers,

                        new Vec3(0, 0, -LunaElectricArrowModel.LENGTH),

                        new Vec3(0, 0, -0.18D), seed, 1.0F);

            } finally {

                stack.popPose();

            }

            if (arrow.isLanded() && attached == null) {

                if (ModConfigData.showShockRadius()) {

                    stack.pushPose();

                    stack.translate(0, 0.1, 0);

                    renderRadiusFill(stack, buffers);

                    stack.popPose();

                }

                LunaElectricArcRenderer.renderField(stack, buffers, arrow, center, seed);

            }

        } finally {

            stack.popPose();

        }

        super.render(arrow, entityYaw, partialTick, stack, buffers, light);

    }

    

    private static void renderRadiusFill(PoseStack stack, MultiBufferSource buffers) {

        VertexConsumer consumer = buffers.getBuffer(LunaVisualRenderTypes.ARC);

        Matrix4f matrix = stack.last().pose();

        int segments = 72;

        double radius = LunaShockArrowService.FIELD_RADIUS;

        int r = 255, g = 0, b = 0;

        int layers = 20;

        double layerRadius = radius / layers;

        for (int j = 0; j < layers; j++) {

            double rIn = j * layerRadius;

            double rOut = (j + 1) * layerRadius;

            int aIn = j * 2;

            int aOut = (j + 1) * 2;

            for (int i = 0; i < segments; i++) {

                double a1 = 2.0 * Math.PI * i / segments;

                double a2 = 2.0 * Math.PI * (i + 1) / segments;

                Vec3 pIn1 = new Vec3(Math.cos(a1) * rIn, 0, Math.sin(a1) * rIn);

                Vec3 pIn2 = new Vec3(Math.cos(a2) * rIn, 0, Math.sin(a2) * rIn);

                Vec3 pOut1 = new Vec3(Math.cos(a1) * rOut, 0, Math.sin(a1) * rOut);

                Vec3 pOut2 = new Vec3(Math.cos(a2) * rOut, 0, Math.sin(a2) * rOut);

                vertex(consumer, matrix, pIn1, r, g, b, aIn);

                vertex(consumer, matrix, pIn2, r, g, b, aIn);

                vertex(consumer, matrix, pOut2, r, g, b, aOut);

                vertex(consumer, matrix, pOut1, r, g, b, aOut);

            }

        }

    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 point, int r, int g, int b, int alpha) {

        consumer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)

                .color(r, g, b, alpha).endVertex();

    }

    private static void sampleTrail(LunaShockArrowEntity arrow, Vec3 position) {

        if (arrow.level() != trailLevel) {

            TRAILS.clear();

            trailLevel = arrow.level();

        }

        Trail trail = TRAILS.computeIfAbsent(arrow.getId(), id -> new Trail());

        long now = arrow.level().getGameTime();

        trail.lastSeenTick = now;

        if (trail.lastSampleTick == arrow.tickCount) {

            return;

        }

        trail.lastSampleTick = arrow.tickCount;

        trail.points.add(0, position);

        while (trail.points.size() > TRAIL_SAMPLES) {

            trail.points.remove(trail.points.size() - 1);

        }

        cleanupStale(now);

    }

    private static void cleanupStale(long now) {

        if (now - lastCleanupTick < 100L) {

            return;

        }

        lastCleanupTick = now;

        TRAILS.entrySet().removeIf(entry -> now - entry.getValue().lastSeenTick > 100L);

    }

    private static void renderFlyingTrail(PoseStack stack, MultiBufferSource buffers,

                                          LunaShockArrowEntity arrow, Vec3 center) {

        Trail trail = TRAILS.get(arrow.getId());

        if (trail == null || trail.points.size() < 2) {

            return;

        }

        List<Vec3> local = new ArrayList<>(trail.points.size());

        for (Vec3 point : trail.points) {

            local.add(point.subtract(center));

        }

        long seed = arrow.level().getGameTime() * 31L + arrow.getId() * 7919L;

        LunaElectricArcRenderer.renderTrail(stack, buffers, local, seed, 1.0F);

    }

    public static void clearVisual() {

        LunaElectricArrowModel.clearVisual();

        TRAILS.clear();

        trailLevel = null;

        lastCleanupTick = 0L;

    }

    @Override

    public ResourceLocation getTextureLocation(LunaShockArrowEntity arrow) {

        return TEXTURE;

    }

    private static final class Trail {

        private final List<Vec3> points = new ArrayList<>();

        private int lastSampleTick = Integer.MIN_VALUE;

        private long lastSeenTick;

    }

}

