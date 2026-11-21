package lol.silencecute.client;

import lol.silencecute.Client;
import lol.silencecute.client.render.hud.LunaDamageTint;
import lol.silencecute.client.render.hud.LunaExposureHud;
import lol.silencecute.client.render.hud.LunaScanWarningHud;
import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import lol.silencecute.registry.ModSounds;

import net.minecraft.client.Minecraft;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import net.minecraft.sounds.SoundSource;

import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import net.minecraftforge.event.TickEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)

public final class LunaRevealClient {

    private static final Map<Integer, Long> REVEALED = new HashMap<>();

    private static net.minecraft.client.multiplayer.ClientLevel revealLevel;

    private static long lastHitSoundTime = 0L;

    private LunaRevealClient() { }

    private static boolean checkLevel() {

        var level = Minecraft.getInstance().level;

        if (level != revealLevel) {

            REVEALED.clear();

            revealLevel = level;

        }

        return level != null;

    }

    public static void reveal(int entityId, int durationTicks) {

        if (entityId == -1) {

            LunaExposureHud.flash(durationTicks > 0 ? durationTicks * 50L : 500L);

            LunaDamageTint.trigger();

            long now = System.currentTimeMillis();

            if (now - lastHitSoundTime > 1500L) {

                lastHitSoundTime = now;

                Minecraft mc = Minecraft.getInstance();

                if (mc.player != null && mc.level != null) {

                    mc.getSoundManager().play(new SimpleSoundInstance(

                            ModSounds.LUNA_PASSIVE_HIT.get(),

                            SoundSource.PLAYERS, 1.0F, 1.0F, mc.level.random,

                            mc.player.getX(), mc.player.getY(), mc.player.getZ()

                    ));

                }

            }

            return;

        }

        if (entityId == -2) {

            LunaScanWarningHud.flash();

            return;

        }

        if (!checkLevel() || durationTicks <= 0) return;

        long expiresAt = revealLevel.getGameTime() + durationTicks;

        REVEALED.merge(entityId, expiresAt, Math::max);

    }

    public static List<Integer> getActiveRevealedEntityIds() {

        List<Integer> result = new ArrayList<>();

        if (!checkLevel()) return result;

        long now = revealLevel.getGameTime();

        REVEALED.forEach((id, expiresAt) -> {

            if (now < expiresAt) result.add(id);

        });

        return result;

    }

    @SubscribeEvent

    public static void onClientTick(TickEvent.ClientTickEvent event) {

        if (event.phase != TickEvent.Phase.END || !checkLevel()) return;

        long now = revealLevel.getGameTime();

        REVEALED.entrySet().removeIf(entry -> now >= entry.getValue());

    }

    @SubscribeEvent

    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {

        REVEALED.clear();

        revealLevel = null;

        lastHitSoundTime = 0L;

    }

}

