package lol.silencecute.client.render.kit;

import java.awt.Color;

import lol.silencecute.client.render.ui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;

public final class RoundedUtil {

    private RoundedUtil() {
    }

    public static void drawRound(float x, float y, float width, float height, float radius, Color color) {
        GuiGraphics g = GuiKit.graphics();
        if (g == null || width <= 0.0F || height <= 0.0F) {
            return;
        }
        UiDraw.roundRect(g, x, y, width, height, radius, argb(color));
    }

    public static void drawRound(float x, float y, float width, float height, float radius, float blur, Color color) {
        drawRound(x, y, width, height, radius, color);
    }

    public static void drawRound(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        drawRound(x, y, width, height, radius, color);
    }

    public static void drawRoundOutline(float x, float y, float width, float height, float radius,
                                        float outlineWidth, Color outlineColor, Color fillColor) {
        GuiGraphics g = GuiKit.graphics();
        if (g == null || width <= 0.0F || height <= 0.0F) {
            return;
        }
        if (fillColor != null && fillColor.getAlpha() > 0) {
            UiDraw.roundRect(g, x, y, width, height, radius, argb(fillColor));
        }
        if (outlineColor != null && outlineColor.getAlpha() > 0) {
            UiDraw.roundRectOutline(g, x, y, width, height, radius,
                    Math.max(1.0F, outlineWidth), argb(outlineColor));
        }
    }

    public static void drawGradientRound(float x, float y, float width, float height, float radius,
                                         Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        GuiGraphics g = GuiKit.graphics();
        if (g == null || width <= 0.0F || height <= 0.0F) {
            return;
        }
        UiDraw.roundRectGradient(g, x, y, width, height, radius, argb(topLeft), argb(bottomLeft), true);
    }

    public static void drawGradientRound(float x, float y, float width, float height, float radius,
                                         Color top, Color bottom) {
        drawGradientRound(x, y, width, height, radius, bottom, top, bottom, top);
    }

    public static void drawRoundTexture(net.minecraft.resources.ResourceLocation texture,
                                        float x, float y, float width, float height, float radius, Color color) {
        GuiGraphics g = GuiKit.graphics();
        if (g == null) {
            return;
        }
        UiDraw.image(g, texture, x, y, width, height, color == null ? 0xFFFFFFFF : argb(color));
    }

    public static int argb(Color color) {
        return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }
}
