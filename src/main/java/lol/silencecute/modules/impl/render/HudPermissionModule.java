package lol.silencecute.modules.impl.render;

import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "权限显示",
        description = "HUD 右侧实时显示自己的权限组与可用操作",
        category = Category.RENDER
)
public class HudPermissionModule extends Module {

    public static HudPermissionModule INSTANCE;

    public HudPermissionModule() {
        super("权限显示", "HUD 右侧实时显示自己的权限组与可用操作", Category.RENDER);
        INSTANCE = this;
        initModule();

        this.toggleable = true;
        this.state = ModConfigData.hudPermission();
        this.whenToggled(() -> ModConfigData.setHudPermission(this.state));
    }
}
