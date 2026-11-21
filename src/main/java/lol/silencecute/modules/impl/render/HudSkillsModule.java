package lol.silencecute.modules.impl.render;

import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "玩家技能显示",
        description = "HUD 上的技能栏",
        category = Category.RENDER
)
public class HudSkillsModule extends Module {

    public static HudSkillsModule INSTANCE;

    public HudSkillsModule() {
        super("玩家技能显示", "HUD 上的技能栏", Category.RENDER);
        INSTANCE = this;
        initModule();

        this.toggleable = true;
        this.state = ModConfigData.hudSkills();
        this.whenToggled(() -> ModConfigData.setHudSkills(this.state));
    }
}
