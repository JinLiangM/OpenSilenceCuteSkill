package lol.silencecute.modules.impl.render;

import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "玩家被动显示",
        description = "HUD 上的干员被动栏",
        category = Category.RENDER
)
public class HudPassiveModule extends Module {

    public static HudPassiveModule INSTANCE;

    public HudPassiveModule() {
        super("玩家被动显示", "HUD 上的干员被动栏", Category.RENDER);
        INSTANCE = this;
        initModule();

        this.toggleable = true;
        this.state = ModConfigData.hudPassive();
        this.whenToggled(() -> ModConfigData.setHudPassive(this.state));
    }
}
