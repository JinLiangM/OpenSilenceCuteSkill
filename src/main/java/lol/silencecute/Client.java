package lol.silencecute;

import lol.silencecute.client.compat.TaczGrenadeCompat;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.config.SkillConfigData;
import lol.silencecute.access.AccessService;
import lol.silencecute.command.AdminCommands;
import lol.silencecute.event.LunaPassiveService;
import lol.silencecute.event.OperatorState;
import lol.silencecute.event.PassiveService;
import lol.silencecute.event.TempstPassiveService;
import lol.silencecute.event.TempstRollSizeHandler;
import lol.silencecute.manager.TempstRollService;
import lol.silencecute.manager.*;
import lol.silencecute.registry.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import static net.minecraftforge.fml.ModLoadingContext.*;

@Mod(Client.MOD_ID)
public final class Client {
    public static final String MOD_ID = "silencecute_skill";

    public Client() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        get().registerConfig(ModConfig.Type.CLIENT, ModConfigData.SPEC,
                "operator_skills-client.toml");
        get().registerConfig(ModConfig.Type.SERVER, SkillConfigData.SPEC,
                "operator_skills-server.toml");
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModNetwork.register();
        TaczGrenadeCompat.initialize();
        MinecraftForge.EVENT_BUS.register(DashService.class);
        MinecraftForge.EVENT_BUS.register(PassiveService.class);
        MinecraftForge.EVENT_BUS.register(TempstPassiveService.class);
        MinecraftForge.EVENT_BUS.register(TempstRollService.class);
        MinecraftForge.EVENT_BUS.register(TempstUltimateService.class);
        MinecraftForge.EVENT_BUS.register(TempstRollSizeHandler.class);
        MinecraftForge.EVENT_BUS.register(LunaPassiveService.class);
        MinecraftForge.EVENT_BUS.register(LunaGrenadeService.class);
        MinecraftForge.EVENT_BUS.register(LunaShockArrowService.class);
        MinecraftForge.EVENT_BUS.register(LunaReconArrowService.class);
        MinecraftForge.EVENT_BUS.register(MagneticBombService.class);
        MinecraftForge.EVENT_BUS.register(AdminCommands.class);
        MinecraftForge.EVENT_BUS.register(AccessService.class);
        MinecraftForge.EVENT_BUS.register(OperatorState.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(
                    lol.silencecute.client.render.hud.notification.NotificationRea1.class);
        }
    }
}