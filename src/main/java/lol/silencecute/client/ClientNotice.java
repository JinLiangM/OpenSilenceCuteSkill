package lol.silencecute.client;

import lol.silencecute.client.render.hud.notification.NotificationRea1;
import lol.silencecute.config.ModConfigData;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientNotice {

    private ClientNotice() {
    }

    public static void warn(String text) {
        if (text == null || text.isEmpty()) return;

        boolean popup;
        try {
            popup = ModConfigData.notificationRea1();
        } catch (Exception e) {
            popup = true;
        }

        if (popup) {
            NotificationRea1.warning(text);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("§c" + text), false);
        }
    }
}
