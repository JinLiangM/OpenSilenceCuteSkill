package lol.silencecute.modules.impl.admin;

import java.util.function.Supplier;

import lol.silencecute.client.AccessClient;
import lol.silencecute.client.render.hud.notification.NotificationRea1;
import lol.silencecute.client.screen.ModeValue;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "当前权限组",
        description = "只读显示自己的权限组、干员切换授权与当前干员",
        category = Category.ADMIN
)
public class AdminAccessModule extends Module {

    private static final String ADMIN = "管理员";
    private static final String PLAYER = "普通玩家";
    private static final String GRANTED = "已授权";
    private static final String LOCKED = "已锁定";

    public AdminAccessModule() {
        super("当前权限组", "只读显示自己的权限组、干员切换授权与当前干员", Category.ADMIN);
        initModule();

        this.toggleable = false;
        this.enableRow = false;
        this.state = true;

        addValue(readOnly("权限组",
                () -> AccessClient.isAdmin() ? ADMIN : PLAYER,
                ADMIN, PLAYER));

        addValue(readOnly("干员切换",
                () -> AccessClient.canChoose() ? GRANTED : LOCKED,
                GRANTED, LOCKED));

        addValue(readOnly("当前干员",
                () -> ModConfigData.operator().name(),
                "LUNA", "WEILONG", "JIFENG"));
    }

    private static ModeValue readOnly(String label, Supplier<String> getter, String... modes) {
        ModeValue value = new ModeValue(label, getter, ignored -> {
        }, modes);
        value.locked = () -> true;
        value.onLockedClick = () -> NotificationRea1.warning(label + "由服务器分配，不能在这里修改");
        return value;
    }
}
