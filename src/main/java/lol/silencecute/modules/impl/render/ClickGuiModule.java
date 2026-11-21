package lol.silencecute.modules.impl.render;

import lol.silencecute.api.GuiTheme;
import lol.silencecute.client.screen.ColorValue;
import lol.silencecute.client.screen.ModeValue;
import lol.silencecute.client.screen.NumberValue;
import lol.silencecute.client.screen.fatality.GuiTransition;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "ClickGui",
        description = "ClickGui 的风格、配色与背景",
        category = Category.RENDER
)
public class ClickGuiModule extends Module {

    public static ClickGuiModule INSTANCE;

    public ClickGuiModule() {
        super("ClickGui", "ClickGui 的风格、配色与背景", Category.RENDER);
        INSTANCE = this;
        initModule();

        addValue(new ModeValue("风格",
                () -> ModConfigData.guiStyle(),
                value -> {
                    try {

                        if (ModConfigData.guiStyle().equalsIgnoreCase(value)) return;

                        boolean toNaven = "NAVEN".equalsIgnoreCase(value);
                        int sourceDefault = toNaven ? 0xFF615A : 0x3662EC;
                        int targetDefault = toNaven ? 0x3662EC : 0xFF615A;
                        ModConfigData.setGuiStyle(value);

                        GuiTransition.request(value, () -> {
                            try {
                                if (ModConfigData.guiAccent() == sourceDefault) {
                                    ModConfigData.setGuiAccent(targetDefault);
                                }
                            } catch (Exception ignored) {

                            }
                        });
                    } catch (Exception ignored) {

                    }
                }, "Modern", "Naven"));

        addValue(new ModeValue("模式",
                () -> {
                    try {
                        return ModConfigData.guiTheme().displayName;
                    } catch (Exception e) {
                        return GuiTheme.DARK.displayName;
                    }
                },
                value -> {
                    try {
                        ModConfigData.GUI_THEME.set(GuiTheme.byDisplayName(value));
                        ModConfigData.SPEC.save();
                    } catch (Exception ignored) {

                    }
                },
                GuiTheme.DARK.displayName, GuiTheme.LIGHT.displayName));

        addValue(new ColorValue("GUI颜色",
                () -> {
                    try {
                        return ModConfigData.guiAccent();
                    } catch (Exception e) {
                        return 0xFF615A;
                    }
                },
                rgb -> {
                    try {
                        ModConfigData.setGuiAccent(rgb);
                    } catch (Exception ignored) {

                    }
                }));

        addValue(new ModeValue("语言",
                () -> ModConfigData.guiChinese() ? "简体中文" : "English",
                value -> {
                    try {
                        ModConfigData.GUI_CHINESE.set("简体中文".equals(value));
                        ModConfigData.SPEC.save();
                    } catch (Exception ignored) {

                    }
                },
                "English", "简体中文"));

        addValue(new ColorValue("背景颜色",
                ModConfigData::navenBgColor,
                ModConfigData::setNavenBgColor));

        addValue(new NumberValue("背景不透明度",
                ModConfigData::navenBgAlpha,
                ModConfigData::setNavenBgAlpha,
                0.0f, 1.0f, 0.05f));
    }
}
