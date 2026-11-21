package lol.silencecute.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lol.silencecute.api.ModDamageTypes;
import lol.silencecute.network.LunaRevealPacket;
import lol.silencecute.registry.ModNetwork;
import lol.silencecute.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public final class LunaPassiveService {
    
    public static final int DAMAGE_REVEAL_TICKS = 10;
    
    public static final int GRENADE_REVEAL_TICKS = 20;

    
    private static final int VOICE_COOLDOWN_TICKS = 30 * 20;
    
    private static final float VOICE_CHANCE = 0.1F;
    private static final float VOICE_VOLUME = 1.6F;

    
    private static final int SHOCK_VOICE_COOLDOWN_TICKS = 30 * 20;
    private static final float SHOCK_VOICE_VOLUME = 1.65F;
    private static final int SHOCK_VOICE_COUNT = 4;

    private static final RandomSource RANDOM = RandomSource.create();
    private static final RandomSource SHOCK_VOICE_RANDOM = RandomSource.create();
    private static final Map<UUID, Long> NEXT_VOICE_TIME = new HashMap<>();
    private static final Map<UUID, Long> VOICE_JUDGED_TICK = new HashMap<>();
    private static final Map<UUID, Long> NEXT_SHOCK_VOICE_TIME = new HashMap<>();

    private LunaPassiveService() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        revealFromDamage(event.getEntity(), event.getSource(), event.getAmount());
    }

    private static void revealFromDamage(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0.0F || target.level().isClientSide) {
            return;
        }

        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }

        ServerPlayer luna = resolveLunaAttacker(source, target);
        if (luna == null || target == luna) {
            return;
        }

        int duration = source.is(ModDamageTypes.LUNA_EXPLOSION)
                ? GRENADE_REVEAL_TICKS
                : DAMAGE_REVEAL_TICKS;

        sendReveal(luna, target, duration);

        if (target instanceof ServerPlayer targetPlayer) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> targetPlayer),
                    new LunaRevealPacket(-1, duration));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        ServerPlayer luna = resolveLunaAttacker(event.getSource(), target);
        if (luna == null || target == luna) {
            return;
        }
        playVoiceIfReady(luna);
    }

    public static void revealGrenadeTarget(ServerPlayer luna, LivingEntity target) {
        if (!canReveal(luna, target)) {
            return;
        }
        sendReveal(luna, target, GRENADE_REVEAL_TICKS);
        playVoiceIfReady(luna);
    }

    public static void revealTarget(ServerPlayer luna, LivingEntity target) {
        if (!canReveal(luna, target)) {
            return;
        }
        sendReveal(luna, target, DAMAGE_REVEAL_TICKS);
    }

    public static void playShockArrowVoice(ServerPlayer luna) {
        if (luna == null || !OperatorState.isLuna(luna.getUUID())) {
            return;
        }
        long now = luna.serverLevel().getGameTime();
        UUID lunaId = luna.getUUID();
        if (now < NEXT_SHOCK_VOICE_TIME.getOrDefault(lunaId, 0L)) {
            return;
        }
        NEXT_SHOCK_VOICE_TIME.put(lunaId, now + SHOCK_VOICE_COOLDOWN_TICKS);
        luna.playNotifySound(
                ModSounds.lunaShockVoice(SHOCK_VOICE_RANDOM.nextInt(SHOCK_VOICE_COUNT)),
                SoundSource.MASTER, SHOCK_VOICE_VOLUME, 1.0F);
    }

    private static boolean canReveal(ServerPlayer luna, LivingEntity target) {
        if (luna == null || target == luna || target.level().isClientSide || !OperatorState.isLuna(luna.getUUID())) {
            return false;
        }

        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return true;
    }

    private static void sendReveal(ServerPlayer luna, LivingEntity target, int duration) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> luna),
                new LunaRevealPacket(target.getId(), duration));
    }

    private static void playVoiceIfReady(ServerPlayer luna) {
        long now = luna.serverLevel().getGameTime();
        UUID lunaId = luna.getUUID();
        if (VOICE_JUDGED_TICK.getOrDefault(lunaId, -1L) == now) {
            return;
        }
        VOICE_JUDGED_TICK.put(lunaId, now);
        if (now < NEXT_VOICE_TIME.getOrDefault(lunaId, 0L)) {
            return;
        }
        if (RANDOM.nextFloat() >= VOICE_CHANCE) {
            return;
        }
        NEXT_VOICE_TIME.put(lunaId, now + VOICE_COOLDOWN_TICKS);

        SoundEvent voice = RANDOM.nextBoolean()
                ? ModSounds.LUNA_ENEMY_ANALYSIS_WHERE.get()
                : ModSounds.LUNA_ENEMY_ANALYSIS_WATCHING.get();
        luna.playNotifySound(voice, SoundSource.MASTER, VOICE_VOLUME, 1.0F);
    }

    private static ServerPlayer resolveLunaAttacker(DamageSource source, LivingEntity target) {
        ServerPlayer attacker = asLuna(source.getEntity());
        if (attacker != null) {
            return attacker;
        }

        Entity directEntity = source.getDirectEntity();
        attacker = asLuna(directEntity);
        if (attacker != null) {
            return attacker;
        }
        if (directEntity instanceof TraceableEntity traceable) {
            attacker = asLuna(traceable.getOwner());
            if (attacker != null) {
                return attacker;
            }
        }

        return asLuna(target.getLastHurtByMob());
    }

    private static ServerPlayer asLuna(Entity entity) {
        if (entity instanceof ServerPlayer player && OperatorState.isLuna(player.getUUID())) {
            return player;
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        NEXT_VOICE_TIME.remove(playerId);
        VOICE_JUDGED_TICK.remove(playerId);
        NEXT_SHOCK_VOICE_TIME.remove(playerId);
    }
}
