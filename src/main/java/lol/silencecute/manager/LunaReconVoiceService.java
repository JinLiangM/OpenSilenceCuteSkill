package lol.silencecute.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lol.silencecute.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class LunaReconVoiceService {
    private static final RandomSource RANDOM = RandomSource.create();

    
    private static final float SHOOT_VOICE_CHANCE = 0.1F;
    private static final int SHOOT_VOICE_COOLDOWN_TICKS = 40 * 20;

    
    private static final int VOICE_BUSY_TICKS = 80;

    private static final Map<UUID, Long> NEXT_SHOOT_VOICE = new HashMap<>();
    
    private static final Map<UUID, Long> VOICE_BUSY_UNTIL = new HashMap<>();

    private static final List<SoundEvent> SHOOT_VOICES = List.of(
            ModSounds.LUNA_RECON_SHOOT_1.get(), ModSounds.LUNA_RECON_SHOOT_2.get(),
            ModSounds.LUNA_RECON_SHOOT_3.get(), ModSounds.LUNA_RECON_SHOOT_4.get(),
            ModSounds.LUNA_RECON_SHOOT_5.get());

    private static final List<SoundEvent> ARMOR_VOICES = List.of(
            ModSounds.LUNA_RECON_ARMOR_1.get(), ModSounds.LUNA_RECON_ARMOR_2.get(),
            ModSounds.LUNA_RECON_ARMOR_3.get());

    private static final List<SoundEvent> NORMAL_VOICES = List.of(
            ModSounds.LUNA_RECON_FOUND_1.get(), ModSounds.LUNA_RECON_FOUND_2.get(),
            ModSounds.LUNA_RECON_FOUND_3.get(), ModSounds.LUNA_RECON_FOUND_4.get(),
            ModSounds.LUNA_RECON_FOUND_5.get(), ModSounds.LUNA_RECON_FOUND_6.get(),
            ModSounds.LUNA_RECON_FOUND_7.get(), ModSounds.LUNA_RECON_FOUND_8.get());

    private LunaReconVoiceService() {
    }

    
    public static void playShootVoice(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        UUID id = player.getUUID();

        if (now < NEXT_SHOOT_VOICE.getOrDefault(id, 0L)) {
            return;
        }
        if (RANDOM.nextFloat() >= SHOOT_VOICE_CHANCE) {
            return;
        }

        NEXT_SHOOT_VOICE.put(id, now + SHOOT_VOICE_COOLDOWN_TICKS);

        VOICE_BUSY_UNTIL.put(id, now + VOICE_BUSY_TICKS);

        SoundEvent voice = SHOOT_VOICES.get(RANDOM.nextInt(SHOOT_VOICES.size()));
        player.playNotifySound(voice, SoundSource.MASTER, 1.5F, 1.0F);
    }

    
    public static void playFoundVoice(ServerPlayer player, LivingEntity target) {
        long now = player.serverLevel().getGameTime();
        UUID id = player.getUUID();

        if (now < VOICE_BUSY_UNTIL.getOrDefault(id, 0L)) {
            return;
        }

        VOICE_BUSY_UNTIL.put(id, now + VOICE_BUSY_TICKS);

        boolean armored = isArmored(target);
        List<SoundEvent> pool = armored ? ARMOR_VOICES : NORMAL_VOICES;
        SoundEvent voice = pool.get(RANDOM.nextInt(pool.size()));
        player.playNotifySound(voice, SoundSource.MASTER, 1.5F, 1.0F);
    }

    private static boolean isArmored(LivingEntity target) {
        ItemStack chest = target.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof ArmorItem armor) {
            return armor.getMaterial() == ArmorMaterials.NETHERITE
                    || armor.getMaterial() == ArmorMaterials.DIAMOND;
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        NEXT_SHOOT_VOICE.remove(id);
        VOICE_BUSY_UNTIL.remove(id);
    }
}
