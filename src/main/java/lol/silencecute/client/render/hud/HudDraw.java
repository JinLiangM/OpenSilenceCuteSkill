package lol.silencecute.client.render.hud;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import lol.silencecute.client.font.ModFonts;

public final class HudDraw {

    private HudDraw() {}

    public static void drawSafeIcon(GuiGraphics graphics, ResourceLocation texture,
                                    int x, int y, int size) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        graphics.blit(texture, x, y, 0, 0, size, size, 16, 16);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public static void drawScaledString(GuiGraphics graphics, Font font, String text,
                                        float x, float y, float scale, int color) {
        drawScaledString(graphics, font, text, x, y, scale, color, false);
    }

    public static void drawScaledString(GuiGraphics graphics, Font font, String text,
                                        float x, float y, float scale, int color, boolean shadow) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        ModFonts.drawString(graphics, text, 0, 0, color, shadow);
        graphics.pose().popPose();
    }

    public static void pushWidget(GuiGraphics graphics, HudWidget widget) {
        graphics.pose().pushPose();
        graphics.pose().translate(widget.x(), widget.y(), 0.0F);
        graphics.pose().scale(widget.scaleX(), widget.scaleY(), 1.0F);
    }

    public static void popWidget(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    public static double mouseX(Minecraft minecraft) {
        return minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getScreenWidth();
    }

    public static double mouseY(Minecraft minecraft) {
        return minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getScreenHeight();
    }
}
