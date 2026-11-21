package lol.silencecute.component;

import lol.silencecute.Client;
import lol.silencecute.client.render.hud.notifications.NotificationManager;
import lol.silencecute.client.render.hud.notifications.NotificationType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class SB {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {

        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_5) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS) {
        }
    }
}