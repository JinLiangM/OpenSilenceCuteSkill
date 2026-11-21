package lol.silencecute.event;

import lol.silencecute.manager.DashService;
import java.util.HashMap;

import java.util.Map;

import java.util.UUID;

import lol.silencecute.registry.ModEffects;

import lol.silencecute.registry.ModSounds;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundSource;

import net.minecraft.world.damagesource.DamageTypes;

import net.minecraft.world.effect.MobEffectInstance;

import net.minecraftforge.event.entity.living.LivingFallEvent;

import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PassiveService {

    

    public static final float TRIGGER_FALL_BLOCKS = 4.0F;

    

    public static final int PASSIVE_COOLDOWN_TICKS = 50;

    

    public static final int BOOST_DURATION_TICKS = 40;

    public static int hudBoostDurationTicks() {
        return lol.silencecute.config.ModConfigData.operator() == lol.silencecute.api.Operator.JIFENG
                ? TempstPassiveService.BOOST_TICKS
                : BOOST_DURATION_TICKS;
    }

    

    public static final float SAFE_FALL_DAMAGE_CAP = 4.0F;

    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private PassiveService() {

    }

    

    @SubscribeEvent

    public static void onFall(LivingFallEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {

            return;

        }

        if (event.getDistance() < TRIGGER_FALL_BLOCKS) {

            return;

        }

        if (!OperatorState.isWeilong(player.getUUID())) {

            return;

        }

        tryTrigger(player);

    }

    

    @SubscribeEvent

    public static void onFallHurt(LivingHurtEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {

            return;

        }

        if (!event.getSource().is(DamageTypes.FALL)) {

            return;

        }

        if (!OperatorState.isWeilong(player.getUUID())) {

            return;

        }

        float amount = event.getAmount();

        if (amount <= 0.0F) {

            return;

        }

        float survivable = player.getHealth() + player.getAbsorptionAmount();

        if (amount >= survivable) {

            return;

        }

        if (amount > SAFE_FALL_DAMAGE_CAP) {

            event.setAmount(SAFE_FALL_DAMAGE_CAP);

        }

    }

    

    public static void triggerAfterDash(ServerPlayer player) {

        if (OperatorState.isWeilong(player.getUUID())) {

            tryTrigger(player);

        }

    }

    

    public static void triggerAfterBombThrow(ServerPlayer player) {

        if (OperatorState.isWeilong(player.getUUID())) {

            tryTrigger(player);

        }

    }

    private static void tryTrigger(ServerPlayer player) {

        if (player.hasEffect(ModEffects.KINETIC_BOOST.get())) {
            return;
        }

        long now = player.serverLevel().getGameTime();

        if (COOLDOWNS.getOrDefault(player.getUUID(), 0L) > now) {
            return;
        }

        COOLDOWNS.put(player.getUUID(), now + PASSIVE_COOLDOWN_TICKS);

        player.addEffect(new MobEffectInstance(ModEffects.KINETIC_BOOST.get(),
                BOOST_DURATION_TICKS, 0, false, false, false));

        player.level().playSound(null, player.blockPosition(),

                ModSounds.WEILONG_PASSIVE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        DashService.syncCooldown(player);

    }

    public static long cooldownRemainTicks(UUID playerUuid, long now) {

        

        return Math.max(0L, COOLDOWNS.getOrDefault(playerUuid, 0L) - now);

    }

    public static void clearCooldown(UUID playerUuid) {

        COOLDOWNS.remove(playerUuid);

    }

    @SubscribeEvent

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {

        COOLDOWNS.remove(event.getEntity().getUUID());

    }

}
