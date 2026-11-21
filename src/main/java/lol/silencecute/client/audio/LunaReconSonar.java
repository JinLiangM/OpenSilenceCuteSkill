package lol.silencecute.client.audio;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import lol.silencecute.Client;
import lol.silencecute.entity.LunaReconArrowEntity;
import lol.silencecute.registry.ModSounds;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class LunaReconSonar {
    private static final Map<Integer, Sound> ACTIVE = new HashMap<>();

    private LunaReconSonar() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { stopAll(mc); return; }

        Map<Integer, LunaReconArrowEntity> flying = new HashMap<>();
        for (LunaReconArrowEntity arrow : mc.level.getEntitiesOfClass(
                LunaReconArrowEntity.class, mc.player.getBoundingBox().inflate(64.0D))) {
            if (!arrow.isLanded() && !arrow.isRemoved()) flying.put(arrow.getId(), arrow);
        }
        ACTIVE.entrySet().removeIf(e -> {
            if (!flying.containsKey(e.getKey())) { mc.getSoundManager().stop(e.getValue()); return true; }
            return false;
        });
        for (var entry : flying.entrySet()) {
            if (!ACTIVE.containsKey(entry.getKey())) {
                Sound sound = new Sound(entry.getValue());
                ACTIVE.put(entry.getKey(), sound);
                mc.getSoundManager().play(sound);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stopAll(Minecraft.getInstance());
    }

    private static void stopAll(Minecraft mc) {
        if (mc.getSoundManager() != null) {
            for (Sound s : ACTIVE.values()) mc.getSoundManager().stop(s);
        }
        ACTIVE.clear();
    }

    private static final class Sound extends AbstractTickableSoundInstance {
        private static final double MAX_DISTANCE = 64.0D;
        private final LunaReconArrowEntity arrow;

        Sound(LunaReconArrowEntity arrow) {
            super(ModSounds.LUNA_BIG_SOUND.get(), SoundSource.MASTER, RandomSource.create());
            this.arrow = arrow;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.x = arrow.getX();
            this.y = arrow.getY();
            this.z = arrow.getZ();
        }

        @Override
        public void tick() {
            if (arrow.isRemoved() || arrow.isLanded()) { stop(); return; }
            this.x = arrow.getX();
            this.y = arrow.getY();
            this.z = arrow.getZ();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) { this.volume = 0.0F; return; }
            double d = mc.player.position().distanceTo(arrow.position());
            if (d >= MAX_DISTANCE) { this.volume = 0.0F; return; }
            this.volume = Math.min(1.0F, (float) (1.0D - d / MAX_DISTANCE) * 1.5F);
        }
    }
}