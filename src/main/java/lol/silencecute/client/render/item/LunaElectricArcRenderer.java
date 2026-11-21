package lol.silencecute.client.render.item;

import lol.silencecute.client.render.LunaVisualRenderTypes;
import lol.silencecute.entity.LunaShockArrowEntity;
import lol.silencecute.manager.LunaShockArrowService;
import java.util.List;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.world.level.ClipContext;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.HitResult;

import net.minecraft.world.phys.Vec3;

public final class LunaElectricArcRenderer {

    

    static final double FIELD_VISUAL_RADIUS = 2.0D;

    private LunaElectricArcRenderer() { }

    public static void renderChargedArrow(PoseStack stack, MultiBufferSource buffers, Vec3 tip,

                                          Vec3 shaftEnd, long seed, float intensity) {

        renderChargedArrowInternal(stack, buffers, tip, shaftEnd, seed, intensity, false);

    }

    

    public static void renderReconChargedArrow(PoseStack stack, MultiBufferSource buffers, Vec3 tip,

                                               Vec3 shaftEnd, long seed, float intensity) {

        renderChargedArrowInternal(stack, buffers, tip, shaftEnd, seed, intensity, true);

    }

    private static void renderChargedArrowInternal(PoseStack stack, MultiBufferSource buffers,

                                                   Vec3 tip, Vec3 shaftEnd, long seed,

                                                   float intensity, boolean recon) {

        float glow = Math.max(0.14F, Math.min(1.0F, intensity));

        Random random = new Random(seed);

        VertexConsumer consumer = buffers.getBuffer(LunaVisualRenderTypes.ARC);

        for (int spark = 0; spark < 4; spark++) {

            double angle = spark * Math.PI * 0.5D + (random.nextDouble() - 0.5D) * 0.42D;

            Vec3 radial = new Vec3(Math.cos(angle), Math.sin(angle), 0.0D);

            Vec3 start = tip.add(radial.scale(0.025D + random.nextDouble() * 0.018D));

            Vec3 end = tip.add(radial.scale((0.095D + random.nextDouble() * 0.075D) * glow))

                    .add(0.0D, 0.0D, (random.nextDouble() - 0.5D) * 0.09D);

            renderArc(stack, consumer, start, end, random, 0.025D, 0.0042D, glow, recon);

        }

        for (int arc = 0; arc < 2; arc++) {

            double alongA = 0.14D + random.nextDouble() * 0.26D;

            double alongB = Math.min(0.92D, alongA + 0.34D + random.nextDouble() * 0.20D);

            Vec3 start = shaftEnd.lerp(tip, alongA).add(randomOffset(random, 0.025D));

            Vec3 end = shaftEnd.lerp(tip, alongB).add(randomOffset(random, 0.025D));

            renderArc(stack, consumer, start, end, random, 0.021D, 0.0032D, glow, recon);

        }

    }

    static void renderTip(PoseStack stack, MultiBufferSource buffers, Vec3 tip, long seed) {

        renderChargedArrow(stack, buffers, tip, tip.add(0.0D, 0.0D, 0.36D), seed, 1.0F);

    }

    public static void renderTrail(PoseStack stack, MultiBufferSource buffers, List<Vec3> points,

                                   long seed, float intensity) {

        renderTrailInternal(stack, buffers, points, seed, intensity, false);

    }

    

    public static void renderReconTrail(PoseStack stack, MultiBufferSource buffers, List<Vec3> points,

                                        long seed, float intensity) {

        renderTrailInternal(stack, buffers, points, seed, intensity, true);

    }

    private static void renderTrailInternal(PoseStack stack, MultiBufferSource buffers,

                                            List<Vec3> points, long seed, float intensity,

                                            boolean recon) {

        int segments = points.size() - 1;

        if (segments < 1) {

            return;

        }

        VertexConsumer consumer = buffers.getBuffer(LunaVisualRenderTypes.ARC);

        Random random = new Random(seed ^ 0x5A17C3E9L);

        for (int i = 0; i < segments; i++) {

            float fade = 1.0F - i / (float) segments;

            if (fade <= 0.05F) {

                continue;

            }

            Vec3 head = points.get(i);

            Vec3 tail = points.get(i + 1);

            if (head.distanceToSqr(tail) < 1.0E-6D) {

                continue;

            }

            renderArc(stack, consumer, tail, head, random,

                    0.016D + 0.030D * fade,

                    0.010D + 0.034D * fade,

                    intensity * (0.32F + 0.68F * fade),

                    recon);

        }

    }

    

    public static void renderField(PoseStack stack, MultiBufferSource buffers, LunaShockArrowEntity arrow,

                                   Vec3 center, long seed) {

        Random random = new Random(seed ^ 0x71C4A921L);

        VertexConsumer consumer = buffers.getBuffer(LunaVisualRenderTypes.ARC);

        for (int arc = 0; arc < 7; arc++) {

            double angle = random.nextDouble() * Math.PI * 2.0D;

            double radius = 0.25D + random.nextDouble() * 1.40D;

            Vec3 start = groundPoint(arrow.level(), arrow, center,

                    Math.cos(angle) * radius, Math.sin(angle) * radius);

            Vec3 end = groundPoint(arrow.level(), arrow, center,

                    Math.cos(angle + 0.32D) * Math.min(1.85D, radius + 0.25D),

                    Math.sin(angle + 0.32D) * Math.min(1.85D, radius + 0.25D));

            if (start == null || end == null

                    || start.distanceToSqr(center) > FIELD_VISUAL_RADIUS * FIELD_VISUAL_RADIUS

                    || end.distanceToSqr(center) > FIELD_VISUAL_RADIUS * FIELD_VISUAL_RADIUS) continue;

            if (!clearPath(arrow.level(), arrow, center, start)

                    || !clearPath(arrow.level(), arrow, center, end)

                    || !clearPath(arrow.level(), arrow, start, end)) continue;

            renderArc(stack, consumer, start.subtract(center), end.subtract(center),

                    random, 0.045D, 0.007D, 1.0F, false);

        }

    }

    private static Vec3 groundPoint(Level level, LunaShockArrowEntity arrow, Vec3 center,

                                    double x, double z) {

        BlockHitResult hit = level.clip(new ClipContext(center.add(x, 0.6D, z),

                center.add(x, -1.8D, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, arrow));

        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().add(0.0D, 0.045D, 0.0D) : null;

    }

    private static boolean clearPath(Level level, LunaShockArrowEntity arrow, Vec3 from, Vec3 to) {

        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,

                ClipContext.Fluid.NONE, arrow)).getType() == HitResult.Type.MISS;

    }

    private static Vec3 randomOffset(Random random, double radius) {

        Vec3 raw = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D,

                random.nextDouble() - 0.5D);

        return raw.lengthSqr() < 1.0E-8D ? Vec3.ZERO : raw.normalize().scale(radius);

    }

    private static void renderArc(PoseStack stack, VertexConsumer consumer, Vec3 start, Vec3 end,

                                  Random random, double jitter, double width, float intensity,

                                  boolean recon) {

        Vec3 previous = start;

        int outerAlpha = 35 + Math.round(55.0F * intensity);

        int innerAlpha = 90 + Math.round(165.0F * intensity);

        int outerR;

        int outerG;

        int outerB;

        int innerR;

        int innerG;

        int innerB;

        if (recon) {

            outerR = 190 + Math.round(65.0F * intensity);

            outerG = 25;

            outerB = 25;

            innerR = 235 + Math.round(20.0F * intensity);

            innerG = 60 + Math.round(95.0F * intensity);

            innerB = 55 + Math.round(85.0F * intensity);

        } else {

            outerR = 35;

            outerG = 105;

            outerB = 255;

            innerR = 72 + Math.round(110.0F * intensity);

            innerG = 138 + Math.round(105.0F * intensity);

            innerB = 255;

        }

        for (int step = 1; step <= 5; step++) {

            Vec3 next = start.lerp(end, step / 5.0D);

            if (step < 5) next = next.add(randomOffset(random, jitter));

            segment(stack, consumer, previous, next, width * 2.8D,

                    outerR, outerG, outerB, outerAlpha);

            segment(stack, consumer, previous, next, width,

                    Math.min(255, innerR), Math.min(255, innerG), Math.min(255, innerB), innerAlpha);

            previous = next;

        }

    }

    private static void segment(PoseStack stack, VertexConsumer consumer, Vec3 a, Vec3 b,

                                double width, int r, int g, int blue, int alpha) {

        Vec3 direction = b.subtract(a);

        if (direction.lengthSqr() < 1.0E-8D) return;

        direction = direction.normalize();

        Vec3 side = direction.cross(new Vec3(0, 1, 0));

        if (side.lengthSqr() < 1.0E-6D) side = direction.cross(new Vec3(1, 0, 0));

        side = side.normalize().scale(width);

        Vec3 other = direction.cross(side).normalize().scale(width);

        ribbon(stack, consumer, a, b, side, r, g, blue, alpha);

        ribbon(stack, consumer, a, b, other, r, g, blue, alpha);

    }

    private static void ribbon(PoseStack stack, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 side,

                               int r, int g, int blue, int alpha) {

        vertex(stack, consumer, a.add(side), r, g, blue, alpha);

        vertex(stack, consumer, a.subtract(side), r, g, blue, alpha);

        vertex(stack, consumer, b.subtract(side), r, g, blue, alpha);

        vertex(stack, consumer, b.add(side), r, g, blue, alpha);

    }

    private static void vertex(PoseStack stack, VertexConsumer consumer, Vec3 point,

                               int r, int g, int blue, int alpha) {

        consumer.vertex(stack.last().pose(), (float) point.x, (float) point.y, (float) point.z)

                .color(r, g, blue, alpha).endVertex();

    }

}

