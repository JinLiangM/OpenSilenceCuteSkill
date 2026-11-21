package lol.silencecute.client.render.hud;

import lol.silencecute.Client;
import lol.silencecute.api.Operator;
import lol.silencecute.client.AccessClient;
import lol.silencecute.client.font.ModFonts;
import lol.silencecute.client.lang.Lang;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PermissionHudOverlay {

    private static final int BASE_W = 152;

    private static final int BASE_H = 126;

    private static final float MIN_SIZE = 20.0F;

    private static final float MAX_SIZE = 320.0F;

    private static final int BG_COLOR = 0x8C060709;
    private static final int TITLE_COLOR = 0xFFEDEDED;
    private static final int LABEL_COLOR = 0xFF9A9AA2;
    private static final int ADMIN_COLOR = 0xFFFFC24B;
    private static final int PLAYER_COLOR = 0xFF9FB4C7;
    private static final int OK_COLOR = 0xFF6BE39B;
    private static final int NO_COLOR = 0xFFE3676B;
    private static final int LINE_COLOR = 0x33FFFFFF;
    private static final int TEXT_COLOR = 0xFFC9C9D1;

    private static final float ROW_SCALE = 0.75F;

    private static final Minecraft MC = Minecraft.getInstance();

    private static final HudWidget WIDGET = new HudWidget("permission", "权限信息", BASE_W, BASE_H,
            MIN_SIZE, MAX_SIZE,
            (screenW, screenH) -> new float[]{
                    screenW - 6.0F - BASE_W,
                    screenH * 0.36F
            });

    static {
        DragManager.register(WIDGET);
    }

    private PermissionHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (MC.player == null || MC.options == null || MC.options.hideGui) {
            WIDGET.setVisible(false);
            return;
        }
        if (!ModConfigData.hudPermission()) {
            WIDGET.setVisible(false);
            return;
        }

        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();
        WIDGET.ensureInitialized(screenW, screenH);
        WIDGET.setVisible(true);

        GuiGraphics graphics = event.getGuiGraphics();
        draw(graphics, MC.font);

        DragManager.renderWidgetDebug(graphics, WIDGET, HudDraw.mouseX(MC), HudDraw.mouseY(MC));
    }

    private static void draw(GuiGraphics graphics, Font font) {
        boolean known = AccessClient.isKnown();
        boolean admin = AccessClient.isAdmin();
        boolean canChoose = AccessClient.canChoose();
        boolean explicit = AccessClient.isExplicit();
        boolean noCooldown = AccessClient.noCooldown();
        Operator operator = ModConfigData.operator();

        HudDraw.pushWidget(graphics, WIDGET);
        HudBlur.apply(graphics, 0.0F, 0.0F, BASE_W, BASE_H, 4.0F);
        graphics.fill(0, 0, BASE_W, BASE_H, BG_COLOR);
        graphics.fill(0, 0, 2, BASE_H, admin ? ADMIN_COLOR : PLAYER_COLOR);

        HudDraw.drawScaledString(graphics, font, "权限信息", 8.0F, 5.5F, 0.9F, TITLE_COLOR);

        String group = !known ? "同步中" : (admin ? "管理员" : "普通玩家");
        int groupColor = !known ? LABEL_COLOR : (admin ? ADMIN_COLOR : PLAYER_COLOR);
        row(graphics, font, "权限组", group, groupColor, 19.0F);

        String switchText = !known ? "同步中" : (canChoose ? "已授权" : "已锁定");
        int switchColor = !known ? LABEL_COLOR : (canChoose ? OK_COLOR : NO_COLOR);
        row(graphics, font, "干员切换", switchText, switchColor, 30.0F);

        String source;
        int sourceColor;
        if (!known) {
            source = "同步中";
            sourceColor = LABEL_COLOR;
        } else if (explicit) {
            source = "管理员设置";
            sourceColor = ADMIN_COLOR;
        } else if (admin) {
            source = "OP 权限";
            sourceColor = ADMIN_COLOR;
        } else {
            source = "服务器默认";
            sourceColor = PLAYER_COLOR;
        }
        row(graphics, font, "授权来源", source, sourceColor, 41.0F);

        row(graphics, font, "当前干员", Lang.operator(operator), TEXT_COLOR, 52.0F);
        row(graphics, font, "技能冷却",
                noCooldown ? "无冷却" : "有冷却",
                noCooldown ? OK_COLOR : TEXT_COLOR, 63.0F);

        graphics.fill(8, 75, BASE_W - 8, 76, LINE_COLOR);

        HudDraw.drawScaledString(graphics, font, "可用能力", 8.0F, 80.0F, 0.72F, LABEL_COLOR);
        float y = 91.0F;
        if (!known) {
            bullet(graphics, font, "等待服务器同步权限…", LABEL_COLOR, y);
        } else if (admin) {
            bullet(graphics, font, "自由切换干员 / 可用 /Admin 指令", OK_COLOR, y);
            bullet(graphics, font, noCooldown ? "技能无冷却：已开启" : "技能无冷却：可开启",
                    noCooldown ? OK_COLOR : TEXT_COLOR, y + 11.0F);
            bullet(graphics, font, "可进入「管理员」大类", OK_COLOR, y + 22.0F);
        } else if (canChoose) {
            bullet(graphics, font, "管理员已授权：可切换干员", OK_COLOR, y);
            bullet(graphics, font, "技能冷却常开（普通玩家不能关）", TEXT_COLOR, y + 11.0F);
            bullet(graphics, font, "管理员大类：权限不足", NO_COLOR, y + 22.0F);
        } else {
            bullet(graphics, font, "干员锁定，不能自己切换", NO_COLOR, y);
            bullet(graphics, font, "技能冷却常开（普通玩家不能关）", TEXT_COLOR, y + 11.0F);
            bullet(graphics, font, "管理员大类：权限不足", NO_COLOR, y + 22.0F);
        }

        HudDraw.popWidget(graphics);
    }

    private static void row(GuiGraphics graphics, Font font, String label, String value,
                            int valueColor, float y) {
        HudDraw.drawScaledString(graphics, font, label, 8.0F, y, ROW_SCALE, LABEL_COLOR);
        float width = ModFonts.width(value) * ROW_SCALE;
        HudDraw.drawScaledString(graphics, font, value, BASE_W - 8.0F - width, y, ROW_SCALE, valueColor);
    }

    private static void bullet(GuiGraphics graphics, Font font, String text, int color, float y) {
        HudDraw.drawScaledString(graphics, font, "·", 8.0F, y, ROW_SCALE, color);
        HudDraw.drawScaledString(graphics, font, text, 15.0F, y, 0.7F, color);
    }
}
