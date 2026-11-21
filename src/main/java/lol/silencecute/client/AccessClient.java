package lol.silencecute.client;

import lol.silencecute.api.Operator;
import lol.silencecute.client.render.hud.notification.NotificationRea1;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.modules.impl.misc.OperatorModule;
import lol.silencecute.network.AccessRequestPacket;
import lol.silencecute.registry.ModNetwork;
import net.minecraft.client.Minecraft;

public final class AccessClient {

    private static boolean known;

    private static boolean admin;

    private static boolean canChoose;

    private static boolean explicit;

    private static boolean noCooldown;

    private static Operator operator = Operator.LUNA;

    private AccessClient() {
    }

    public static boolean isKnown() {
        return known;
    }

    public static boolean isAdmin() {
        return known && admin;
    }

    public static boolean canChoose() {
        return !known || canChoose;
    }

    public static boolean isLocked() {
        return known && !canChoose;
    }

    public static boolean isExplicit() {
        return known && explicit;
    }

    public static boolean noCooldown() {
        return known && noCooldown;
    }

    public static Operator operator() {
        return operator;
    }

    public static void apply(Operator newOperator, boolean canChooseNow, boolean adminNow,
                             boolean explicitNow, boolean noCooldownNow) {
        boolean wasKnown = known;
        boolean prevCanChoose = canChoose;
        boolean prevNoCooldown = noCooldown;
        boolean prevAdmin = admin;

        known = true;
        canChoose = canChooseNow;
        admin = adminNow;
        explicit = explicitNow;
        noCooldown = noCooldownNow;
        operator = newOperator == null || newOperator == Operator.NONE
                ? Operator.LUNA : newOperator;

        ModConfigData.selectOperatorLocally(operator);
        ModConfigData.setOperatorEnabled(true);
        if (OperatorModule.INSTANCE != null) {
            OperatorModule.INSTANCE.enforceAlwaysOn();
        }

        if (!wasKnown) return;

        if (prevCanChoose && !canChoose) {
            NotificationRea1.warning("管理员已锁定你的干员切换");
        } else if (!prevCanChoose && canChoose && explicit) {
            NotificationRea1.success("管理员已授权你自定义干员");
        }
        if (prevNoCooldown && !noCooldown) {
            NotificationRea1.warning("技能冷却已恢复（普通玩家不能无冷却）");
        }
        if (!prevAdmin && admin) {
            NotificationRea1.success("你已成为管理员");
        }
    }

    public static void reset() {
        known = false;
        admin = false;
        canChoose = false;
        explicit = false;
        noCooldown = false;
        operator = Operator.LUNA;
    }


    public static void query() {
        send(new AccessRequestPacket(AccessRequestPacket.QUERY));
    }

    public static boolean requestOperator(Operator target) {
        if (target == null || target == Operator.NONE) return false;
        if (isLocked()) {
            NotificationRea1.error("鉴权未通过！");
            return false;
        }
        ModConfigData.selectOperatorLocally(target);
        operator = target;
        send(AccessRequestPacket.select(target));
        return true;
    }

    public static void requestNoCooldown(boolean enabled) {
        if (enabled && !isAdmin()) {
            NotificationRea1.error("鉴权未通过！");
            return;
        }
        noCooldown = enabled;
        send(AccessRequestPacket.noCooldown(enabled));
    }

    private static void send(AccessRequestPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getConnection() == null) return;
        try {
            ModNetwork.CHANNEL.sendToServer(packet);
        } catch (Exception ignored) {
        }
    }
}
