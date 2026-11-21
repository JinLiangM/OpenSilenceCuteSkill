package lol.silencecute.client.render.world;

import lol.silencecute.Client;
import lol.silencecute.client.LunaGrenadeClient;
import lol.silencecute.util.LunaGrenadeBallistics;
import java.util.ArrayList;

import java.util.List;

import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.BufferBuilder;

import com.mojang.blaze3d.vertex.BufferUploader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.Tesselator;

import com.mojang.blaze3d.vertex.VertexConsumer;

import com.mojang.blaze3d.vertex.VertexFormat;

import lol.silencecute.config.ModConfigData;

import org.joml.Matrix4f;

import net.minecraft.client.Camera;

import net.minecraft.client.Minecraft;

import net.minecraft.client.player.LocalPlayer;

import net.minecraft.client.renderer.GameRenderer;

import net.minecraft.world.phys.EntityHitResult;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.level.ClipContext;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;

import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.HitResult;

import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.client.event.RenderLevelStageEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)

public final class LunaGrenadeTrajectoryRenderer {

    private static final int MAX_SIMULATION_TICKS = 240;

    private static final double MAX_RANGE_SQ = 130.0D * 130.0D;

    private static final double TUBE_RADIUS = 0.07D;

    private static final int TUBE_SIDES = 8;

    private static final int LINE_ALPHA_START = 1;

    private static final int LINE_ALPHA_END = 250;

    private static final double HEX_RADIUS_OUTER = 0.5D;

    private static final double HEX_RADIUS_INNER = 0.12D;

    private static final int HEX_FILL_OUTER_ALPHA = 0x60;

    private static final int HEX_FILL_INNER_ALPHA = 0xE0;

    private static final int HEX_EDGE_ALPHA = 0xF0;

    private static final float HEX_EDGE_HALF_WIDTH = 0.01F;

    private static final float HEX_EDGE_EXTEND = 0.008F;

    private static final double HEX_OFFSET = 0.03D;

    private LunaGrenadeTrajectoryRenderer() {

    }

    @SubscribeEvent

    public static void onRenderLevelStage(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {

            return;

        }

        if (!ModConfigData.SHOW_TRAJECTORY.get() && !ModConfigData.SHOW_HEXAGON.get()) {
        if (!ModConfigData.operatorRenderEnabled()) return;

            return;

        }

        Minecraft minecraft = Minecraft.getInstance();

        LocalPlayer player = minecraft.player;

        Level level = minecraft.level;

        if (player == null || level == null) return;

        if (!LunaGrenadeClient.isPrimed()) return;

        if (!minecraft.options.getCameraType().isFirstPerson()) return;

        float partial = event.getPartialTick();

        Vec3 origin = LunaGrenadeBallistics.origin(player, partial);

        Vec3 initialVelocity = LunaGrenadeBallistics.velocity(player, partial);

        TraceResult trace = simulate(level, player, origin, initialVelocity);

        if (trace.points().size() < 2) return;

        Camera camera = event.getCamera();

        Vec3 cameraPos = camera.getPosition();

        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();

        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        if (ModConfigData.SHOW_TRAJECTORY.get()) {

            drawTrajectoryTube(poseStack, trace.points());

        }

        if (ModConfigData.SHOW_HEXAGON.get()) {

            drawLandingHexagon(poseStack,

                    trace.points().get(trace.points().size() - 1),

                    initialVelocity, trace.normal());

        }

        poseStack.popPose();

    }

    private static TraceResult simulate(Level level, LocalPlayer owner,

                                        Vec3 origin, Vec3 initialVelocity) {

        List<Vec3> points = new ArrayList<>();

        points.add(origin);

        Vec3 pos = origin;

        Vec3 vel = initialVelocity;

        double gravity = LunaGrenadeBallistics.GRAVITY;

        double minY = level.getMinBuildHeight() - 8.0D;

        for (int i = 0; i < MAX_SIMULATION_TICKS; i++) {

            Vec3 next = pos.add(vel);

            BlockHitResult blockHit = level.clip(new ClipContext(pos, next,

                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));

            Vec3 blockLoc = blockHit.getType() != HitResult.Type.MISS

                    ? blockHit.getLocation() : null;

            double blockDistSq = blockLoc != null

                    ? blockLoc.distanceToSqr(pos) : Double.MAX_VALUE;

            EntityHitResult entityHit = findEntityOnPath(level, owner, pos, next);

            Vec3 entityLoc = entityHit != null ? entityHit.getLocation() : null;

            double entityDistSq = entityLoc != null

                    ? entityLoc.distanceToSqr(pos) : Double.MAX_VALUE;

            if (blockLoc != null && blockDistSq <= entityDistSq) {

                points.add(blockLoc);

                Vec3 normal = Vec3.atLowerCornerOf(blockHit.getDirection().getNormal());

                return new TraceResult(points, normal);

            }

            if (entityLoc != null) {

                points.add(entityLoc);

                Vec3 normal = new Vec3(-vel.x, 0.0D, -vel.z);

                if (normal.lengthSqr() < 1.0E-4D) {

                    normal = new Vec3(0.0D, 1.0D, 0.0D);

                } else {

                    normal = normal.normalize();

                }

                return new TraceResult(points, normal);

            }

            points.add(next);

            if (next.distanceToSqr(origin) > MAX_RANGE_SQ) {

                return new TraceResult(points, new Vec3(0.0D, 1.0D, 0.0D));

            }

            if (next.y < minY) {

                return new TraceResult(points, new Vec3(0.0D, 1.0D, 0.0D));

            }

            pos = next;

            vel = vel.scale(0.99);

            vel = new Vec3(vel.x, vel.y - gravity, vel.z);

        }

        return new TraceResult(points, new Vec3(0.0D, 1.0D, 0.0D));

    }

    private static EntityHitResult findEntityOnPath(Level level, LocalPlayer owner,

                                                    Vec3 from, Vec3 to) {

        AABB search = new AABB(from, to).inflate(0.8D);

        EntityHitResult closest = null;

        double closestDistSq = Double.MAX_VALUE;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search,

                e -> e != owner && e.isAlive())) {

            AABB box = candidate.getBoundingBox().inflate(0.1D);

            Optional<Vec3> hit = box.clip(from, to);

            if (hit.isPresent()) {

                double d = hit.get().distanceToSqr(from);

                if (d < closestDistSq) {

                    closestDistSq = d;

                    closest = new EntityHitResult(candidate, hit.get());

                }

            }

        }

        return closest;

    }

    private static void drawTrajectoryTube(PoseStack poseStack, List<Vec3> points) {

        int n = points.size();

        if (n < 2) return;

        int totalSegments = n - 1;

        Matrix4f matrix = poseStack.last().pose();

        Vec3[] tangents = new Vec3[n];

        for (int i = 0; i < n; i++) {

            Vec3 tangent;

            if (i == 0) {

                tangent = points.get(1).subtract(points.get(0));

            } else if (i == n - 1) {

                tangent = points.get(n - 1).subtract(points.get(n - 2));

            } else {

                tangent = points.get(i + 1).subtract(points.get(i - 1));

            }

            if (tangent.lengthSqr() < 1.0E-8D) {

                tangent = new Vec3(0.0D, 0.0D, 1.0D);

            }

            tangents[i] = tangent.normalize();

        }

        Vec3[][] rings = new Vec3[n][TUBE_SIDES];

        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);

        for (int i = 0; i < n; i++) {

            Vec3 t = tangents[i];

            Vec3 u = t.cross(worldUp);

            if (u.lengthSqr() < 1.0E-6D) {

                u = t.cross(new Vec3(1.0D, 0.0D, 0.0D));

            }

            u = u.normalize();

            Vec3 v = t.cross(u).normalize();

            Vec3 center = points.get(i);

            for (int k = 0; k < TUBE_SIDES; k++) {

                double angle = 2.0D * Math.PI * k / TUBE_SIDES;

                double cx = Math.cos(angle) * TUBE_RADIUS;

                double sx = Math.sin(angle) * TUBE_RADIUS;

                rings[i][k] = center.add(u.scale(cx)).add(v.scale(sx));

            }

        }

        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < totalSegments; i++) {

            float t0 = i / (float) totalSegments;

            float t1 = (i + 1) / (float) totalSegments;

            float s0 = t0 * t0 * t0;

            float s1 = t1 * t1 * t1;

            int a0 = lerpInt(LINE_ALPHA_START, LINE_ALPHA_END, s0);

            int a1 = lerpInt(LINE_ALPHA_START, LINE_ALPHA_END, s1);

            for (int k = 0; k < TUBE_SIDES; k++) {

                int k2 = (k + 1) % TUBE_SIDES;

                addVertex(builder, matrix, rings[i][k], a0);

                addVertex(builder, matrix, rings[i][k2], a0);

                addVertex(builder, matrix, rings[i + 1][k2], a1);

                addVertex(builder, matrix, rings[i + 1][k], a1);

            }

        }

        flush(builder);

    }

    private static void drawLandingHexagon(PoseStack poseStack, Vec3 landing,

                                           Vec3 initialVelocity, Vec3 normal) {

        if (normal.lengthSqr() < 1.0E-6D) {

            normal = new Vec3(0.0D, 1.0D, 0.0D);

        } else {

            normal = normal.normalize();

        }

        Vec3 hint = new Vec3(initialVelocity.x, 0.0D, initialVelocity.z);

        if (hint.lengthSqr() < 1.0E-6D) {

            hint = new Vec3(0.0D, 0.0D, 1.0D);

        }

        Vec3 u = hint.subtract(normal.scale(hint.dot(normal)));

        if (u.lengthSqr() < 1.0E-6D) {

            Vec3 any = Math.abs(normal.y) > 0.9D

                    ? new Vec3(1.0D, 0.0D, 0.0D)

                    : new Vec3(0.0D, 1.0D, 0.0D);

            u = any.subtract(normal.scale(any.dot(normal)));

        }

        if (u.lengthSqr() < 1.0E-8D) {

            u = new Vec3(1.0D, 0.0D, 0.0D);

        }

        u = u.normalize();

        Vec3 v = normal.cross(u);

        if (v.lengthSqr() < 1.0E-8D) {

            v = new Vec3(0.0D, 0.0D, 1.0D);

        }

        v = v.normalize();

        Vec3 center = landing.add(normal.scale(HEX_OFFSET));

        Matrix4f matrix = poseStack.last().pose();

        BufferBuilder fillOuter = Tesselator.getInstance().getBuilder();

        fillOuter.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        emitHexFill(fillOuter, matrix, center, u, v, HEX_RADIUS_OUTER,

                HEX_FILL_OUTER_ALPHA);

        flush(fillOuter);

        BufferBuilder edges = Tesselator.getInstance().getBuilder();

        edges.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        emitHexOutline(edges, matrix, center, u, v, normal, HEX_RADIUS_OUTER,

                HEX_EDGE_HALF_WIDTH, HEX_EDGE_EXTEND, HEX_EDGE_ALPHA);

        flush(edges);

        BufferBuilder fillInner = Tesselator.getInstance().getBuilder();

        fillInner.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        emitHexFill(fillInner, matrix, center, u, v, HEX_RADIUS_INNER,

                HEX_FILL_INNER_ALPHA);

        flush(fillInner);

    }

    private static void emitHexFill(VertexConsumer consumer, Matrix4f matrix,

                                    Vec3 center, Vec3 u, Vec3 v, double radius, int alpha) {

        Vec3[] corners = hexCorners(center, u, v, radius);

        for (int i = 0; i < 6; i++) {

            addVertex(consumer, matrix, center, alpha);

            addVertex(consumer, matrix, corners[i], alpha);

            addVertex(consumer, matrix, corners[(i + 1) % 6], alpha);

        }

    }

    private static void emitHexOutline(VertexConsumer consumer, Matrix4f matrix,

                                       Vec3 center, Vec3 u, Vec3 v, Vec3 normal, double radius,

                                       float halfWidth, float extend, int alpha) {

        Vec3[] corners = hexCorners(center, u, v, radius);

        for (int i = 0; i < 6; i++) {

            Vec3 a = corners[i];

            Vec3 b = corners[(i + 1) % 6];

            Vec3 dir = b.subtract(a);

            double len = dir.length();

            if (len < 1.0E-4D) continue;

            dir = dir.scale(1.0D / len);

            Vec3 inPlaneNormal = normal.cross(dir);

            if (inPlaneNormal.lengthSqr() < 1.0E-8D) continue;

            inPlaneNormal = inPlaneNormal.normalize().scale(halfWidth);

            Vec3 aExt = a.subtract(dir.scale(extend));

            Vec3 bExt = b.add(dir.scale(extend));

            Vec3 a0 = aExt.add(inPlaneNormal);

            Vec3 a1 = aExt.subtract(inPlaneNormal);

            Vec3 b0 = bExt.add(inPlaneNormal);

            Vec3 b1 = bExt.subtract(inPlaneNormal);

            addVertex(consumer, matrix, a0, alpha);

            addVertex(consumer, matrix, a1, alpha);

            addVertex(consumer, matrix, b1, alpha);

            addVertex(consumer, matrix, b0, alpha);

        }

    }

    private static Vec3[] hexCorners(Vec3 center, Vec3 u, Vec3 v, double radius) {

        Vec3[] corners = new Vec3[6];

        for (int i = 0; i < 6; i++) {

            double angle = i * (Math.PI / 3.0D);

            corners[i] = center

                    .add(u.scale(Math.cos(angle) * radius))

                    .add(v.scale(Math.sin(angle) * radius));

        }

        return corners;

    }

    private static void flush(BufferBuilder builder) {

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        RenderSystem.enableBlend();

        RenderSystem.defaultBlendFunc();

        RenderSystem.disableCull();

        RenderSystem.depthMask(false);

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);

        RenderSystem.enableCull();

        RenderSystem.disableBlend();

    }

    private static void addVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 pos, int alpha) {

        consumer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)

                .color(255, 255, 255, alpha)

                .endVertex();

    }

    private static int lerpInt(int a, int b, float t) {

        return Math.round(a + (b - a) * t);

    }

    private record TraceResult(List<Vec3> points, Vec3 normal) {

    }

}

