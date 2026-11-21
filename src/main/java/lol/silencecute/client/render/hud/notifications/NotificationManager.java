package lol.silencecute.client.render.hud.notifications;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import lol.silencecute.Client;
import lol.silencecute.config.ModConfigData;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class NotificationManager {
    private static Notification active = null;

    private NotificationManager() {}

    public static void post(NotificationType type) {
        post(type, type.getDefaultContent(), -1L);
    }

    public static void post(NotificationType type, String content) {
        post(type, content, -1L);
    }

    public static void post(NotificationType type, String content, long durationMs) {
        if (active != null && active.isPlaying()) {
            return;
        }
        String c = (content == null || content.isEmpty()) ? type.getDefaultContent() : content;
        long d = durationMs > 0 ? durationMs : type.getDefaultDuration();
        if (type == NotificationType.WARNING) {
            d = Math.max(WARNING_MIN_DURATION, d);
        }
        active = new Notification(type, c, d);
    }

    private static final long WARNING_MIN_DURATION = 750L;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ModConfigData.notificationEnabled()) return;
        if (active == null) return;
        if (!active.isPlaying()) { active = null; return; }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        GuiGraphics g = event.getGuiGraphics();
        active.render(g, event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight());
    }
}