package lol.silencecute.client.render.world;

import java.util.List;

import lol.silencecute.Client;
import lol.silencecute.client.TempstUltAnchorClient;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TempstUltAnchorRenderer {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("silencecute_skill");
    private static boolean probeLogged;

    private static final double CYLINDER_RADIUS = 0.05D;
    private static final double CYLINDER_HEIGHT = 0.5D;
    private static final int CYLINDER_SIDES = 8;

    private static final float ROPE_RED = 0.45F;
    private static final float ROPE_GREEN = 0.16F;
    private static final float ROPE_BLUE = 0.12F;
    private static final double ROPE_HALF_WIDTH = 0.035D;

    private TempstUltAnchorRenderer() {
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!TempstUltAnchorClient.isActive()) {
            probeLogged = false;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        if (!probeLogged) {
            probeLogged = true;
            LOGGER.info("[TempstUlt] anchor render active");
        }
        Vec3 anchorA = TempstUltAnchorClient.anchorA();
        Vec3 anchorB = TempstUltAnchorClient.anchorB();
        if (anchorA == null || anchorB == null) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        List<Vec3> ropeA = TempstUltAnchorClient.ropePath(player, 0);
        List<Vec3> ropeB = TempstUltAnchorClient.ropePath(player, 1);

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        drawCylinder(builder, matrix, anchorA);
        drawCylinder(builder, matrix, anchorB);

        drawRope(builder, matrix, camera, ropeA);
        drawRope(builder, matrix, camera, ropeB);

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void drawRope(BufferBuilder builder, Matrix4f matrix, Vec3 camera, List<Vec3> path) {
        if (path.size() < 2) return;
        for (int i = 0; i < path.size() - 1; i++) {
            segment(builder, matrix, camera, path.get(i), path.get(i + 1));
        }
        for (int i = 1; i < path.size() - 1; i++) {
            joint(builder, matrix, camera, path.get(i));
        }
    }

    private static void joint(BufferBuilder builder, Matrix4f matrix, Vec3 camera, Vec3 point) {
        Vec3 view = camera.subtract(point);
        double viewLength = view.length();
        if (viewLength < 1.0E-4D) return;
        view = view.scale(1.0D / viewLength);

        Vec3 right = view.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = view.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        right = right.normalize().scale(ROPE_HALF_WIDTH);
        Vec3 up = right.cross(view).normalize().scale(ROPE_HALF_WIDTH);

        Vec3 a = point.add(right).add(up);
        Vec3 b = point.subtract(right).add(up);
        Vec3 c = point.subtract(right).subtract(up);
        Vec3 d = point.add(right).subtract(up);
        vertex(builder, matrix, a, 1.0F);
        vertex(builder, matrix, b, 1.0F);
        vertex(builder, matrix, c, 1.0F);
        vertex(builder, matrix, d, 1.0F);
    }

    private static void segment(BufferBuilder builder, Matrix4f matrix, Vec3 camera, Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        double length = direction.length();
        if (length < 1.0E-4D) return;

        Vec3 dir = direction.scale(1.0D / length);
        Vec3 mid = from.add(to).scale(0.5D);
        Vec3 toCamera = camera.subtract(mid);
        double cameraLength = toCamera.length();
        if (cameraLength < 1.0E-4D) return;

        Vec3 side = dir.cross(toCamera.scale(1.0D / cameraLength));
        double sideLength = side.length();
        if (sideLength < 1.0E-4D) return;
        side = side.scale(ROPE_HALF_WIDTH / sideLength);

        Vec3 a = from.add(side);
        Vec3 b = from.subtract(side);
        Vec3 c = to.subtract(side);
        Vec3 d = to.add(side);

        vertex(builder, matrix, d, 1.0F);
        vertex(builder, matrix, c, 1.0F);
        vertex(builder, matrix, b, 1.0F);
        vertex(builder, matrix, a, 1.0F);
    }

    private static void drawCylinder(BufferBuilder builder, Matrix4f matrix, Vec3 base) {
        for (int i = 0; i < CYLINDER_SIDES; i++) {
            double a0 = Math.PI * 2.0D * i / CYLINDER_SIDES;
            double a1 = Math.PI * 2.0D * (i + 1) / CYLINDER_SIDES;

            Vec3 p0 = new Vec3(base.x + Math.cos(a0) * CYLINDER_RADIUS, base.y,
                    base.z + Math.sin(a0) * CYLINDER_RADIUS);
            Vec3 p1 = new Vec3(base.x + Math.cos(a1) * CYLINDER_RADIUS, base.y,
                    base.z + Math.sin(a1) * CYLINDER_RADIUS);
            Vec3 t0 = p0.add(0.0D, CYLINDER_HEIGHT, 0.0D);
            Vec3 t1 = p1.add(0.0D, CYLINDER_HEIGHT, 0.0D);

            vertexColor(builder, matrix, t1, 0.30F, 0.30F, 0.33F);
            vertexColor(builder, matrix, t0, 0.30F, 0.30F, 0.33F);
            vertexColor(builder, matrix, p0, 0.62F, 0.62F, 0.66F);
            vertexColor(builder, matrix, p1, 0.62F, 0.62F, 0.66F);
        }
    }

    private static void vertex(BufferBuilder builder, Matrix4f matrix, Vec3 pos, float alpha) {
        vertexColor(builder, matrix, pos, ROPE_RED, ROPE_GREEN, ROPE_BLUE);
    }

    private static void vertexColor(BufferBuilder builder, Matrix4f matrix, Vec3 pos,
                                    float red, float green, float blue) {
        builder.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(red, green, blue, 1.0F)
                .endVertex();
    }
}
