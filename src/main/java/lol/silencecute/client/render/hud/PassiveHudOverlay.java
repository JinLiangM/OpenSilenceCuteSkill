package lol.silencecute.client.render.hud;

import lol.silencecute.Client;
import lol.silencecute.api.Operator;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.event.PassiveService;
import lol.silencecute.registry.ModEffects;

import lol.silencecute.client.anim.UiAnimation.DecelerateAnimation;
import lol.silencecute.client.anim.UiAnimation.Direction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import lol.silencecute.client.font.ModFonts;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PassiveHudOverlay {

    private static final int BOX_W = 24;
    private static final int BOX_H = 24;

    private static final ResourceLocation TEX_KINETIC =
             new ResourceLocation("silencecute_skill", "textures/gui/kinetic_boost.png");
    private static final ResourceLocation TEX_TEMPST_PASSIVE =
            new ResourceLocation("silencecute_skill", "textures/gui/tempst_passive.png");
    private static final int KINETIC_TEX_SIZE = 100;

    private static final float ICON_INSET = 1f;

    private static final float ICON_DX = 0f;
    private static final float ICON_DY = 0f;

    private static final float TEMPST_ICON_DX = -0.5F;

    private static final int TEXT_GAP = 2;

    private static final int BASE_W = BOX_W;
    private static final int BASE_H = BOX_H + TEXT_GAP + 8;

    private static final float BAR_MAX_HEIGHT = BOX_H * 0.5F;

    private static final float MIN_SIZE = 10.0F;
    private static final float MAX_SIZE = 320.0F;

    private static final float BOOST_TIME_SCALE = 1.16F;

    private static final float COOLDOWN_TIME_SCALE = 1.4F;

    private static final float BAR_LERP = 0.225F;

    private static final float BAR_FULL_EPSILON = 0.5F;

    private static final long GAP_MS = 200L;

    private static final float MS_PER_TICK = 50.0F;

    private static final String TEXT_BOOST = "加速";
    private static final String TEXT_TEMPST_PASSIVE = "爆发型辅助背椎";
    private static final String TEXT_COOLDOWN = "冷却";

    private static final int TEXT_OFFSET_X = 1;

    private static final int BG_MAX_ALPHA = 140;

    private static final HudWidget WIDGET = new HudWidget("passive", "被动技能", BASE_W, BASE_H,
            MIN_SIZE, MAX_SIZE,
            (screenW, screenH) -> new float[]{
                    screenW / 2.0F - BOX_W / 2.0F,
                    screenH / 2.0F + 60.0F
            }).debugPadding(3.0F);

    static {
        DragManager.register(WIDGET);
    }

    private static final DecelerateAnimation alphaAnim = new DecelerateAnimation(150, 1.0);
    private static final DecelerateAnimation scaleAnim = new DecelerateAnimation(180, 1.0);

    private static final DecelerateAnimation debugAnim = new DecelerateAnimation(150, 1.0, Direction.BACKWARDS);

    private static final DecelerateAnimation joinFade = new DecelerateAnimation(400, 1.0, Direction.BACKWARDS);

    private static boolean chatOpen = false;

    private static float currentBarHeight = 0.0F;
    private static boolean barReachedTop = false;

    private static HudState state = HudState.IDLE;

    private static long stateEnteredAt = 0L;

    private static int lastEffectDuration = -1;

    private enum HudState {
        IDLE, BOOST, GAP, COOLDOWN
    }

    private static boolean armed;

    private PassiveHudOverlay() {}

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        state = HudState.IDLE;
        stateEnteredAt = 0L;
        barReachedTop = false;
        currentBarHeight = 0.0F;
        lastEffectDuration = -1;
        chatOpen = false;
        armed = false;
        joinFade.setDirection(Direction.BACKWARDS);
        joinFade.timerUtil.setTime(0L);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        if (!ModConfigData.hudPassive() || !ModConfigData.operatorEnabled()) {
            WIDGET.setVisible(false);
            return;
        }

        WIDGET.setVisible(true);

        WIDGET.ensureInitialized(event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight());

        MobEffectInstance effect = minecraft.player.getEffect(ModEffects.KINETIC_BOOST.get());
        boolean hasBoost = effect != null;

        boolean editing = DragManager.isEditingNow();
        long now = System.currentTimeMillis();

        if (editing != chatOpen) {
            chatOpen = editing;
            if (editing) {
                debugAnim.setDirection(Direction.FORWARDS);
            } else {
                debugAnim.setDirection(Direction.BACKWARDS);
            }
            debugAnim.timerUtil.setTime(0L);
        }

        int currentDuration = hasBoost ? effect.getDuration() : -1;
        boolean freshBoost = hasBoost && currentDuration > lastEffectDuration;
        lastEffectDuration = currentDuration;

        if (freshBoost) {
            armed = true;
        }

        advanceState(freshBoost, hasBoost, now);

        boolean showBoost = state == HudState.BOOST;
        boolean showCooldown = state == HudState.COOLDOWN;

        boolean showDebugBox = (state == HudState.IDLE || editing)
                && (editing || armed)
                && (chatOpen || (float) debugAnim.getOutput() > 0.005F
                || (float) joinFade.getOutput() > 0.005F);

        if (showBoost || showCooldown || showDebugBox) {
            renderHud(event, minecraft, showBoost, showCooldown, showDebugBox, effect, now);
        }

        DragManager.renderWidgetDebug(event.getGuiGraphics(), WIDGET,
                HudDraw.mouseX(minecraft), HudDraw.mouseY(minecraft));
    }

    private static void renderHud(RenderGuiEvent.Post event, Minecraft minecraft,
                                  boolean showBoost, boolean showCooldown, boolean showDebugBox,
                                  MobEffectInstance effect, long now) {

        float targetBarHeight = 0.0F;
        if (showBoost && effect != null) {
            float partialTick = minecraft.getFrameTime();
            float maxTicks = PassiveService.hudBoostDurationTicks();
            float remaining = Mth.clamp(effect.getDuration() - partialTick, 0.0F, maxTicks);
            targetBarHeight = BAR_MAX_HEIGHT * riseT(maxTicks - remaining, maxTicks, BOOST_TIME_SCALE);
        } else if (showCooldown) {
            float maxTicks = PassiveService.hudBoostDurationTicks();
            float elapsedTicks = (now - stateEnteredAt) / MS_PER_TICK;
            targetBarHeight = BAR_MAX_HEIGHT * riseT(elapsedTicks, maxTicks, COOLDOWN_TIME_SCALE);
        }

        currentBarHeight = Mth.lerp(BAR_LERP, currentBarHeight, targetBarHeight);

        if ((showBoost || showCooldown) && currentBarHeight >= BAR_MAX_HEIGHT - BAR_FULL_EPSILON) {
            barReachedTop = true;
        }

        if (barReachedTop) {
            alphaAnim.setDirection(Direction.BACKWARDS);
        } else {
            alphaAnim.setDirection(Direction.FORWARDS);
        }
        scaleAnim.setDirection(Direction.FORWARDS);

        float contentAlpha;
        float scaleAnimation;
        if (showDebugBox) {
            float debugOutput = Math.max((float) debugAnim.getOutput(), (float) joinFade.getOutput());
            contentAlpha = debugOutput;
            scaleAnimation = 0.2F + debugOutput * 0.8F;
        } else {
            contentAlpha = (float) alphaAnim.getOutput();
            scaleAnimation = 0.2F + (float) scaleAnim.getOutput() * 0.8F;
        }

        if (contentAlpha < 0.01F) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;

        int alphaInt = (int) (contentAlpha * 255);

        int bgAlpha = (int) (contentAlpha * BG_MAX_ALPHA);

        int boxX = Mth.floor(WIDGET.x());
        int boxY = Mth.floor(WIDGET.y());
        float centerX = boxX + WIDGET.width() / 2.0F;
        float centerY = boxY + WIDGET.height() / 2.0F;

        graphics.pose().pushPose();

        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(scaleAnimation, scaleAnimation, 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0F);

        HudDraw.pushWidget(graphics, WIDGET);

        HudBlur.apply(graphics, 0.0F, 0.0F, BOX_W, BOX_H, 0.0F);
        graphics.fill(0, 0, BOX_W, BOX_H, bgAlpha << 24);

        if (contentAlpha > 0.02F) {
            int inset = (int) ICON_INSET;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, contentAlpha));
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ZERO);
            graphics.pose().pushPose();
            float iconDx = ICON_DX + 0.45f;
            if (ModConfigData.operator() == Operator.JIFENG) iconDx += TEMPST_ICON_DX;
            graphics.pose().translate(iconDx, ICON_DY, 0.0F);
            graphics.blit(icon(), inset, inset, BOX_W - inset * 2, BOX_H - inset * 2,
                    0.0F, 0.0F, KINETIC_TEX_SIZE, KINETIC_TEX_SIZE, KINETIC_TEX_SIZE, KINETIC_TEX_SIZE);
            graphics.pose().popPose();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (currentBarHeight > 0.1F) {
            int barColor = ((int) (170 * contentAlpha) << 24) | 0xFFFFFF;
            graphics.pose().pushPose();
            graphics.pose().scale(1.0F, currentBarHeight / BOX_H, 1.0F);
            graphics.fill(0, 0, BOX_W, BOX_H, barColor);
            graphics.pose().popPose();
        }

        String text = showCooldown ? TEXT_COOLDOWN : boostLabel();
        float rawTextWidth = ModFonts.width(text);
        float textScale = 1.0F;
        float maxTextWidth = BOX_W * 3.2F;
        if (rawTextWidth > maxTextWidth) textScale = maxTextWidth / rawTextWidth;
        int textW = (int) (rawTextWidth * textScale);
        HudDraw.drawScaledString(graphics, font, text,
                (BOX_W - textW) / 2.0F + TEXT_OFFSET_X,
                BOX_H + TEXT_GAP, textScale, (alphaInt << 24) | 0xFFFFFF, true);

        HudDraw.popWidget(graphics);
        graphics.pose().popPose();
    }

    private static ResourceLocation icon() {
        return ModConfigData.operator() == Operator.JIFENG ? TEX_TEMPST_PASSIVE : TEX_KINETIC;
    }

    private static String boostLabel() {
        return ModConfigData.operator() == Operator.JIFENG ? TEXT_TEMPST_PASSIVE : TEXT_BOOST;
    }

    private static void advanceState(boolean freshBoost, boolean hasBoost, long now) {
        switch (state) {
            case IDLE -> {
                if (freshBoost) enterState(HudState.BOOST, now);
            }
            case BOOST -> {
                if (!hasBoost) {
                    alphaAnim.setDirection(Direction.BACKWARDS);
                    if ((float) alphaAnim.getOutput() < 0.01F) {
                        enterState(HudState.GAP, now);
                    }
                } else if (barReachedTop && (float) alphaAnim.getOutput() < 0.01F) {
                    enterState(HudState.GAP, now);
                }
            }
            case GAP -> {
                if (freshBoost) {
                    enterState(HudState.BOOST, now);
                } else if (now - stateEnteredAt >= GAP_MS) {
                    enterState(ModConfigData.operator() == Operator.JIFENG
                            ? HudState.IDLE : HudState.COOLDOWN, now);
                }
            }
            case COOLDOWN -> {
                if (freshBoost) {
                    enterState(HudState.BOOST, now);
                } else if (barReachedTop && (float) alphaAnim.getOutput() < 0.01F) {
                    enterState(HudState.IDLE, now);
                }
            }
            default -> {
            }
        }
    }

    private static void enterState(HudState newState, long now) {
        state = newState;
        stateEnteredAt = now;
        currentBarHeight = 0.0F;
        barReachedTop = false;
        alphaAnim.setDirection(Direction.FORWARDS);
        scaleAnim.setDirection(Direction.FORWARDS);
        alphaAnim.timerUtil.setTime(0L);
        scaleAnim.timerUtil.setTime(0L);
    }

    private static float riseT(float elapsedTicks, float maxTicks, float timeScale) {
        float riseTicks = maxTicks / timeScale;
        return Mth.clamp(elapsedTicks / riseTicks, 0.0F, 1.0F);
    }
}
