package lol.silencecute.client.render.hud;

import lol.silencecute.Client;
import lol.silencecute.api.Operator;
import lol.silencecute.client.ClientEvents;
import lol.silencecute.client.LunaGrenadeClient;
import lol.silencecute.client.LunaReconArrowClient;
import lol.silencecute.client.LunaShockArrowClient;
import lol.silencecute.client.SkillReadyFlash;
import lol.silencecute.client.TempstUltAnchorClient;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.manager.DashService;
import lol.silencecute.manager.LunaGrenadeService;
import lol.silencecute.manager.LunaReconArrowService;
import lol.silencecute.manager.LunaShockArrowService;
import lol.silencecute.manager.MagneticBombService;
import lol.silencecute.manager.TempstRollService;
import lol.silencecute.manager.TempstUltimateService;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import lol.silencecute.client.font.ModFonts;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SkillHudOverlay {

    private static final int SLOT_W = 26;
    private static final int SLOT_COUNT = 3;

    private static final int BOX_W = SLOT_W * SLOT_COUNT;
    private static final int BOX_H = 22;

    private static final int KEY_GAP = 2;
    private static final int KEY_H = 6;

    private static final int BASE_W = BOX_W;
    private static final int BASE_H = BOX_H + KEY_GAP + KEY_H;

    private static final int ICON_OFFSET_Y = 3;
    private static final float MIN_SIZE = 20.0F;
    private static final float MAX_SIZE = 320.0F;

    private static final int ARROW_SLOT = 1;
    private static final int CONSUMABLE_SLOT = 2;

    private static final int BG_COLOR       = 0x5C060709;
    private static final int RECHARGE_COLOR = 0x3D5E5E5E;
    private static final int KEY_BG_COLOR   = 0x8E424242;
    private static final int COUNT_COLOR    = 0xFFFFFFFF;
    private static final int KEY_TEXT_COLOR = 0xFF000000;

    private static final ResourceLocation TEX_LUNA_ULT  = tex("arrowa.png");
    private static final ResourceLocation TEX_LUNA_DASH = tex("arrowb.png");
    private static final ResourceLocation TEX_LUNA_BOMB = tex("greade.png");
    private static final ResourceLocation TEX_WL_CANNON = tex("weilong_bigzhao.png");
    private static final ResourceLocation TEX_WL_DASH   = tex("weilong_dash.png");
    private static final ResourceLocation TEX_WL_BOMB   = tex("c4.png");
    private static final ResourceLocation TEX_TEMPST_ULT   = tex("tempst_ult.png");
    private static final ResourceLocation TEX_TEMPST_DASH  = tex("tempst_dash.png");
    private static final ResourceLocation TEX_TEMPST_SKILL2 = tex("tempst_skill2.png");

    private static final Minecraft MC = Minecraft.getInstance();

    private static final HudWidget WIDGET = new HudWidget("skills", "干员技能", BASE_W, BASE_H,
            MIN_SIZE, MAX_SIZE,
            (screenW, screenH) -> new float[]{
                    screenW - 6.0F - BOX_W,
                    screenH - 63.0F - BOX_H
            });

    static {
        DragManager.register(WIDGET);
    }

    private SkillHudOverlay() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Client.MOD_ID, "textures/gui/" + name);
    }

    private record SkillStats(int charges, int maxCharges, long remainMillis, long totalMillis) {}

    public static boolean isSlotRecharging(Operator operator, int slot) {
        SkillStats stats = getStats(operator, slot);
        return stats != null && stats.remainMillis() > 0L && stats.charges() < stats.maxCharges();
    }
    private static SkillStats getStats(Operator operator, int slot) {
        if (operator == Operator.LUNA) {
            return switch (slot) {
                case 0 -> new SkillStats(
                        LunaReconArrowClient.reconCharges(), LunaReconArrowService.MAX_CHARGES,
                        LunaReconArrowClient.rechargeMillis(), LunaReconArrowService.RECHARGE_TICKS * 50L);
                case ARROW_SLOT -> new SkillStats(
                        LunaShockArrowClient.arrowCharges(), LunaShockArrowService.MAX_CHARGES,
                        LunaShockArrowClient.rechargeMillis(), LunaShockArrowService.RECHARGE_TICKS * 50L);
                case CONSUMABLE_SLOT -> new SkillStats(
                        LunaGrenadeClient.grenadeCharges(), LunaGrenadeService.MAX_CHARGES,
                        LunaGrenadeClient.rechargeMillis(), LunaGrenadeService.RECHARGE_TICKS * 50L);
                default -> null;
            };
        }
        if (operator == Operator.JIFENG) {
            return switch (slot) {
                case 0 -> {
                    long ultCooldown = ClientEvents.ultimateCooldownMillis();
                    yield new SkillStats(ultCooldown <= 0L ? 1 : 0, 1, ultCooldown,
                            TempstUltimateService.COOLDOWN_TICKS * 50L);
                }
                case ARROW_SLOT -> {
                    long cooldown = ClientEvents.cooldownMillis();
                    yield new SkillStats(cooldown <= 0L ? 1 : 0, 1, cooldown,
                            TempstRollService.COOLDOWN_TICKS * 50L);
                }
                default -> null;
            };
        }
        return switch (slot) {
            case ARROW_SLOT -> {
                long cooldown = ClientEvents.cooldownMillis();
                yield new SkillStats(cooldown <= 0L ? 1 : 0, 1, cooldown,
                        DashService.COOLDOWN_TICKS * 50L);
            }
            case CONSUMABLE_SLOT -> new SkillStats(
                    ClientEvents.bombCharges(), MagneticBombService.MAX_CHARGES,
                    ClientEvents.bombRechargeMillis(), MagneticBombService.RECHARGE_TICKS * 50L);
            default -> null;
        };
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (MC.player == null || MC.options.hideGui) return;

        if (!ModConfigData.hudSkills() || !ModConfigData.operatorEnabled()) {
            WIDGET.setVisible(false);
            return;
        }

        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();
        WIDGET.ensureInitialized(screenW, screenH);

        Operator operator = ModConfigData.operator();
        WIDGET.setVisible(operator != Operator.NONE);
        if (operator == Operator.NONE) return;

        GuiGraphics graphics = event.getGuiGraphics();
        draw(graphics, MC.font, operator);

        DragManager.renderWidgetDebug(graphics, WIDGET,
                HudDraw.mouseX(MC), HudDraw.mouseY(MC));
    }

    private static void draw(GuiGraphics graphics, Font font, Operator operator) {
        HudDraw.pushWidget(graphics, WIDGET);

        HudBlur.apply(graphics, 0.0F, 0.0F, BOX_W, BOX_H, 0.0F);
        graphics.fill(0, 0, BOX_W, BOX_H, BG_COLOR);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            drawSlot(graphics, font, slot, operator);
        }

        HudDraw.popWidget(graphics);
    }

    private static void drawSlot(GuiGraphics graphics, Font font, int slot, Operator operator) {
        int slotX = slot * SLOT_W;
        int iconX = slotX + (SLOT_W - 16) / 2;
        int iconY = ICON_OFFSET_Y;

        HudDraw.drawSafeIcon(graphics, getIcon(slot, operator), iconX, iconY, 16);

        SkillStats stats = getStats(operator, slot);

        if (stats != null) {
            drawRechargeOverlay(graphics, slotX, stats);
        }

        if (stats != null && stats.charges() >= 0
                && !((slot == 0 || slot == ARROW_SLOT) && operator != Operator.LUNA)) {
            String countStr = String.valueOf(stats.charges());
            float textScale = 0.7F;
            float scaledWidth = ModFonts.width(countStr) * textScale;
            HudDraw.drawScaledString(graphics, font, countStr,
                    iconX + 16 - scaledWidth + 1.0F, iconY - 0.5F, textScale, COUNT_COLOR);
        }

        String keyName = getKeyName(slot);
        if (stats != null && (stats.charges() == 0
                || (operator == Operator.WEILONG && slot == ARROW_SLOT
                && ClientEvents.cooldownMillis() > 0L))) {
            if (stats.remainMillis() > 0L) {
                if (slot == 0 && operator != Operator.WEILONG && stats.totalMillis() > 0L) {
                    int percent = (int) Math.ceil(100.0D
                            * (1.0D - (double) stats.remainMillis() / (double) stats.totalMillis()));
                    keyName = percent + "%";
                } else {
                    keyName = String.valueOf((int) Math.ceil(stats.remainMillis() / 1000.0D));
                }
            }
        }

        drawKeyHint(graphics, font, slotX, keyName);

        float flash = SkillReadyFlash.flashAlpha(slot);
        if (flash > 0.0F) {
            int flashAlpha = (int) (flash * 255.0F) & 0xFF;
            graphics.fill(slotX, 0, slotX + SLOT_W, BOX_H, (flashAlpha << 24) | 0xFFFFFF);
        }

        if (slot == 0 && TempstUltAnchorClient.isActive()) {
            double seconds = System.nanoTime() / 1.0E9D;
            float breathe = (float) (0.15D + 0.13D * (0.5D + 0.5D * Math.sin(seconds * 5.6D)));
            int breatheAlpha = (int) (breathe * 255.0F) & 0xFF;
            graphics.fill(slotX, 0, slotX + SLOT_W, BOX_H, (breatheAlpha << 24) | 0xFF0000);
        }
    }

    private static void drawRechargeOverlay(GuiGraphics graphics, int slotX, SkillStats stats) {
        if (stats.charges() >= stats.maxCharges()
                || stats.remainMillis() <= 0L
                || stats.totalMillis() <= 0L) {
            return;
        }
        float progress = 1.0F - (float) stats.remainMillis() / (float) stats.totalMillis();
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        int barHeight = (int) (BOX_H * progress);
        if (barHeight <= 0) return;
        graphics.fill(slotX, BOX_H - barHeight, slotX + SLOT_W, BOX_H, RECHARGE_COLOR);
    }

    private static void drawKeyHint(GuiGraphics graphics, Font font, int slotX, String keyName) {
        if (keyName == null || keyName.isEmpty()) return;

        float keyScale = 0.5F;
        int textW = (int) ((int) ( ModFonts.width(keyName) * keyScale));
        int boxW = Math.max(textW + 2, 6);
        int boxH = KEY_H;
        int boxX = slotX + (SLOT_W - boxW) / 2;
        int boxY = BOX_H + KEY_GAP;

        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, KEY_BG_COLOR);

        float textX = boxX + (boxW - ModFonts.width(keyName) * keyScale) / 2.0F;
        float textY = boxY + (boxH - 8 * keyScale) / 2.0F;
        HudDraw.drawScaledString(graphics, font, keyName, textX, textY, keyScale, KEY_TEXT_COLOR);
    }

    private static ResourceLocation getIcon(int slot, Operator operator) {
        if (operator == Operator.LUNA) {
            return switch (slot) {
                case 0 -> TEX_LUNA_ULT;
                case ARROW_SLOT -> TEX_LUNA_DASH;
                default -> TEX_LUNA_BOMB;
            };
        }
        if (operator == Operator.JIFENG) {
            return switch (slot) {
                case 0 -> TEX_TEMPST_ULT;
                case ARROW_SLOT -> TEX_TEMPST_DASH;
                default -> TEX_TEMPST_SKILL2;
            };
        }
        return switch (slot) {
            case 0 -> TEX_WL_CANNON;
            case ARROW_SLOT -> TEX_WL_DASH;
            default -> TEX_WL_BOMB;
        };
    }

    private static String getKeyName(int slot) {
        KeyMapping mapping = switch (slot) {
            case 0 -> ClientEvents.ULTIMATE_KEY;
            case ARROW_SLOT -> ClientEvents.DASH_KEY;
            default -> ClientEvents.BOMB_KEY;
        };
        InputConstants.Key key = mapping.getKey();

        if (key.getType() == InputConstants.Type.MOUSE) {
            int code = key.getValue();
            if (code == 3 || code == 4) return code == 3 ? "M4" : "M5";
        }
        return mapping.getTranslatedKeyMessage().getString();
    }
}
