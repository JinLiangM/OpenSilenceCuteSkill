package lol.silencecute.client.render.entity;

import lol.silencecute.client.render.item.LunaElectricArcRenderer;
import lol.silencecute.client.render.item.LunaElectricArrowModel;
import lol.silencecute.entity.LunaReconArrowEntity;
import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.client.renderer.entity.EntityRenderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.minecraft.client.renderer.texture.TextureAtlas;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.util.Mth;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.Vec3;

public final class LunaReconArrowRenderer extends EntityRenderer<LunaReconArrowEntity> {

    private static final float WORLD_SCALE = 0.72F;

    private static final float TIP_OFFSET = 0.22F;

    

    private static final int TRAIL_SAMPLES = 20;

    private static final Map<Integer, Trail> TRAILS = new HashMap<>();

    private static Level trailLevel;

    private static long lastCleanupTick;

    public LunaReconArrowRenderer(EntityRendererProvider.Context context) {

        super(context);

        this.shadowRadius = 0.0F;

    }

    @Override

    public void render(LunaReconArrowEntity arrow, float entityYaw, float partialTick,

                       PoseStack stack, MultiBufferSource buffers, int light) {

        Vec3 center = arrow.getPosition(partialTick);

        boolean flying = !arrow.isLanded();

        if (flying) {

            sampleTrail(arrow, center);

            renderFlyingTrail(stack, buffers, arrow, center);

        }

        float yaw = Mth.rotLerp(partialTick, arrow.yRotO, arrow.getYRot());

        float pitch = Mth.lerp(partialTick, arrow.xRotO, arrow.getXRot());

        stack.pushPose();

        try {

            stack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));

            stack.mulPose(Axis.XP.rotationDegrees(pitch));

            stack.translate(0, 0, LunaElectricArrowModel.LENGTH * WORLD_SCALE - TIP_OFFSET);

            stack.scale(WORLD_SCALE, WORLD_SCALE, WORLD_SCALE);

            LunaElectricArrowModel.render(stack, buffers, light);

            long seed = (arrow.level().getGameTime() / 5L) * 31L + arrow.getId() * 7919L;

            LunaElectricArcRenderer.renderReconChargedArrow(stack, buffers,

                    new Vec3(0, 0, -LunaElectricArrowModel.LENGTH),

                    new Vec3(0, 0, -0.18D), seed, 1.0F);

        } finally {

            stack.popPose();

        }

        super.render(arrow, entityYaw, partialTick, stack, buffers, light);

    }

    

    private static void sampleTrail(LunaReconArrowEntity arrow, Vec3 position) {

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

                                          LunaReconArrowEntity arrow, Vec3 center) {

        Trail trail = TRAILS.get(arrow.getId());

        if (trail == null || trail.points.size() < 2) {

            return;

        }

        List<Vec3> local = new ArrayList<>(trail.points.size());

        for (Vec3 point : trail.points) {

            local.add(point.subtract(center));

        }

        long seed = arrow.level().getGameTime() * 31L + arrow.getId() * 7919L;

        LunaElectricArcRenderer.renderReconTrail(stack, buffers, local, seed, 1.0F);

    }

    public static void clearVisual() {

        TRAILS.clear();

        trailLevel = null;

        lastCleanupTick = 0L;

    }

    @Override

    public ResourceLocation getTextureLocation(LunaReconArrowEntity entity) {

        return TextureAtlas.LOCATION_BLOCKS;

    }

    private static final class Trail {

        private final List<Vec3> points = new ArrayList<>();

        private int lastSampleTick = Integer.MIN_VALUE;

        private long lastSeenTick;

    }

}

