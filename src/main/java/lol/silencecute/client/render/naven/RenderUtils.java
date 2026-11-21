package lol.silencecute.client.render.naven;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class RenderUtils implements Wrapper {
    private static final AABB DEFAULT_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public static int reAlpha(int color, float alpha) {
        int col = MathUtils.clamp((int) (alpha * 255.0F), 0, 255) << 24;
        col |= MathUtils.clamp(color >> 16 & 0xFF, 0, 255) << 16;
        col |= MathUtils.clamp(color >> 8 & 0xFF, 0, 255) << 8;
        return col | MathUtils.clamp(color & 0xFF, 0, 255);
    }

    public static void drawTracer(PoseStack poseStack, float x, float y, float size, float widthDiv, float heightDiv, int color) {
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glEnable(2848);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = poseStack.last().pose();
        float a = (float) (color >> 24 & 0xFF) / 255.0F;
        float r = (float) (color >> 16 & 0xFF) / 255.0F;
        float g = (float) (color >> 8 & 0xFF) / 255.0F;
        float b = (float) (color & 0xFF) / 255.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x - size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x, y + size / heightDiv, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x + size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(3042);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(2848);
    }

    public static int getRainbowOpaque(int index, float saturation, float brightness, float speed) {
        float hue = (float) ((System.currentTimeMillis() + (long) index) % (long) ((int) speed)) / speed;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static BlockPos getCameraBlockPos() {
        Camera camera = mc.getBlockEntityRenderDispatcher().camera;
        return camera.getBlockPosition();
    }

    public static Vec3 getCameraPos() {
        Camera camera = mc.getBlockEntityRenderDispatcher().camera;
        return camera.getPosition();
    }

    public static RegionPos getCameraRegion() {
        return RegionPos.of(getCameraBlockPos());
    }

    public static void applyRegionalRenderOffset(PoseStack matrixStack) {
        applyRegionalRenderOffset(matrixStack, getCameraRegion());
    }

    public static void applyRegionalRenderOffset(PoseStack matrixStack, RegionPos region) {
        Vec3 offset = region.toVec3().subtract(getCameraPos());
        matrixStack.translate(offset.x, offset.y, offset.z);
    }

    public static void fill(PoseStack pPoseStack, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
        innerFill(pPoseStack.last().pose(), pMinX, pMinY, pMaxX, pMaxY, pColor);
    }

    public static void fillGradientH(PoseStack poseStack, float left, float top, float width, float height, int leftColor, int rightColor) {
        float right = left + width;
        float bottom = top + height;

        Matrix4f matrix = poseStack.last().pose();
        float la = (float) (leftColor >> 24 & 0xFF) / 255.0F;
        float lr = (float) (leftColor >> 16 & 0xFF) / 255.0F;
        float lg = (float) (leftColor >> 8 & 0xFF) / 255.0F;
        float lb = (float) (leftColor & 0xFF) / 255.0F;

        float ra = (float) (rightColor >> 24 & 0xFF) / 255.0F;
        float rr = (float) (rightColor >> 16 & 0xFF) / 255.0F;
        float rg = (float) (rightColor >> 8 & 0xFF) / 255.0F;
        float rb = (float) (rightColor & 0xFF) / 255.0F;

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, left, bottom, 0.0F).color(lr, lg, lb, la).endVertex();
        buffer.vertex(matrix, right, bottom, 0.0F).color(rr, rg, rb, ra).endVertex();
        buffer.vertex(matrix, right, top, 0.0F).color(rr, rg, rb, ra).endVertex();
        buffer.vertex(matrix, left, top, 0.0F).color(lr, lg, lb, la).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void innerFill(Matrix4f pMatrix, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
        if (pMinX < pMaxX) {
            float i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        if (pMinY < pMaxY) {
            float j = pMinY;
            pMinY = pMaxY;
            pMaxY = j;
        }

        float f3 = (float) (pColor >> 24 & 0xFF) / 255.0F;
        float f = (float) (pColor >> 16 & 0xFF) / 255.0F;
        float f1 = (float) (pColor >> 8 & 0xFF) / 255.0F;
        float f2 = (float) (pColor & 0xFF) / 255.0F;
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(pMatrix, pMinX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(pMatrix, pMaxX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(pMatrix, pMaxX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(pMatrix, pMinX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawRectBound(PoseStack poseStack, float x, float y, float width, float height, int color) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        float alpha = (float) (color >> 24 & 0xFF) / 255.0F;
        float red = (float) (color >> 16 & 0xFF) / 255.0F;
        float green = (float) (color >> 8 & 0xFF) / 255.0F;
        float blue = (float) (color & 0xFF) / 255.0F;
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
        tesselator.end();
    }

    private static void color(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
        float alpha = (float) (color >> 24 & 0xFF) / 255.0F;
        float red = (float) (color >> 16 & 0xFF) / 255.0F;
        float green = (float) (color >> 8 & 0xFF) / 255.0F;
        float blue = (float) (color & 0xFF) / 255.0F;
        buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    public static void drawRoundedRect(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, int color) {
        if (width == 0.0F || height == 0.0F) {
            return;
        }

        if (width < 0.0F) {
            x += width;
            width = -width;
        }
        if (height < 0.0F) {
            y += height;
            height = -height;
        }

        if (color == 16777215) {
            color = ARGB32.color(255, 255, 255, 255);
        }

        float maxRadius = Math.min(width * 0.5F, height * 0.5F);
        if (edgeRadius > maxRadius) {
            edgeRadius = maxRadius;
        }
        if (edgeRadius < 0.0F) {
            edgeRadius = 0.0F;
        }

        if (edgeRadius <= 0.0F) {
            fill(poseStack, x, y, x + width, y + height, color);
            return;
        }

        float alpha = (float) (color >> 24 & 0xFF) / 255.0F;
        float red = (float) (color >> 16 & 0xFF) / 255.0F;
        float green = (float) (color >> 8 & 0xFF) / 255.0F;
        float blue = (float) (color & 0xFF) / 255.0F;

        boolean cullWasEnabled = false;
        boolean lineSmoothWasEnabled = false;
        try {
            try {
                cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
                lineSmoothWasEnabled = GL11.glIsEnabled(GL11.GL_LINE_SMOOTH);
            } catch (Throwable ignored) {
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            renderRoundedQuadInternal(
                    poseStack.last().pose(),
                    red,
                    green,
                    blue,
                    alpha,
                    x,
                    y,
                    x + width,
                    y + height,
                    edgeRadius,
                    45
            );
        } finally {
            if (!lineSmoothWasEnabled) {
                try {
                    GL11.glDisable(GL11.GL_LINE_SMOOTH);
                } catch (Throwable ignored) {
                }
            }
            if (cullWasEnabled) {
                try {
                    RenderSystem.enableCull();
                } catch (Throwable ignored) {
                }
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    private static void renderRoundedQuadInternal(
            Matrix4f matrix,
            float red,
            float green,
            float blue,
            float alpha,
            float fromX,
            float fromY,
            float toX,
            float toY,
            float radius,
            int samples
    ) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        float[][] map = new float[][]{
                {toX - radius, toY - radius, radius},
                {toX - radius, fromY + radius, radius},
                {fromX + radius, fromY + radius, radius},
                {fromX + radius, toY - radius, radius}
        };

        for (int i = 0; i < 4; i++) {
            float[] current = map[i];
            float rad = current[2];

            float startDeg = i * 90.0F;
            float endDeg = startDeg + 90.0F;
            float step = 90.0F / (float) Math.max(1, samples);
            for (float deg = startDeg; deg < endDeg; deg += step) {
                float rad1 = (float) Math.toRadians(deg);
                float sin = (float) (Math.sin(rad1) * (double) rad);
                float cos = (float) (Math.cos(rad1) * (double) rad);
                bufferBuilder.vertex(matrix, current[0] + sin, current[1] + cos, 0.0F).color(red, green, blue, alpha).endVertex();
            }

            float finalRad = (float) Math.toRadians(endDeg);
            float finalSin = (float) (Math.sin(finalRad) * (double) rad);
            float finalCos = (float) (Math.cos(finalRad) * (double) rad);
            bufferBuilder.vertex(matrix, current[0] + finalSin, current[1] + finalCos, 0.0F).color(red, green, blue, alpha).endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void fillGradient(PoseStack poseStack, float x, float y, float width, float height,
                                    int topLeft, int topRight, int bottomRight, int bottomLeft) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        if (((topLeft >>> 24) & 0xFF) == 0 && ((topRight >>> 24) & 0xFF) == 0
                && ((bottomRight >>> 24) & 0xFF) == 0 && ((bottomLeft >>> 24) & 0xFF) == 0) {
            return;
        }

        Matrix4f matrix = poseStack.last().pose();
        float right = x + width;
        float bottom = y + height;

        boolean cullWasEnabled = isCullFaceEnabled();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            color(buffer, matrix, x, y, topLeft);
            color(buffer, matrix, right, y, topRight);
            color(buffer, matrix, right, bottom, bottomRight);
            color(buffer, matrix, x, bottom, bottomLeft);
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            restoreCullFace(cullWasEnabled);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    public static void drawRoundedOutline(PoseStack poseStack, float x, float y, float width, float height,
                                          float edgeRadius, float lineWidth, int color) {
        if (width <= 0.0F || height <= 0.0F || lineWidth <= 0.0F) {
            return;
        }
        if (((color >>> 24) & 0xFF) == 0) {
            return;
        }

        float outerRadius = Math.max(0.0F, Math.min(edgeRadius, Math.min(width, height) * 0.5F));
        float innerWidth = width - lineWidth * 2.0F;
        float innerHeight = height - lineWidth * 2.0F;
        if (innerWidth <= 0.0F || innerHeight <= 0.0F) {
            fill(poseStack, x, y, x + width, y + height, color);
            return;
        }
        float innerRadius = Math.max(0.0F, outerRadius - lineWidth);
        int segments = Math.max(2, (int) Math.ceil(Math.max(1.0F, outerRadius) * 2.0F));

        float[][] outer = roundedPath(x, y, width, height, outerRadius, segments);
        float[][] inner = roundedPath(x + lineWidth, y + lineWidth, innerWidth, innerHeight, innerRadius, segments);

        Matrix4f matrix = poseStack.last().pose();
        boolean cullWasEnabled = isCullFaceEnabled();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < outer.length; i++) {
                int next = (i + 1) % outer.length;
                color(buffer, matrix, outer[i][0], outer[i][1], color);
                color(buffer, matrix, outer[next][0], outer[next][1], color);
                color(buffer, matrix, inner[next][0], inner[next][1], color);
                color(buffer, matrix, outer[i][0], outer[i][1], color);
                color(buffer, matrix, inner[next][0], inner[next][1], color);
                color(buffer, matrix, inner[i][0], inner[i][1], color);
            }
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            restoreCullFace(cullWasEnabled);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    private static float[][] roundedPath(float x, float y, float width, float height, float radius, int segments) {
        float rad = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
        float[][] corners = {
                {x + width - rad, y + rad, -90.0F},
                {x + width - rad, y + height - rad, 0.0F},
                {x + rad, y + height - rad, 90.0F},
                {x + rad, y + rad, 180.0F}
        };
        float[][] points = new float[4 * (segments + 1)][2];
        int index = 0;
        for (float[] corner : corners) {
            for (int s = 0; s <= segments; s++) {
                double angle = Math.toRadians(corner[2] + 90.0F * s / segments);
                points[index][0] = (float) (corner[0] + Math.cos(angle) * rad);
                points[index][1] = (float) (corner[1] + Math.sin(angle) * rad);
                index++;
            }
        }
        return points;
    }

    private static boolean isCullFaceEnabled() {
        try {
            return GL11.glIsEnabled(GL11.GL_CULL_FACE);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void restoreCullFace(boolean enabled) {
        if (!enabled) {
            return;
        }
        try {
            RenderSystem.enableCull();
        } catch (Throwable ignored) {
        }
    }

    public static void drawSolidBox(PoseStack matrixStack) {
        drawSolidBox(DEFAULT_BOX, matrixStack);
    }

    public static void drawSolidBox(AABB bb, PoseStack matrixStack) {
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void drawOutlinedBox(PoseStack matrixStack) {
        drawOutlinedBox(DEFAULT_BOX, matrixStack);
    }

    public static void drawOutlinedBox(AABB bb, PoseStack matrixStack) {
        Matrix4f matrix = matrixStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void drawSolidBox(AABB bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
        drawSolidBox(bb, bufferBuilder);
        BufferUploader.reset();
        vertexBuffer.bind();
        RenderedBuffer buffer = bufferBuilder.end();
        vertexBuffer.upload(buffer);
        VertexBuffer.unbind();
    }

    public static void drawSolidBox(AABB bb, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
    }

    public static void drawOutlinedBox(AABB bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
        drawOutlinedBox(bb, bufferBuilder);
        vertexBuffer.upload(bufferBuilder.end());
    }

    public static void drawOutlinedBox(AABB bb, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
    }

    public static boolean isHovering(int mouseX, int mouseY, float xLeft, float yUp, float xRight, float yBottom) {
        return (float) mouseX > xLeft && (float) mouseX < xRight && (float) mouseY > yUp && (float) mouseY < yBottom;
    }

    public static boolean isHoveringBound(int mouseX, int mouseY, float xLeft, float yUp, float width, float height) {
        return (float) mouseX > xLeft && (float) mouseX < xLeft + width && (float) mouseY > yUp && (float) mouseY < yUp + height;
    }

    public static void fillBound(PoseStack stack, float left, float top, float width, float height, int color) {
        float right = left + width;
        float bottom = top + height;
        fill(stack, left, top, right, bottom, color);
    }

    public static void drawPlayerHead(PoseStack poseStack, float x, float y, float width, float height, AbstractClientPlayer player) {
        drawPlayerHead(poseStack, x, y, width, height, player, 1.0F);
    }

    public static void drawPlayerHead(PoseStack poseStack, float x, float y, float width, float height, AbstractClientPlayer player, float alpha) {
        if (mc.player == null || mc.level == null) return;

        ResourceLocation skin = mc.player.getSkinTextureLocation();

        try {
            skin = player.getSkinTextureLocation();
        } catch (Exception e) {

        }

        int hurtTime = player.hurtTime;
        float redTint = 1.0f;

        if (hurtTime > 0) {
            float progress = (float) hurtTime / 10.0f;
            progress = Math.min(progress, 1.0f);
            redTint = lerp(progress, 0.6f, 1.0f);
        }

        float clampedAlpha = MathUtils.clamp(alpha, 0.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, skin);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, redTint, redTint, clampedAlpha);

        blit(poseStack, x, y, width, height, 8, 8, 8, 8, 64, 64);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    public static void drawRoundedPlayerHead(PoseStack poseStack, float x, float y, float width, float height, float radius, AbstractClientPlayer player, float alpha) {
        StencilUtils.write(false);
        if (!StencilUtils.isAvailable()) {

            StencilUtils.dispose();

            if (mc.player == null || mc.level == null) return;

            ResourceLocation skin = mc.player.getSkinTextureLocation();
            try {
                skin = player.getSkinTextureLocation();
            } catch (Exception ignored) {
            }

            int hurtTime = player.hurtTime;
            float redTint = 1.0f;
            if (hurtTime > 0) {
                float progress = (float) hurtTime / 10.0f;
                progress = Math.min(progress, 1.0f);
                redTint = lerp(progress, 0.6f, 1.0f);
            }

            float clampedAlpha = MathUtils.clamp(alpha, 0.0F, 1.0F);
            RoundedTextureRenderer.drawRoundedSubTex(
                    x,
                    y,
                    width,
                    height,
                    radius,
                    skin,
                    8.0F / 64.0F,
                    8.0F / 64.0F,
                    16.0F / 64.0F,
                    16.0F / 64.0F,
                    1.0F,
                    redTint,
                    redTint,
                    clampedAlpha
            );
            return;
        }

        drawRoundedRect(poseStack, x, y, width, height, radius, Integer.MIN_VALUE);
        StencilUtils.erase(true);
        drawPlayerHead(poseStack, x, y, width, height, player, alpha);
        StencilUtils.dispose();
    }

    private static void blit(PoseStack poseStack, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
        float u0 = (float) uOffset / (float) textureWidth;
        float u1 = (float) (uOffset + uWidth) / (float) textureWidth;
        float v0 = (float) vOffset / (float) textureHeight;
        float v1 = (float) (vOffset + vHeight) / (float) textureHeight;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, x, y + height, 0).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrix, x + width, y + height, 0).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix, x + width, y, 0).uv(u1, v0).endVertex();
        bufferBuilder.vertex(matrix, x, y, 0).uv(u0, v0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static float lerp(float progress, float start, float end) {
        return start + (end - start) * (1.0f - progress);
    }

    public static void 装女人(BufferBuilder bufferBuilder, Matrix4f matrix, AABB box) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getPosition().x());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getPosition().y());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getPosition().x());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getPosition().y());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void renderGuiItem(PoseStack poseStack, ItemStack itemStack, float x, float y, BakedModel model) {
        mc.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.pushPose();

        poseStack.translate(x, y, 100.0F);
        poseStack.translate(8.0, 8.0, 0.0);
        poseStack.scale(1.0F, -1.0F, 1.0F);
        poseStack.scale(16.0F, 16.0F, 16.0F);
        RenderSystem.applyModelViewMatrix();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        boolean flag = !model.usesBlockLight();
        if (flag) {
            Lighting.setupForFlatItems();
        }

        mc.getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY, model);
        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
        if (flag) {
            Lighting.setupFor3DItems();
        }

        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    public static void renderGuiItem(PoseStack poseStack, ItemStack itemStack, float x, float y) {
        renderGuiItem(poseStack, itemStack, x, y, mc.getItemRenderer().getModel(itemStack, null, null, 0));
    }

    public static void drawPlayerSolidBox(PoseStack poseStack, double x, double y, double z, int color) {
        drawEntitySolidBox(poseStack, x, y, z, 0.6F, 1.8F, color);
    }

    public static void drawEntitySolidBox(PoseStack poseStack, double x, double y, double z, float width, float height, int color) {
        Vec3 cameraPos = getCameraPos();

        float a = (float) (color >> 24 & 0xFF) / 255.0F;
        float r = (float) (color >> 16 & 0xFF) / 255.0F;
        float g = (float) (color >> 8 & 0xFF) / 255.0F;
        float b = (float) (color & 0xFF) / 255.0F;

        poseStack.pushPose();

        poseStack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);

        AABB box = new AABB(
                -width / 2.0, 0, -width / 2.0,
                width / 2.0, height, width / 2.0
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(GameRenderer::getPositionShader);

        RenderSystem.setShaderColor(r, g, b, a);

        drawSolidBox(box, poseStack);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        poseStack.popPose();
    }
}
