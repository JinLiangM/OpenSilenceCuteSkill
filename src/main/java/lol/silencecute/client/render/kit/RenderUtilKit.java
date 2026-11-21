package lol.silencecute.client.render.kit;

import java.awt.Color;

import lol.silencecute.client.render.ui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class RenderUtilKit {

    private RenderUtilKit() {
    }

    public static void drawRectWH(float x, float y, float width, float height, Color color) {
        GuiGraphics g = GuiKit.graphics();
        if (g == null) {
            return;
        }
        UiDraw.rect(g, x, y, width, height, RoundedUtil.argb(color));
    }

    public static void drawRectWH(float x, float y, float width, float height, int color) {
        GuiGraphics g = GuiKit.graphics();
        if (g == null) {
            return;
        }
        UiDraw.rect(g, x, y, width, height, color);
    }

    public static void drawCircleCGUI(float centerX, float centerY, float radius, Color color) {
        GuiGraphics g = GuiKit.graphics();
        if (g == null) {
            return;
        }
        UiDraw.circle(g, centerX, centerY, radius, RoundedUtil.argb(color));
    }

    public static void drawCircleCGUI(float centerX, float centerY, float radius, int color) {
        GuiGraphics g = GuiKit.graphics();
        if (g == null) {
            return;
        }
        UiDraw.circle(g, centerX, centerY, radius, color);
    }

    public static void drawImage(ResourceLocation texture, float x, float y, float width, float height, Color color) {
        GuiGraphics g = GuiKit.graphics();
        if (g == null) {
            return;
        }
        UiDraw.image(g, texture, x, y, width, height, color == null ? 0xFFFFFFFF : RoundedUtil.argb(color));
    }

    public static boolean isHovering(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static Color color(int r, int g, int b, int a) {
        return new Color(r, g, b, a);
    }

    public static int reAlpha(int color, float alpha) {
        int a = (int) (alpha * 255.0F);
        a = Math.max(0, Math.min(255, a));
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
