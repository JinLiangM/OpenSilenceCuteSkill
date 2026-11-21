package lol.silencecute.client.render.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import lol.silencecute.Client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import lol.silencecute.client.font.ModFonts;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class LunaExposureHud {
    private static final ResourceLocation TEX_EAGLE = ResourceLocation.fromNamespaceAndPath(
            Client.MOD_ID, "textures/gui/exposure_eagle.png");
    private static final ResourceLocation TEX_STRIPE = ResourceLocation.fromNamespaceAndPath(
            Client.MOD_ID, "textures/gui/exposure_stripe.png");

    private static final int FINAL_WIDTH = 80;
    private static final int HEIGHT = 20;
    private static final int TOP_MARGIN = 35;

    private static final float BASE_W = FINAL_WIDTH;
    private static final int EAGLE_SIZE = 15;

    private static final long TIME_FADE_IN = 250L;
    private static final long TIME_FADE_OUT = 400L;

    private static long totalDuration = 2000L;
    private static long startTime = 0L;

    private static final HudWidget WIDGET = new HudWidget("exposure", "技能警告", FINAL_WIDTH, HEIGHT,
            12.0F, 480.0F,
            (screenW, screenH) -> new float[]{screenW / 2.0F - FINAL_WIDTH / 2.0F, TOP_MARGIN});

    static {
        DragManager.register(WIDGET);
    }

    private LunaExposureHud() {
    }

    public static void flash(long durationMs) {
        long now = System.currentTimeMillis();
        if (now >= startTime + totalDuration) {
            totalDuration = Math.max(TIME_FADE_IN + TIME_FADE_OUT + 100L, durationMs);
            startTime = now;
        } else {
            totalDuration = Math.max(totalDuration, durationMs);
        }
    }

    private static boolean previewing;
    private static boolean wasEditing;
    private static long previewStart;

    public static HudWidget widget() {
        return WIDGET;
    }

    public static boolean isPlaying() {
        return startTime != 0L && System.currentTimeMillis() < startTime + totalDuration;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;

        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();
        WIDGET.ensureInitialized(screenW, screenH);

        boolean editing = DragManager.isEditingNow();
        if (editing && !wasEditing) {
            previewing = true;
            previewStart = System.currentTimeMillis();
        } else if (!editing && wasEditing) {
            previewing = false;
            startTime = 0L;
        }
        wasEditing = editing;

        long elapsed;
        if (previewing) {

            elapsed = System.currentTimeMillis() - previewStart;
        } else {
            elapsed = System.currentTimeMillis() - startTime;
            if (startTime == 0L || elapsed >= totalDuration) {
                startTime = 0L;

                DragManager.renderWidgetDebug(event.getGuiGraphics(), WIDGET,
                        HudDraw.mouseX(minecraft), HudDraw.mouseY(minecraft));
                return;
            }
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;

        float globalAlpha = 1.0F;
        if (!previewing) {
            if (elapsed < TIME_FADE_IN) {
                globalAlpha = elapsed / (float) TIME_FADE_IN;
            } else if (elapsed < totalDuration - TIME_FADE_OUT) {
                globalAlpha = 1.0F;
            } else {
                globalAlpha = 1.0F - (elapsed - (totalDuration - TIME_FADE_OUT)) / (float) TIME_FADE_OUT;
            }
            globalAlpha = Math.max(0.0F, Math.min(1.0F, globalAlpha));
        }

        int ix = WIDGET.intX();
        int iy = WIDGET.intY();
        int iw = Math.max(1, (int) WIDGET.width());
        int ih = Math.max(1, (int) WIDGET.height());
        float sx = (float) iw / BASE_W;
        float sy = (float) ih / HEIGHT;

        float currentWidth = BASE_W;
        int topY = 0;
        int bottomY = HEIGHT;
        int drawLeft = 0;

        graphics.pose().pushPose();
        graphics.pose().translate(ix, iy, 0.0F);
        graphics.pose().scale(sx, sy, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, globalAlpha);

        graphics.enableScissor(ix, iy, ix + iw, iy + ih);

        int bgAlpha = (int) (0xCC * globalAlpha);
        graphics.fill(drawLeft, topY, drawLeft + (int) currentWidth, bottomY, (bgAlpha << 24) | 0x151515);

        int stripeWidth = 5;
        int scrollOffset = (int) ((System.currentTimeMillis() / 15L) % stripeWidth);
        RenderSystem.setShaderColor(1.0F, 0.4F, 0.4F, 0.15F * globalAlpha);

        int stripeYOffset = 2;
        int stripeHeight = HEIGHT - (stripeYOffset * 2);

        for (int x = drawLeft - stripeWidth; x < drawLeft + currentWidth + stripeWidth; x += stripeWidth) {
            int drawX = x + (stripeWidth - scrollOffset);
            graphics.blit(TEX_STRIPE, drawX, topY + stripeYOffset, 0, 0, stripeWidth, stripeHeight, stripeWidth, stripeHeight);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, globalAlpha);

        int borderAlpha = (int) (255 * globalAlpha);
        int borderColor = (borderAlpha << 24) | 0xAA1A1A;
        int thickness = 1;
        int cornerLength = 6;
        int drawWidth = (int) currentWidth;

        graphics.fill(drawLeft, topY, drawLeft + thickness, bottomY, borderColor);
        graphics.fill(drawLeft, topY, drawLeft + cornerLength, topY + thickness, borderColor);
        graphics.fill(drawLeft, bottomY - thickness, drawLeft + cornerLength, bottomY, borderColor);

        int rightX = drawLeft + drawWidth;
        graphics.fill(rightX - thickness, topY, rightX, bottomY, borderColor);
        graphics.fill(rightX - cornerLength, topY, rightX, topY + thickness, borderColor);
        graphics.fill(rightX - cornerLength, bottomY - thickness, rightX, bottomY, borderColor);

        int eagleCurrentY = topY + (HEIGHT - EAGLE_SIZE) / 2 + 1;
        int eagleDrawX = drawLeft + 13;
        graphics.blit(TEX_EAGLE, eagleDrawX, eagleCurrentY, 0, 0, EAGLE_SIZE, EAGLE_SIZE, EAGLE_SIZE, EAGLE_SIZE);

        String text = previewing ? "测试HUD" : "位置暴露";
        float textScale = 1F;
        float textCurrentX = drawLeft + currentWidth - ModFonts.width(text) * textScale - 13;
        float textY = topY + (HEIGHT - ModFonts.height() * textScale) / 2.0F;

        graphics.pose().pushPose();
        graphics.pose().translate(textCurrentX, textY, 0);
        graphics.pose().scale(textScale, textScale, 1.0F);

        int textAlpha = (int) (255 * globalAlpha);
        int color = (textAlpha << 24) | 0xE0E0E0;
        ModFonts.drawString(graphics, text, 0, 0, color, false);
        graphics.pose().popPose();

        graphics.disableScissor();
        graphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        DragManager.renderWidgetDebug(graphics, WIDGET,
                HudDraw.mouseX(minecraft), HudDraw.mouseY(minecraft));
    }
}
