package lol.silencecute.modules.impl.admin;

import lol.silencecute.client.AccessClient;
import lol.silencecute.client.render.hud.notification.NotificationRea1;
import lol.silencecute.client.screen.ModeValue;
import lol.silencecute.modules.Category;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleInfo;

@ModuleInfo(
        name = "干员技能冷却",
        description = "统一开关所有干员技能的冷却（仅管理员）",
        category = Category.ADMIN
)
public class AdminCooldownModule extends Module {

    public static final String NO_COOLDOWN = "无冷却";

    public static final String HAS_COOLDOWN = "有冷却";

    public AdminCooldownModule() {
        super("干员技能冷却", "统一开关所有干员技能的冷却（仅管理员）", Category.ADMIN);
        initModule();

        this.toggleable = false;
        this.enableRow = false;
        this.state = true;

        ModeValue cooldown = new ModeValue("冷却状态",
                () -> AccessClient.noCooldown() ? NO_COOLDOWN : HAS_COOLDOWN,
                value -> AccessClient.requestNoCooldown(NO_COOLDOWN.equals(value)),
                NO_COOLDOWN, HAS_COOLDOWN);
        cooldown.locked = () -> !AccessClient.isAdmin();
        cooldown.onLockedClick = () -> NotificationRea1.error("鉴权未通过！");
        addValue(cooldown);
    }
}
