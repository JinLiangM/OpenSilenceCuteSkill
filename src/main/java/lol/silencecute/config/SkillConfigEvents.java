package lol.silencecute.config;

import lol.silencecute.Client;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SkillConfigEvents {

    private SkillConfigEvents() {
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SkillConfigData.SPEC) {
            SkillConfigData.applyAll();
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SkillConfigData.SPEC) {
            SkillConfigData.applyAll();
        }
    }
}
