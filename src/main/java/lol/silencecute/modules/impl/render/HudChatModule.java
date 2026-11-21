package lol.silencecute.modules.impl.render;

import lol.silencecute.client.screen.NumberValue;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "聊天框",
        description = "可自由移动缩放的聊天显示",
        category = Category.RENDER
)
public class HudChatModule extends Module {

    public static HudChatModule INSTANCE;

    public HudChatModule() {
        super("聊天框", "可自由移动缩放的聊天显示", Category.RENDER);
        INSTANCE = this;
        initModule();

        this.toggleable = true;
        this.state = ModConfigData.hudChat();
        this.whenToggled(() -> ModConfigData.setHudChat(this.state));

        addValue(new NumberValue("最多显示",
                () -> (float) ModConfigData.hudChatMaxMessages(),
                value -> ModConfigData.setHudChatMaxMessages(value.intValue()),
                5.0F, 15.0F, 1.0F));

        addValue(new NumberValue("消失时间",
                () -> (float) ModConfigData.hudChatFadeTime(),
                value -> ModConfigData.setHudChatFadeTime(value.intValue()),
                1.0F, 60.0F, 1.0F));
    }
}
