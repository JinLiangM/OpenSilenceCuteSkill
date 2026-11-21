package lol.silencecute.modules.impl.render;

import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "信息",
        description = "右下角通知提示",
        category = Category.RENDER
)
public class NotificationModule extends Module {

    public static NotificationModule INSTANCE;

    public NotificationModule() {
        super("信息", "右下角通知提示", Category.RENDER);
        INSTANCE = this;
        initModule();

        this.toggleable = true;
        this.state = ModConfigData.notificationRea1();
        this.whenToggled(() -> ModConfigData.setNotificationRea1(this.state));
    }
}
