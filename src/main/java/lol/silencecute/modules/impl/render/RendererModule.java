package lol.silencecute.modules.impl.render;

import lol.silencecute.client.screen.BoolValue;
import lol.silencecute.client.screen.LabelValue;
import lol.silencecute.client.screen.ModeValue;
import lol.silencecute.client.screen.NumberValue;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "渲染器设置",
        description = "Naven 风格背景模糊与着色器参数",
        category = Category.RENDER
)
public class RendererModule extends Module {

    public static RendererModule INSTANCE;

    public RendererModule() {
        super("渲染器设置", "Naven 风格背景模糊与着色器参数", Category.RENDER);
        INSTANCE = this;
        initModule();

        this.toggleable = true;
        this.state = ModConfigData.rendererEnabled();
        this.whenToggled(() -> ModConfigData.setRendererEnabled(this.state));

        addValue(new BoolValue("背景模糊",
                ModConfigData::navenBlur,
                ModConfigData::setNavenBlur));
        addValue(new NumberValue("模糊帧率",
                ModConfigData::navenBlurFps,
                ModConfigData::setNavenBlurFps,
                15.0f, 120.0f, 1.0f));
        addValue(new NumberValue("模糊力度",
                ModConfigData::navenBlurStrength,
                ModConfigData::setNavenBlurStrength,
                0.0f, 19.0f, 1.0f));

        addValue(new LabelValue("兼容性设置"));
        addValue(new ModeValue("改变动作时对YSM的兼容性",
                ModConfigData::yssmCompatMode,
                mode -> {
                    if ("YSM蹲姿".equals(mode) && !lol.silencecute.client.compat.YsmCompat.isYsmLoaded()) {
                        lol.silencecute.client.render.hud.notification.NotificationRea1.warning("未检测到YSM!");
                        return;
                    }
                    ModConfigData.setYssmCompatMode(mode);
                },
                "原版翻滚", "YSM蹲姿"));
    }
}
