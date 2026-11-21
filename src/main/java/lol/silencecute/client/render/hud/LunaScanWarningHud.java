package lol.silencecute.client.render.hud;

import lol.silencecute.Client;
import lol.silencecute.client.anim.LunaBowAnimation;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.util.Mth;

import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.client.event.RenderGuiEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;
import lol.silencecute.client.font.ModFonts;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)

public final class LunaScanWarningHud {

    private static final ResourceLocation TEX_EAGLE = ResourceLocation.fromNamespaceAndPath(

            Client.MOD_ID, "textures/gui/exposure_eagle.png");

    private static final ResourceLocation TEX_STRIPE = ResourceLocation.fromNamespaceAndPath(

            Client.MOD_ID, "textures/gui/exposure_stripe2.png");

    private static final int HEIGHT = 20;

    private static final int TOP_MARGIN = 35;

    private static final int EAGLE_SIZE = 15;

    private static final String TEXT = "即将扫描移动单位";

    private static final int THEME_COLOR = 0xFFFFAA00;

    private static final long TIME_SCALE_IN = 50L;

    private static final long TIME_EXPAND = 20L;

    private static final long TIME_SLIDE = 110L;

    private static final long TIME_HOLD = 2500L;

    private static final long TIME_FADE_OUT = 150L;

    private static final long TOTAL_TIME = TIME_SCALE_IN + TIME_EXPAND + TIME_SLIDE + TIME_HOLD + TIME_FADE_OUT;

    private static final long SCAN_START_TIME = TIME_SCALE_IN + TIME_EXPAND;

    private static final long SCAN_DURATION = 2500L;

    private static final int SCAN_TAIL_LENGTH = 100;

    private static long startTime = 0L;

    private static boolean forceClosed = false;

    private static boolean pendingStart = false;

    private LunaScanWarningHud() {}

    public static void flash() {

        if (LunaExposureHud.isPlaying()) {

            pendingStart = true;

        } else {

            startTime = System.currentTimeMillis();

            forceClosed = false;

        }

    }

    public static void forceClose() {

        if (startTime != 0L && !forceClosed) {

            startTime = System.currentTimeMillis() - (TIME_SCALE_IN + TIME_EXPAND + TIME_SLIDE + TIME_HOLD);

            forceClosed = true;

        }

    }

    @SubscribeEvent

    public static void onRenderGui(RenderGuiEvent.Post event) {

        if (pendingStart && !LunaExposureHud.isPlaying()) {

            pendingStart = false;

            startTime = System.currentTimeMillis();

            forceClosed = false;

        }

        if (startTime == 0L) return;

        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed >= TOTAL_TIME) {

            startTime = 0L;

            return;

        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.options.hideGui || minecraft.player == null) return;

        GuiGraphics graphics = event.getGuiGraphics();

        Font font = minecraft.font;

        int screenWidth = event.getWindow().getGuiScaledWidth();

        HudWidget widget = LunaExposureHud.widget();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        widget.ensureInitialized(screenWidth, screenHeight);
        int baseWidth = (int) (EAGLE_SIZE + 4 + 8 + ModFonts.width(TEXT) + 8);

        float barScale = Math.max(0.2F, widget.height() / (float) HEIGHT);

        float scaleX = barScale;

        float scaleY = barScale;

        float barX = widget.x() + widget.width() * 0.5F - baseWidth * barScale * 0.5F;

        graphics.pose().pushPose();

        graphics.pose().translate(barX, widget.y(), 0.0F);

        graphics.pose().scale(scaleX, scaleY, 1.0F);

        int centerX = baseWidth / 2;

        int targetWidth = baseWidth;

        float tScale = 0.0F;

        float tExpand = 0.0F;

        float tSlide = 0.0F;

        float globalAlpha = 1.0F;

        if (elapsed < TIME_SCALE_IN) {

            tScale = elapsed / (float) TIME_SCALE_IN;

        } else if (elapsed < TIME_SCALE_IN + TIME_EXPAND) {

            tScale = 1.0F;

            tExpand = (elapsed - TIME_SCALE_IN) / (float) TIME_EXPAND;

        } else if (elapsed < TIME_SCALE_IN + TIME_EXPAND + TIME_SLIDE) {

            tScale = 1.0F;

            tExpand = 1.0F;

            tSlide = (elapsed - TIME_SCALE_IN - TIME_EXPAND) / (float) TIME_SLIDE;

        } else if (elapsed < TOTAL_TIME - TIME_FADE_OUT) {

            tScale = 1.0F;

            tExpand = 1.0F;

            tSlide = 1.0F;

        } else {

            tScale = 1.0F;

            tExpand = 1.0F;

            tSlide = 1.0F;

            globalAlpha = 1.0F - (elapsed - (TOTAL_TIME - TIME_FADE_OUT)) / (float) TIME_FADE_OUT;

        }

        tScale = LunaBowAnimation.smooth(tScale);

        tExpand = LunaBowAnimation.smooth(tExpand);

        tSlide = LunaBowAnimation.smooth(tSlide);

        globalAlpha = Math.max(0.0F, Math.min(1.0F, globalAlpha));

        float currentScale = Mth.lerp(tScale, 0.3F, 1.0F);

        float currentWidth = Mth.lerp(tExpand, EAGLE_SIZE + 12, targetWidth);

        float centerXFloat = centerX;

        float leftEdgeX = centerX - currentWidth / 2.0F;

        float eagleTargetX = centerX - EAGLE_SIZE / 2.0F;

        float eagleLeftX = leftEdgeX + 4.0F;

        float eagleCurrentX = Mth.lerp(tSlide, eagleTargetX, eagleLeftX);

        int topY = 0;

        int bottomY = topY + HEIGHT;

        int drawLeft = (int) (centerXFloat - currentWidth / 2.0F);

        RenderSystem.enableBlend();

        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, globalAlpha);

        graphics.pose().pushPose();

        graphics.pose().translate(centerXFloat, topY + HEIGHT / 2.0F, 0);

        graphics.pose().scale(currentScale, currentScale, 1.0F);

        graphics.pose().translate(-centerXFloat, -(topY + HEIGHT / 2.0F), 0);

        graphics.enableScissor(
                (int) (barX + drawLeft * scaleX),
                (int) (widget.y() + topY * scaleY),
                (int) (barX + (drawLeft + currentWidth) * scaleX),
                (int) (widget.y() + (topY + HEIGHT) * scaleY));

        int bgAlpha = (int) (0xCC * globalAlpha);

        graphics.fill(drawLeft, topY, drawLeft + (int) currentWidth, bottomY, (bgAlpha << 24) | 0x151515);

        int stripeWidth = 5;

        int scrollOffset = (int) ((System.currentTimeMillis() / 15L) % stripeWidth);

        float r = ((THEME_COLOR >> 16) & 0xFF) / 255.0F;

        float g = ((THEME_COLOR >> 8) & 0xFF) / 255.0F;

        float b = (THEME_COLOR & 0xFF) / 255.0F;

        float darken = 0.6F;

        float stripeAlpha = 0.15F * globalAlpha;

        RenderSystem.setShaderColor(r * darken, g * darken, b * darken, stripeAlpha);

        int stripeYOffset = 2;

        int stripeHeight = HEIGHT - (stripeYOffset * 2);

        for (int x = drawLeft - stripeWidth; x < drawLeft + currentWidth + stripeWidth; x += stripeWidth) {

            int drawX = x + scrollOffset;

            graphics.blit(TEX_STRIPE, drawX, topY + stripeYOffset, 0, 0, stripeWidth, stripeHeight, stripeWidth, stripeHeight);

        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, globalAlpha);

        long scanElapsed = elapsed - SCAN_START_TIME;

        if (scanElapsed > 0 && scanElapsed < SCAN_DURATION) {

            float progress = scanElapsed / (float) SCAN_DURATION;

            int headX = drawLeft + (int) (currentWidth * progress);

            for (int i = 0; i < SCAN_TAIL_LENGTH; i += 2) {

                int segX = headX - i;

                if (segX < drawLeft) break;

                float alphaRatio = 1.0F - (float) i / SCAN_TAIL_LENGTH;

                int scanAlpha = (int) (alphaRatio * 220 * globalAlpha);

                int color = (scanAlpha << 24) | (THEME_COLOR & 0x00FFFFFF);

                int leftSegX = Math.max(drawLeft, segX - 2);

                if (leftSegX < segX) {

                    graphics.fill(leftSegX, topY + stripeYOffset, segX, bottomY - stripeYOffset, color);

                }

            }

        }

        int thickness = 1;

        int cornerLength = 6;

        int drawWidth = (int) currentWidth;

        int borderAlpha = (int) (255 * globalAlpha);

        int borderColor = (borderAlpha << 24) | (THEME_COLOR & 0x00FFFFFF);

        graphics.fill(drawLeft, topY, drawLeft + thickness, bottomY, borderColor);

        graphics.fill(drawLeft, topY, drawLeft + cornerLength, topY + thickness, borderColor);

        graphics.fill(drawLeft, bottomY - thickness, drawLeft + cornerLength, bottomY, borderColor);

        int rightX = drawLeft + drawWidth;

        graphics.fill(rightX - thickness, topY, rightX, bottomY, borderColor);

        graphics.fill(rightX - cornerLength, topY, rightX, topY + thickness, borderColor);

        graphics.fill(rightX - cornerLength, bottomY - thickness, rightX, bottomY, borderColor);

        int eagleCurrentY = topY + (HEIGHT - EAGLE_SIZE) / 2 + 1;

        int eagleDrawX = (int) (centerXFloat + (eagleCurrentX - centerXFloat) * currentScale) + 5;

        graphics.blit(TEX_EAGLE, eagleDrawX, eagleCurrentY, 0, 0, EAGLE_SIZE, EAGLE_SIZE, EAGLE_SIZE, EAGLE_SIZE);

        if (tSlide > 0.01F) {

            float textScale = 1.0F;

            float textStartX = eagleDrawX + EAGLE_SIZE + 6;

            float textTargetX = drawLeft + currentWidth - ModFonts.width(TEXT) * textScale - 6;

            float textCurrentX = Mth.lerp(tSlide, textStartX, textTargetX);

            float textY = topY + (HEIGHT - ModFonts.height() * textScale) / 2.0F;

            graphics.pose().pushPose();

            graphics.pose().translate(textCurrentX, textY, 0);

            graphics.pose().scale(textScale, textScale, 1.0F);

            int textAlpha = (int) (tSlide * 255 * globalAlpha);

            int color = (textAlpha << 24) | 0xFFFFFF;

            ModFonts.drawString(graphics, TEXT, 0, 0, color, false);

            graphics.pose().popPose();

        }

        graphics.pose().popPose();

        graphics.disableScissor();

        graphics.pose().popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.disableBlend();

    }

}

