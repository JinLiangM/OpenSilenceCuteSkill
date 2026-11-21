package lol.silencecute.client.render.hud.notifications;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import lol.silencecute.Client;
import lol.silencecute.client.font.ModFonts;

public final class Notification {
    private static final ResourceLocation TEX_EAGLE = ResourceLocation.fromNamespaceAndPath(
            Client.MOD_ID, "textures/gui/exposure_eagle.png");

    private static final ResourceLocation TEX_STRIPE_RED = ResourceLocation.fromNamespaceAndPath(
            Client.MOD_ID, "textures/gui/exposure_stripe.png");

    private static final ResourceLocation TEX_STRIPE_ORANGE = ResourceLocation.fromNamespaceAndPath(
            Client.MOD_ID, "textures/gui/exposure_stripe2.png");

    private static final int HEIGHT = 20;
    private static final int TOP_MARGIN = 35;
    private static final int EAGLE_SIZE = 15;

    private static final int WARNING_MIN_WIDTH = 80;
    private static final long WARNING_FADE_IN_MS = 250L;
    private static final long WARNING_FADE_OUT_MS = 400L;
    private static final long WARNING_MIN_DURATION_MS =
            WARNING_FADE_IN_MS + WARNING_FADE_OUT_MS + 100L;

    private static final long SCAN_SCALE_IN_MS = 50L;
    private static final long SCAN_EXPAND_MS = 20L;
    private static final long SCAN_SLIDE_MS = 110L;
    private static final long SCAN_FADE_OUT_MS = 150L;
    private static final long SCAN_FIXED_MS =
            SCAN_SCALE_IN_MS + SCAN_EXPAND_MS + SCAN_SLIDE_MS + SCAN_FADE_OUT_MS;
    private static final long SCAN_DEFAULT_SCAN_MS = 2500L;
    private static final int SCAN_TAIL_LENGTH = 100;

    private final NotificationType type;
    private final String content;
    private final long startTime;
    private final long duration;

    Notification(NotificationType type, String content, long duration) {
        this.type = type;
        this.content = content;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
    }

    public NotificationType getType() { return type; }
    public String getContent() { return content; }

    public boolean isPlaying() {
        return System.currentTimeMillis() < startTime + duration;
    }

    public long remainingMs() {
        return startTime + duration - System.currentTimeMillis();
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        switch (type) {
            case WARNING -> renderWarning(g, screenWidth);
            case SCANNING -> renderScanning(g, screenWidth);
        }
    }

    private void renderWarning(GuiGraphics g, int screenWidth) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= duration) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int textWidth = (int) ( ModFonts.width(content));
        int width = Math.max(WARNING_MIN_WIDTH, 26 + EAGLE_SIZE + textWidth);
        int centerX = screenWidth / 2;

        float alpha;
        if (elapsed < WARNING_FADE_IN_MS) {
            alpha = elapsed / (float) WARNING_FADE_IN_MS;
        } else if (elapsed < duration - WARNING_FADE_OUT_MS) {
            alpha = 1.0F;
        } else {
            alpha = 1.0F - (elapsed - (duration - WARNING_FADE_OUT_MS)) / (float) WARNING_FADE_OUT_MS;
        }
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);

        int topY = TOP_MARGIN;
        int bottomY = topY + HEIGHT;
        int drawLeft = centerX - width / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        g.enableScissor(drawLeft, topY, drawLeft + width, bottomY);

        drawPanel(g, drawLeft, topY, width, HEIGHT, alpha, type.getThemeColor());

        int stripeWidth = 5;
        int scrollOffset = (int) ((System.currentTimeMillis() / 15L) % stripeWidth);
        RenderSystem.setShaderColor(1.0F, 0.4F, 0.4F, 0.15F * alpha);
        for (int x = drawLeft - stripeWidth; x < drawLeft + width + stripeWidth; x += stripeWidth) {
            g.blit(TEX_STRIPE_RED, x + (stripeWidth - scrollOffset), topY + 2,
                    0, 0, stripeWidth, HEIGHT - 4, stripeWidth, HEIGHT - 4);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        drawEagle(g, drawLeft + 13, topY + (HEIGHT - EAGLE_SIZE) / 2 + 1);
        int textAlpha = (int) (255 * alpha);
        drawText(g, font, content,
                drawLeft + width - textWidth - 10f,
                topY + (HEIGHT - ModFonts.height()) / 2.0F,
                (textAlpha << 24) | 0xE0E0E0);

        g.disableScissor();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderScanning(GuiGraphics g, int screenWidth) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= duration) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int targetWidth = (int) (EAGLE_SIZE + 4 + 8 +  ModFonts.width(content) + 8);
        int centerX = screenWidth / 2;

        long fadeOutStart = duration - SCAN_FADE_OUT_MS;
        long scanStart = SCAN_SCALE_IN_MS + SCAN_EXPAND_MS;
        long scanDuration = Math.min(SCAN_DEFAULT_SCAN_MS, Math.max(0L, fadeOutStart - scanStart));

        float tScale, tExpand, tSlide;
        float globalAlpha = 1.0F;
        if (elapsed < SCAN_SCALE_IN_MS) {
            tScale = elapsed / (float) SCAN_SCALE_IN_MS; tExpand = 0; tSlide = 0;
        } else if (elapsed < SCAN_SCALE_IN_MS + SCAN_EXPAND_MS) {
            tScale = 1; tExpand = (elapsed - SCAN_SCALE_IN_MS) / (float) SCAN_EXPAND_MS; tSlide = 0;
        } else if (elapsed < SCAN_SCALE_IN_MS + SCAN_EXPAND_MS + SCAN_SLIDE_MS) {
            tScale = 1; tExpand = 1;
            tSlide = (elapsed - SCAN_SCALE_IN_MS - SCAN_EXPAND_MS) / (float) SCAN_SLIDE_MS;
        } else if (elapsed < fadeOutStart) {
            tScale = 1; tExpand = 1; tSlide = 1;
        } else {
            tScale = 1; tExpand = 1; tSlide = 1;
            globalAlpha = 1.0F - (elapsed - fadeOutStart) / (float) SCAN_FADE_OUT_MS;
        }
        tScale = smooth(tScale);
        tExpand = smooth(tExpand);
        tSlide = smooth(tSlide);
        globalAlpha = Mth.clamp(globalAlpha, 0.0F, 1.0F);

        float currentScale = Mth.lerp(tScale, 0.3F, 1.0F);
        float currentWidth = Mth.lerp(tExpand, EAGLE_SIZE + 12, targetWidth);
        float centerXf = centerX;
        float leftEdgeX = centerX - currentWidth / 2.0F;
        float eagleTargetX = centerX - EAGLE_SIZE / 2.0F;
        float eagleLeftX = leftEdgeX + 4.0F;
        float eagleCurrentX = Mth.lerp(tSlide, eagleTargetX, eagleLeftX);

        int topY = TOP_MARGIN;
        int bottomY = topY + HEIGHT;
        int drawLeft = (int) (centerXf - currentWidth / 2.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, globalAlpha);

        g.pose().pushPose();
        g.pose().translate(centerXf, topY + HEIGHT / 2.0F, 0);
        g.pose().scale(currentScale, currentScale, 1.0F);
        g.pose().translate(-centerXf, -(topY + HEIGHT / 2.0F), 0);
        g.enableScissor(drawLeft, topY, drawLeft + (int) currentWidth, bottomY);

        drawPanel(g, drawLeft, topY, (int) currentWidth, HEIGHT, globalAlpha, type.getThemeColor());

        int stripeWidth = 5;
        int scrollOffset = (int) ((System.currentTimeMillis() / 15L) % stripeWidth);
        float r = ((type.getThemeColor() >> 16) & 0xFF) / 255.0F;
        float gr = ((type.getThemeColor() >> 8) & 0xFF) / 255.0F;
        float b = (type.getThemeColor() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r * 0.6F, gr * 0.6F, b * 0.6F, 0.15F * globalAlpha);
        int stripeY = 2;
        int stripeH = HEIGHT - stripeY * 2;
        for (int x = drawLeft - stripeWidth; x < drawLeft + currentWidth + stripeWidth; x += stripeWidth) {
            g.blit(TEX_STRIPE_ORANGE, x + scrollOffset, topY + stripeY,
                    0, 0, stripeWidth, stripeH, stripeWidth, stripeH);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, globalAlpha);

        long scanElapsed = elapsed - scanStart;
        if (scanElapsed > 0 && scanElapsed < scanDuration) {
            float progress = scanElapsed / (float) scanDuration;
            int headX = drawLeft + (int) (currentWidth * progress);
            for (int i = 0; i < SCAN_TAIL_LENGTH; i += 2) {
                int segX = headX - i;
                if (segX < drawLeft) break;
                float ratio = 1.0F - (float) i / SCAN_TAIL_LENGTH;
                int scanAlpha = (int) (ratio * 220 * globalAlpha);
                int color = (scanAlpha << 24) | (type.getThemeColor() & 0x00FFFFFF);
                int leftSegX = Math.max(drawLeft, segX - 2);
                if (leftSegX < segX) g.fill(leftSegX, topY + stripeY, segX, bottomY - stripeY, color);
            }
        }

        int eagleY = topY + (HEIGHT - EAGLE_SIZE) / 2 + 1;
        int eagleX = (int) (centerXf + (eagleCurrentX - centerXf) * currentScale) + 5;
        drawEagle(g, eagleX, eagleY);

        if (tSlide > 0.01F) {
            float textStartX = eagleX + EAGLE_SIZE + 6;
            float textTargetX = drawLeft + currentWidth - ModFonts.width(content) - 6;
            float textX = Mth.lerp(tSlide, textStartX, textTargetX);
            float textY = topY + (HEIGHT - ModFonts.height()) / 2.0F;
            int textAlpha = (int) (tSlide * 255 * globalAlpha);
            drawText(g, font, content, textX, textY, (textAlpha << 24) | 0xFFFFFF);
        }

        g.disableScissor();
        g.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawPanel(GuiGraphics g, int left, int top, int width, int height,
                                  float alpha, int themeColor) {
        int bgAlpha = (int) (0xCC * alpha);
        g.fill(left, top, left + width, top + height, (bgAlpha << 24) | 0x151515);
        int borderAlpha = (int) (255 * alpha);
        int borderColor = (borderAlpha << 24) | (themeColor & 0x00FFFFFF);
        int thickness = 1;
        int cornerLength = 6;
        int rightX = left + width;
        g.fill(left, top, left + thickness, top + height, borderColor);
        g.fill(left, top, left + cornerLength, top + thickness, borderColor);
        g.fill(left, top + height - thickness, left + cornerLength, top + height, borderColor);
        g.fill(rightX - thickness, top, rightX, top + height, borderColor);
        g.fill(rightX - cornerLength, top, rightX, top + thickness, borderColor);
        g.fill(rightX - cornerLength, top + height - thickness, rightX, top + height, borderColor);
    }

    private static void drawEagle(GuiGraphics g, int x, int y) {
        g.blit(TEX_EAGLE, x, y, 0, 0, EAGLE_SIZE, EAGLE_SIZE, EAGLE_SIZE, EAGLE_SIZE);
    }

    private static void drawText(GuiGraphics g, Font font, String text, float x, float y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        ModFonts.drawString(g, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}