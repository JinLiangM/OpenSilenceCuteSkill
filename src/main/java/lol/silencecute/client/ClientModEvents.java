package lol.silencecute.client;

import lol.silencecute.Client;
import lol.silencecute.client.compat.TaczGrenadeCompat;
import lol.silencecute.client.render.entity.LunaGrenadeRenderer;
import lol.silencecute.client.render.entity.LunaReconArrowRenderer;
import lol.silencecute.client.render.entity.LunaShockArrowRenderer;
import lol.silencecute.client.render.entity.MagneticBombRenderer;
import lol.silencecute.client.render.item.LunaElectricArrowModel;
import lol.silencecute.client.render.item.LunaGrenadeModel;
import lol.silencecute.registry.ModEntities;

import lol.silencecute.registry.ModItems;

import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.client.event.EntityRenderersEvent;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)

public final class ClientModEvents {

    private ClientModEvents() {

    }

    

    @SubscribeEvent

    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {

        event.enqueueWork(() -> {

            TaczGrenadeCompat.setClientBlocker(player ->

                    player == net.minecraft.client.Minecraft.getInstance().player

                            && LunaGrenadeClient.blocksWeaponInput());

            net.minecraft.client.renderer.item.ItemProperties.register(ModItems.LUNA_COMPOUND_BOW.get(),

                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Client.MOD_ID, "animated"),

                    (stack, level, entity, seed) -> stack.hasTag()

                            && stack.getTag().getBoolean("LunaAnimated") ? 1.0F : 0.0F);

            net.minecraft.client.Minecraft.getInstance().execute(
                    lol.silencecute.client.font.ModFonts::warmup);

        });

    }

    @SubscribeEvent

    public static void onRegisterModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {

        event.register(LunaElectricArrowModel.MODEL);

    }

    @SubscribeEvent

    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {

        event.register(ClientEvents.ULTIMATE_KEY);

        event.register(ClientEvents.DASH_KEY);
        event.register(ClientEvents.BOMB_KEY);
    }

    @SubscribeEvent

    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(ModEntities.MAGNETIC_BOMB.get(), MagneticBombRenderer::new);

        event.registerEntityRenderer(ModEntities.LUNA_GRENADE.get(), LunaGrenadeRenderer::new);

        event.registerEntityRenderer(ModEntities.LUNA_SHOCK_ARROW.get(), LunaShockArrowRenderer::new);

        event.registerEntityRenderer(ModEntities.LUNA_RECON_ARROW.get(), LunaReconArrowRenderer::new);

    }

    @SubscribeEvent

    public static void onReloadListeners(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {

        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)

                manager -> {

                    LunaGrenadeModel.invalidate();

                    LunaReconArrowRenderer.clearVisual();

                });

    }

}

