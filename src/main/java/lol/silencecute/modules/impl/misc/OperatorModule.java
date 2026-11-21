package lol.silencecute.modules.impl.misc;

import com.mojang.blaze3d.platform.InputConstants;

import lol.silencecute.api.Operator;
import lol.silencecute.client.AccessClient;
import lol.silencecute.client.ClientEvents;
import lol.silencecute.client.lang.Lang;
import lol.silencecute.client.render.hud.notification.NotificationRea1;
import lol.silencecute.client.screen.KeyValue;
import lol.silencecute.client.screen.ModeValue;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

@ModuleInfo(
        name = "干员",
        description = "干员技能总开关（常开，无法关闭）",
        category = Category.MISC
)
public class OperatorModule extends Module {

    public static OperatorModule INSTANCE;

    public OperatorModule() {
        super("干员", "干员技能总开关（常开，无法关闭）", Category.MISC);
        INSTANCE = this;
        initModule();

        this.toggleable = false;
        this.enableRow = false;
        this.state = true;

        ModeValue operatorValue = new ModeValue("当前干员",
                () -> ModConfigData.operator().name(),
                value -> select(Operator.byInput(value)),
                "LUNA", "WEILONG", "JIFENG");
        operatorValue.locked = AccessClient::isLocked;
        operatorValue.onLockedClick = () -> NotificationRea1.error("鉴权未通过！");
        addValue(operatorValue);

        addKey("大招", ClientEvents.ULTIMATE_KEY);
        addKey("一技能", ClientEvents.DASH_KEY);
        addKey("二技能", ClientEvents.BOMB_KEY);
    }

    private static void select(Operator operator) {
        if (operator == null || operator == Operator.NONE) return;
        if (!AccessClient.requestOperator(operator)) return;
        NotificationRea1.info(Lang.tr("notif.operator.changed") + Lang.operator(operator));
    }

    private void addKey(String label, KeyMapping mapping) {
        KeyValue value = new KeyValue(label,
                () -> readKey(mapping),
                code -> bindKey(mapping, code));
        value.availableWhen(() -> ModConfigData.operator() != Operator.NONE);
        addValue(value);
    }

    private static int readKey(KeyMapping mapping) {
        try {
            return KeyValue.encode(mapping.getKey());
        } catch (Exception e) {
            return 0;
        }
    }

    private static void bindKey(KeyMapping mapping, int code) {
        try {
            if (code <= 0) {
                mapping.setKey(InputConstants.UNKNOWN);
            } else if (KeyValue.isMouse(code)) {
                mapping.setKey(InputConstants.Type.MOUSE.getOrCreate(KeyValue.mouseButton(code)));
            } else {
                mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(code));
            }
            KeyMapping.resetMapping();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.options != null) minecraft.options.save();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void setState(boolean newState) {
        if (!newState) {
            NotificationRea1.error("鉴权未通过！");
            if (!this.state) super.setState(true);
            return;
        }
        super.setState(true);
    }

    public void enforceAlwaysOn() {
        if (!this.state) super.setState(true);
    }
}
