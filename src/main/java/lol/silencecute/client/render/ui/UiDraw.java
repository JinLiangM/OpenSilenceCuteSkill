package lol.silencecute.client.render.ui;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class UiDraw {

    private static final int ARC_SEGMENTS = 12;

    private static final int CIRCLE_SEGMENTS = 48;

    private static final Map<ResourceLocation, int[]> TEXTURE_SIZES = new HashMap<>();

    private UiDraw() {}

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int multiplyAlpha(int argb, float factor) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * Mth.clamp(factor, 0.0F, 1.0F));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    public static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0x00FFFFFF);
    }

    public static int mix(int c1, int c2, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int a = lerpComponent((c1 >>> 24) & 0xFF, (c2 >>> 24) & 0xFF, t);
        int r = lerpComponent((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, t);
        int g = lerpComponent((c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF, t);
        int b = lerpComponent(c1 & 0xFF, c2 & 0xFF, t);
        return rgba(r, g, b, a);
    }

    private static int lerpComponent(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }

    public static void rect(GuiGraphics graphics, float x, float y, float width, float height, int argb) {
        rectGradient(graphics, x, y, width, height, argb, argb, true);
    }

    public static void gradientHorizontal(GuiGraphics graphics, float x, float y, float width, float height,
                                          int left, int right) {
        rectGradient(graphics, x, y, width, height, left, right, true);
    }

    public static void gradientVertical(GuiGraphics graphics, float x, float y, float width, float height,
                                        int top, int bottom) {
        rectGradient(graphics, x, y, width, height, top, bottom, false);
    }

    private static void rectGradient(GuiGraphics graphics, float x, float y, float width, float height,
                                     int from, int to, boolean horizontal) {
        if (width <= 0.0F || height <= 0.0F) return;
        if (((from >>> 24) & 0xFF) == 0 && ((to >>> 24) & 0xFF) == 0) return;
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer vc = graphics.bufferSource().getBuffer(RenderType.gui());
        emitRect(vc, matrix, x, y, x + width, y + height, from, to, horizontal, x, y, width, height);
    }

    public static void line(GuiGraphics graphics, float x1, float y1, float x2, float y2,
                            float thickness, int argb) {
        if (y1 == y2) {
            rect(graphics, Math.min(x1, x2), y1, Math.abs(x2 - x1), thickness, argb);
        } else if (x1 == x2) {
            rect(graphics, x1, Math.min(y1, y2), thickness, Math.abs(y2 - y1), argb);
        }
    }

    public static void roundRect(GuiGraphics graphics, float x, float y, float width, float height,
                             float radius, int argb) {
        if (width <= 0.0F || height <= 0.0F) return;
        if (((argb >>> 24) & 0xFF) == 0) return;

        int x0 = Math.round(x);
        int y0 = Math.round(y);
        int x1 = Math.round(x + width);
        int y1 = Math.round(y + height);
        if (x1 <= x0 || y1 <= y0) return;

        int r = Math.round(Mth.clamp(radius, 0.0F, Math.min(width, height) / 2.0F));
        if (r <= 0) {
            graphics.fill(x0, y0, x1, y1, argb);
            return;
        }

        graphics.fill(x0, y0 + r, x1, y1 - r, argb);

        for (int i = 0; i < r; i++) {
            float dy = r - i - 0.5F;
            float inset = r - (float) Math.sqrt(Math.max(0.0F, (float) r * r - dy * dy));
            int ix0 = x0 + Math.round(inset);
            int ix1 = x1 - Math.round(inset);
            if (ix1 <= ix0) continue;
            graphics.fill(ix0, y0 + i, ix1, y0 + i + 1, argb);
            graphics.fill(ix0, y1 - i - 1, ix1, y1 - i, argb);
        }
    }

    public static void roundRectGradient(GuiGraphics graphics, float x, float y, float width, float height,
                                         float radius, int from, int to, boolean horizontal) {
        if (width <= 0.0F || height <= 0.0F) return;
        if (((from >>> 24) & 0xFF) == 0 && ((to >>> 24) & 0xFF) == 0) return;
        float r = Mth.clamp(radius, 0.0F, Math.min(width, height) / 2.0F);
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer vc = graphics.bufferSource().getBuffer(RenderType.gui());
        emitRoundRect(vc, matrix, x, y, width, height, r, from, to, horizontal,
                x, y, width, height);
    }

    public static void roundRectOutline(GuiGraphics graphics, float x, float y, float width, float height,
                                        float radius, float thickness, int argb) {
        if (width <= 0.0F || height <= 0.0F || thickness <= 0.0F) return;
        if (((argb >>> 24) & 0xFF) == 0) return;
        float r = Mth.clamp(radius, 0.0F, Math.min(width, height) / 2.0F);
        float t = Math.min(thickness, Math.max(r, 0.0001F));
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer vc = graphics.bufferSource().getBuffer(RenderType.gui());
        emitRoundRectOutline(vc, matrix, x, y, width, height, r, t, argb);
    }

    public static void roundRectWithBorder(GuiGraphics graphics, float x, float y, float width, float height,
                                           float radius, float thickness, int border, int fill) {
        roundRect(graphics, x, y, width, height, radius, fill);
        roundRectOutline(graphics, x, y, width, height, radius, thickness, border);
    }

    public static void circle(GuiGraphics graphics, float centerX, float centerY, float radius, int argb) {
        if (radius <= 0.0F) return;
        if (((argb >>> 24) & 0xFF) == 0) return;

        int cx = Math.round(centerX);
        int cy = Math.round(centerY);
        int r = Math.round(radius);
        for (int dy = -r; dy < r; dy++) {
            float span = (float) Math.sqrt(Math.max(0.0F, (float) r * r - (float) dy * dy));
            int dx = Math.round(span);
            if (dx <= 0) continue;
            graphics.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, argb);
        }
    }

    public static void ring(GuiGraphics graphics, float centerX, float centerY, float radius,
                            float thickness, int argb) {
        if (radius <= 0.0F || thickness <= 0.0F) return;
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer vc = graphics.bufferSource().getBuffer(RenderType.gui());
        float inner = Math.max(0.0F, radius - thickness);
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double a1 = Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
            double a2 = Math.PI * 2.0 * (i + 1) / CIRCLE_SEGMENTS;
            float ox1 = centerX + (float) Math.cos(a1) * radius;
            float oy1 = centerY + (float) Math.sin(a1) * radius;
            float ox2 = centerX + (float) Math.cos(a2) * radius;
            float oy2 = centerY + (float) Math.sin(a2) * radius;
            float ix1 = centerX + (float) Math.cos(a1) * inner;
            float iy1 = centerY + (float) Math.sin(a1) * inner;
            float ix2 = centerX + (float) Math.cos(a2) * inner;
            float iy2 = centerY + (float) Math.sin(a2) * inner;
            emitQuad(vc, matrix, ox1, oy1, ox2, oy2, ix2, iy2, ix1, iy1,
                    argb, argb, argb, argb);
        }
    }

    public static void image(GuiGraphics graphics, ResourceLocation texture, float x, float y,
                             float width, float height, int argb) {
        int[] size = textureSize(texture);
        if (size == null || width <= 0.0F || height <= 0.0F) return;
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        if (alpha <= 0.0F) return;

        graphics.flush();
        RenderSystem.setShaderColor(((argb >> 16) & 0xFF) / 255.0F,
                ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F,
                alpha);
        graphics.blit(texture, (int) x, (int) y, (int) width, (int) height,
                0.0F, 0.0F, size[0], size[1], size[0], size[1]);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int[] textureSize(ResourceLocation location) {
        int[] cached = TEXTURE_SIZES.get(location);
        if (cached != null) return cached.length == 0 ? null : cached;
        try (InputStream stream = Minecraft.getInstance().getResourceManager()
                .getResource(location).orElseThrow().open()) {
            byte[] header = stream.readNBytes(24);
            if (header.length < 24 || header[0] != (byte) 0x89 || header[1] != 'P') {
                TEXTURE_SIZES.put(location, new int[0]);
                return null;
            }
            int[] size = new int[]{readInt(header, 16), readInt(header, 20)};
            TEXTURE_SIZES.put(location, size);
            return size;
        } catch (Exception e) {
            TEXTURE_SIZES.put(location, new int[0]);
            return null;
        }
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
    }

    public static boolean isHovering(double mouseX, double mouseY,
                                     double x, double y, double width, double height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static boolean isHoveringCircle(double mouseX, double mouseY,
                                           float centerX, float centerY, float radius) {
        float dx = (float) mouseX - centerX;
        float dy = (float) mouseY - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    private static void emitRect(VertexConsumer vc, Matrix4f matrix,
                                 float x1, float y1, float x2, float y2,
                                 int from, int to, boolean horizontal,
                                 float boxX, float boxY, float boxW, float boxH) {
        int cTL = gradientColorAt(from, to, horizontal, boxX, boxY, boxW, boxH, x1, y1);
        int cTR = gradientColorAt(from, to, horizontal, boxX, boxY, boxW, boxH, x2, y1);
        int cBR = gradientColorAt(from, to, horizontal, boxX, boxY, boxW, boxH, x2, y2);
        int cBL = gradientColorAt(from, to, horizontal, boxX, boxY, boxW, boxH, x1, y2);

        vc.vertex(matrix, x1, y2, 0.0F).color(cBL).endVertex();
        vc.vertex(matrix, x2, y2, 0.0F).color(cBR).endVertex();
        vc.vertex(matrix, x2, y1, 0.0F).color(cTR).endVertex();
        vc.vertex(matrix, x1, y1, 0.0F).color(cTL).endVertex();
    }

    private static void emitTriangle(VertexConsumer vc, Matrix4f matrix,
                                     float ax, float ay, float bx, float by, float cx, float cy,
                                     int ca, int cb, int cc) {
        vc.vertex(matrix, ax, ay, 0.0F).color(ca).endVertex();
        vc.vertex(matrix, bx, by, 0.0F).color(cb).endVertex();
        vc.vertex(matrix, cx, cy, 0.0F).color(cc).endVertex();
        vc.vertex(matrix, cx, cy, 0.0F).color(cc).endVertex();
    }

    private static void emitQuad(VertexConsumer vc, Matrix4f matrix,
                                 float ax, float ay, float bx, float by,
                                 float cx, float cy, float dx, float dy,
                                 int ca, int cb, int cc, int cd) {
        vc.vertex(matrix, ax, ay, 0.0F).color(ca).endVertex();
        vc.vertex(matrix, bx, by, 0.0F).color(cb).endVertex();
        vc.vertex(matrix, cx, cy, 0.0F).color(cc).endVertex();
        vc.vertex(matrix, dx, dy, 0.0F).color(cd).endVertex();
    }

    private static void emitRoundRect(VertexConsumer vc, Matrix4f matrix,
                                      float x, float y, float width, float height, float radius,
                                      int from, int to, boolean horizontal,
                                      float boxX, float boxY, float boxW, float boxH) {
        if (radius <= 0.01F) {
            emitRect(vc, matrix, x, y, x + width, y + height, from, to, horizontal, boxX, boxY, boxW, boxH);
            return;
        }

        emitRect(vc, matrix, x + radius, y, x + width - radius, y + height,
                from, to, horizontal, boxX, boxY, boxW, boxH);

        emitRect(vc, matrix, x, y + radius, x + radius, y + height - radius,
                from, to, horizontal, boxX, boxY, boxW, boxH);
        emitRect(vc, matrix, x + width - radius, y + radius, x + width, y + height - radius,
                from, to, horizontal, boxX, boxY, boxW, boxH);

        emitCornerFan(vc, matrix, x + radius, y + radius, radius, 180.0F,
                from, to, horizontal, boxX, boxY, boxW, boxH);
        emitCornerFan(vc, matrix, x + width - radius, y + radius, radius, 270.0F,
                from, to, horizontal, boxX, boxY, boxW, boxH);
        emitCornerFan(vc, matrix, x + width - radius, y + height - radius, radius, 0.0F,
                from, to, horizontal, boxX, boxY, boxW, boxH);
        emitCornerFan(vc, matrix, x + radius, y + height - radius, radius, 90.0F,
                from, to, horizontal, boxX, boxY, boxW, boxH);
    }

    private static void emitCornerFan(VertexConsumer vc, Matrix4f matrix,
                                      float centerX, float centerY, float radius, float startDeg,
                                      int from, int to, boolean horizontal,
                                      float boxX, float boxY, float boxW, float boxH) {
        int centerColor = gradientColorAt(from, to, horizontal, boxX, boxY, boxW, boxH, centerX, centerY);
        for (int s = 0; s < ARC_SEGMENTS; s++) {
            double a1 = Math.toRadians(startDeg + 90.0 * s / ARC_SEGMENTS);
            double a2 = Math.toRadians(startDeg + 90.0 * (s + 1) / ARC_SEGMENTS);
            float x1 = centerX + (float) Math.cos(a1) * radius;
            float y1 = centerY + (float) Math.sin(a1) * radius;
            float x2 = centerX + (float) Math.cos(a2) * radius;
            float y2 = centerY + (float) Math.sin(a2) * radius;
            emitTriangle(vc, matrix, centerX, centerY, x1, y1, x2, y2, centerColor,
                    gradientColorAt(from, to, horizontal, boxX, boxY, boxW, boxH, x1, y1),
                    gradientColorAt(from, to, horizontal, boxX, boxY, boxW, boxH, x2, y2));
        }
    }

    private static void emitRoundRectOutline(VertexConsumer vc, Matrix4f matrix,
                                             float x, float y, float width, float height,
                                             float radius, float thickness, int argb) {
        float innerRadius = Math.max(0.0F, radius - thickness);

        emitRect(vc, matrix, x + radius, y, x + width - radius, y + thickness,
                argb, argb, true, x, y, width, height);
        emitRect(vc, matrix, x + radius, y + height - thickness, x + width - radius, y + height,
                argb, argb, true, x, y, width, height);
        emitRect(vc, matrix, x, y + radius, x + thickness, y + height - radius,
                argb, argb, true, x, y, width, height);
        emitRect(vc, matrix, x + width - thickness, y + radius, x + width, y + height - radius,
                argb, argb, true, x, y, width, height);

        emitCornerRing(vc, matrix, x + radius, y + radius, radius, innerRadius, 180.0F, argb);
        emitCornerRing(vc, matrix, x + width - radius, y + radius, radius, innerRadius, 270.0F, argb);
        emitCornerRing(vc, matrix, x + width - radius, y + height - radius, radius, innerRadius, 0.0F, argb);
        emitCornerRing(vc, matrix, x + radius, y + height - radius, radius, innerRadius, 90.0F, argb);
    }

    private static void emitCornerRing(VertexConsumer vc, Matrix4f matrix,
                                       float centerX, float centerY,
                                       float radius, float innerRadius, float startDeg, int argb) {
        for (int s = 0; s < ARC_SEGMENTS; s++) {
            double a1 = Math.toRadians(startDeg + 90.0 * s / ARC_SEGMENTS);
            double a2 = Math.toRadians(startDeg + 90.0 * (s + 1) / ARC_SEGMENTS);
            float ox1 = centerX + (float) Math.cos(a1) * radius;
            float oy1 = centerY + (float) Math.sin(a1) * radius;
            float ox2 = centerX + (float) Math.cos(a2) * radius;
            float oy2 = centerY + (float) Math.sin(a2) * radius;
            float ix1 = centerX + (float) Math.cos(a1) * innerRadius;
            float iy1 = centerY + (float) Math.sin(a1) * innerRadius;
            float ix2 = centerX + (float) Math.cos(a2) * innerRadius;
            float iy2 = centerY + (float) Math.sin(a2) * innerRadius;
            emitQuad(vc, matrix, ox1, oy1, ox2, oy2, ix2, iy2, ix1, iy1,
                    argb, argb, argb, argb);
        }
    }

    private static int gradientColorAt(int from, int to, boolean horizontal,
                                       float boxX, float boxY, float boxW, float boxH,
                                       float px, float py) {
        if (from == to) return from;
        float t = horizontal
                ? (px - boxX) / Math.max(boxW, 0.0001F)
                : (py - boxY) / Math.max(boxH, 0.0001F);
        return mix(from, to, t);
    }

    public static void roundRectBorder(GuiGraphics graphics, float x, float y, float width, float height,
                                       float radius, float thickness, int borderArgb, int fillArgb) {
        float t = Math.max(1.0F, thickness);
        if (((borderArgb >>> 24) & 0xFF) != 0) {
            roundRect(graphics, x, y, width, height, radius, borderArgb);
        }
        roundRect(graphics, x + t, y + t, width - 2.0F * t, height - 2.0F * t,
                Math.max(0.0F, radius - t), fillArgb);
    }
}
