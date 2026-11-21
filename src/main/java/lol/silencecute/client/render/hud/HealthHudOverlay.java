package lol.silencecute.client.render.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import lol.silencecute.Client;
import lol.silencecute.client.font.ModFonts;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HealthHudOverlay {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final int AVATAR = 22;

    private static final int GAP = 4;

    private static final float HP_Y = 0.0F;
    private static final float HP_SCALE = 0.9F;

    private static final float HP_MAX_SCALE = 0.75F;

    private static final float HP_MAX_RISE = 0.4F;

    private static final int BAR_X = AVATAR + GAP;
    private static final int BAR_Y = 9;
    private static final int BAR_W = 110;

    private static final int BAR_H = 4;

    private static final float NAME_Y = 16.0F;
    private static final float NAME_SCALE = 0.75F;

    private static final int BASE_W = BAR_X + BAR_W;
    private static final int BASE_H = 26;

    private static final float MIN_SIZE = 24.0F;
    private static final float MAX_SIZE = 480.0F;

    private static final int BAR_BG     = 0x8A6E6E6E;

    private static final int BAR_LAG    = 0xFF6B1212;

    private static final int BAR_FILL   = 0xFFE9E9E9;

    private static final int BAR_LOW    = 0xFF9E1414;
    private static final int NAME_COLOR = 0xFFFFFFFF;

    private static final int HP_COLOR = 0xFFFFFFFF;
    private static final int HP_MAX_COLOR = 0xFFD2D2D2;

    private static final HudWidget WIDGET = new HudWidget("health", "玩家血量", BASE_W, BASE_H,
            MIN_SIZE, MAX_SIZE,
            (screenW, screenH) -> new float[]{8.0F, 32.0F});

    static {
        DragManager.register(WIDGET);
    }

    private HealthHudOverlay() {}

    private static final float SHOWN_SPEED = 22.0F;

    private static final float LAG_HOLD_SECONDS = 0.25F;

    private static final float LAG_FALL_SPEED = 6.5F;

    private static final float LAG_RISE_SPEED = 12.0F;

    private static final float LOW_HEALTH = 5.0F;

    private static final float LOW_HP_BLEND_SPEED = 16.0F;

    private static float shownRatio = 1.0F;

    private static float lagRatio = 1.0F;

    private static float lastTarget = 1.0F;

    private static float lagHold;

    private static boolean animInit;

    private static float lowHpBlend;
    private static long lastNanos;

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (MC.player == null || MC.options.hideGui) return;

        if (!ModConfigData.hudHealth() || MC.player.isDeadOrDying() || MC.player.isSpectator()) {
            WIDGET.setVisible(false);
            return;
        }
        WIDGET.setVisible(true);

        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();
        WIDGET.ensureInitialized(screenW, screenH);

        tickAnimation();

        GuiGraphics graphics = event.getGuiGraphics();
        draw(graphics, MC.font);

        DragManager.renderWidgetDebug(graphics, WIDGET,
                HudDraw.mouseX(MC), HudDraw.mouseY(MC));
    }

    private static void tickAnimation() {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0.016F : (now - lastNanos) / 1_000_000_000.0F;
        lastNanos = now;
        dt = Mth.clamp(dt, 0.0F, 0.1F);

        float max = Math.max(1.0F, MC.player.getMaxHealth());
        float target = Mth.clamp(MC.player.getHealth() / max, 0.0F, 1.0F);

        if (!animInit) {
            animInit = true;
            shownRatio = target;
            lagRatio = target;
            lastTarget = target;
            lagHold = 0.0F;
            lowHpBlend = MC.player.getHealth() <= LOW_HEALTH ? 1.0F : 0.0F;
        }

        shownRatio = approach(shownRatio, target, dt, SHOWN_SPEED);

        if (target < lastTarget - 0.0005F) {
            lagHold = LAG_HOLD_SECONDS;
        }
        lastTarget = target;

        if (lagRatio > shownRatio) {

            if (lagHold > 0.0F) {
                lagHold = Math.max(0.0F, lagHold - dt);
            } else {
                lagRatio = approach(lagRatio, shownRatio, dt, LAG_FALL_SPEED);
            }
        } else {

            lagHold = 0.0F;
            lagRatio = approach(lagRatio, shownRatio, dt, LAG_RISE_SPEED);
        }

        float lowTarget = MC.player.getHealth() <= LOW_HEALTH ? 1.0F : 0.0F;
        lowHpBlend = approach(lowHpBlend, lowTarget, dt, LOW_HP_BLEND_SPEED);
    }

    private static float approach(float current, float target, float dt, float speed) {
        float k = 1.0F - (float) Math.exp(-dt * speed);
        float next = current + (target - current) * k;
        if (Math.abs(target - next) < 0.0005F) next = target;
        return next;
    }

    private static int mixColor(int a, int b, float t) {
        float f = Mth.clamp(t, 0.0F, 1.0F);
        int alpha = (int) Mth.lerp(f, (a >>> 24) & 0xFF, (b >>> 24) & 0xFF);
        int red   = (int) Mth.lerp(f, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int green = (int) Mth.lerp(f, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int blue  = (int) Mth.lerp(f, a & 0xFF, b & 0xFF);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static void draw(GuiGraphics graphics, Font font) {
        HudDraw.pushWidget(graphics, WIDGET);

        drawAvatar(graphics, 0, 0, AVATAR);

        graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + BAR_H, BAR_BG);

        fillBar(graphics, lagRatio, BAR_LAG);
        fillBar(graphics, shownRatio, mixColor(BAR_FILL, BAR_LOW, lowHpBlend));

        drawHealthText(graphics, font);

        HudDraw.drawScaledString(graphics, font, playerName(),
                BAR_X, NAME_Y, NAME_SCALE, NAME_COLOR);

        HudDraw.popWidget(graphics);
    }

    private static void drawHealthText(GuiGraphics graphics, Font font) {
        float health = Math.max(0.0F, MC.player.getHealth());
        float max = Math.max(1.0F, MC.player.getMaxHealth());

        int current;
        int total;
        if ("ALWAYS_100".equalsIgnoreCase(ModConfigData.healthDisplayMode())) {

            current = Math.round(health / max * 100.0F);
            total = 100;
        } else {

            current = (int) Math.ceil(health);
            total = (int) Math.ceil(max);
        }

        String main = String.valueOf(current);
        String suffix = "/" + total;

        int color = mixColor(HP_COLOR, BAR_LOW, lowHpBlend);
        HudDraw.drawScaledString(graphics, font, main, BAR_X, HP_Y, HP_SCALE, color);

        float advance = ModFonts.width(main) * HP_SCALE;

        float drop = ModFonts.height() * (HP_SCALE - HP_MAX_SCALE) - HP_MAX_RISE;
        HudDraw.drawScaledString(graphics, font, suffix,
                BAR_X + advance + 1.0F, HP_Y + drop, HP_MAX_SCALE, HP_MAX_COLOR);
    }

    private static void fillBar(GuiGraphics graphics, float ratio, int color) {
        float r = Mth.clamp(ratio, 0.0F, 1.0F);
        if (r <= 0.0005F) return;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(BAR_X, BAR_Y, 0.0F);
        pose.scale(BAR_W * r, BAR_H, 1.0F);
        graphics.fill(0, 0, 1, 1, color);
        pose.popPose();
    }

    private static void drawAvatar(GuiGraphics graphics, int x, int y, int size) {
        ResourceLocation skin;
        try {
            skin = MC.player.getSkinTextureLocation();
        } catch (Exception e) {
            return;
        }
        if (skin == null) return;

        float scale = size / 8.0F;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(skin, 0, 0, 8, 8, 8, 8, 64, 64);
        graphics.blit(skin, 0, 0, 40, 8, 8, 8, 64, 64);
        RenderSystem.disableBlend();

        pose.popPose();
    }

    private static String playerName() {
        try {
            return MC.player.getGameProfile().getName();
        } catch (Exception e) {
            return MC.player.getName().getString();
        }
    }
}
