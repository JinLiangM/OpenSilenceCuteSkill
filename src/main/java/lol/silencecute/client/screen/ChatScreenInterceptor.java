package lol.silencecute.client.screen;

import lol.silencecute.Client;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChatScreenInterceptor {

    private ChatScreenInterceptor() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        try {
            if (event.getNewScreen() == null) return;
            if (!(event.getNewScreen() instanceof ChatScreen)) return;
            if (!ModConfigData.hudChat()) return;
            event.setNewScreen(new SilenceChatScreen(""));
        } catch (Throwable ignored) {

        }
    }
}
