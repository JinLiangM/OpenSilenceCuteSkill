package lol.silencecute.client.audio;

import lol.silencecute.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class DashSound {
    private DashSound() {
    }

    
    public static void playForLocalPlayer(Minecraft mc, double x, double y, double z) {
        SoundEvent event = ModSounds.WEILONG_DASH.isPresent()
                ? ModSounds.WEILONG_DASH.get()
                : SoundEvents.ENDER_DRAGON_SHOOT;
        mc.getSoundManager().play(new SimpleSoundInstance(event, SoundSource.MASTER, 1.0F, 1.0F,
                RandomSource.create(), x, y, z));
    }
}
