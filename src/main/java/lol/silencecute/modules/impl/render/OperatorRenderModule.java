package lol.silencecute.modules.impl.render;

import lol.silencecute.client.lang.Lang;
import lol.silencecute.client.render.hud.notification.NotificationRea1;
import lol.silencecute.client.screen.BoolValue;
import lol.silencecute.client.screen.ColorValue;
import lol.silencecute.client.screen.LabelValue;
import lol.silencecute.client.screen.ModeValue;
import lol.silencecute.client.screen.NumberValue;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "干员技能设置",
        description = "公共 / 露娜 / 威龙 技能视觉与语音设置",
        category = Category.RENDER
)
public class OperatorRenderModule extends Module {

    public static OperatorRenderModule INSTANCE;

    public OperatorRenderModule() {
        super("干员技能设置", "公共 / 露娜 / 威龙 技能视觉与语音设置", Category.RENDER);
        INSTANCE = this;
        initModule();

        this.toggleable = true;
        this.state = ModConfigData.operatorRenderEnabled();
        this.whenToggled(() -> ModConfigData.setOperatorRenderEnabled(this.state));

        addValue(new LabelValue("公共"));
        addValue(new ModeValue("干员语音设置",
                ModConfigData::operatorVoiceLang,
                OperatorRenderModule::selectVoiceLanguage,
                "中文", "英语"));

        addValue(new LabelValue("威龙"));

        addValue(new BoolValue("暂无",
                ModConfigData::weilongPlaceholder,
                ModConfigData::setWeilongPlaceholder).navenOnly());

        addValue(new LabelValue("露娜"));
        addValue(new BoolValue("显示抛物线",
                ModConfigData.SHOW_TRAJECTORY::get, ModConfigData.SHOW_TRAJECTORY::set)
                .modern("显示手雷抛物线"));
        addValue(new BoolValue("终点六边形绘制",
                ModConfigData.SHOW_HEXAGON::get, ModConfigData.SHOW_HEXAGON::set)
                .modern("显示手雷落点"));
        addValue(new BoolValue("引信时FOV不变",
                ModConfigData.GRENADE_FOV_EFFECT::get, ModConfigData.GRENADE_FOV_EFFECT::set));
        addValue(new BoolValue("显示定位准心",
                ModConfigData.LUNA_CROSSHAIR::get, ModConfigData.LUNA_CROSSHAIR::set)
                .modern("显示电箭准心"));
        addValue(new BoolValue("显示范围",
                ModConfigData.SHOW_SHOCK_RADIUS::get, ModConfigData.SHOW_SHOCK_RADIUS::set)
                .modern("显示电箭范围"));
        addValue(new BoolValue("跟随队伍颜色",
                ModConfigData.CHAMS_TEAM_COLOR::get, ModConfigData.CHAMS_TEAM_COLOR::set)
                .modern("露娜显形跟随队伍颜色"));
        addValue(new BoolValue("显示红标",
                ModConfigData.CHAMS_MARKER::get, ModConfigData.CHAMS_MARKER::set)
                .modern("露娜显形显示红标"));
        addValue(new NumberValue("显形透明度",
                () -> (float) ModConfigData.CHAMS_ALPHA.get(),
                value -> ModConfigData.CHAMS_ALPHA.set(value.intValue()),
                0.0F, 255.0F, 1.0F).modern("露娜显形透明度"));
        addValue(new ColorValue("显形颜色",
                ModConfigData::chamsColor,
                rgb -> {
                    ModConfigData.CHAMS_RED.set((rgb >> 16) & 0xFF);
                    ModConfigData.CHAMS_GREEN.set((rgb >> 8) & 0xFF);
                    ModConfigData.CHAMS_BLUE.set(rgb & 0xFF);
                }).modern("露娜显形颜色"));

        addValue(new LabelValue("疾风"));
        addValue(new BoolValue("暂无",
                ModConfigData::jifengPlaceholder,
                ModConfigData::setJifengPlaceholder).navenOnly());
    }

    private static void selectVoiceLanguage(String mode) {
        if ("中文".equals(mode)) {
            NotificationRea1.warning(Lang.tr("notif.voice.wip"));
            return;
        }
        ModConfigData.setOperatorVoiceLanguage(mode);
    }
}
