package lol.silencecute.modules.impl.render;

import lol.silencecute.client.screen.ModeValue;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "玩家血量显示",
        description = "HUD 上的头像与血条",
        category = Category.RENDER
)
public class HudHealthModule extends Module {

    public static HudHealthModule INSTANCE;

    public HudHealthModule() {
        super("玩家血量显示", "HUD 上的头像与血条", Category.RENDER);
        INSTANCE = this;
        initModule();

        this.toggleable = true;
        this.state = ModConfigData.hudHealth();
        this.whenToggled(() -> ModConfigData.setHudHealth(this.state));

        addValue(new ModeValue("血量上限",
                () -> displayName(ModConfigData.healthDisplayMode()),
                value -> ModConfigData.setHealthDisplayMode(parseMode(value)),
                "始终100", "真上限"));
    }

    private static String displayName(String mode) {
        return "ALWAYS_100".equalsIgnoreCase(mode) ? "始终100" : "真上限";
    }

    private static String parseMode(String value) {
        return "始终100".equals(value) ? "ALWAYS_100" : "TRUE_MAX";
    }
}
