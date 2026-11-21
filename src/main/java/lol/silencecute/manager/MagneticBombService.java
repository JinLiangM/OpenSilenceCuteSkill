package lol.silencecute.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lol.silencecute.access.AccessService;
import lol.silencecute.entity.MagneticBombEntity;
import lol.silencecute.event.OperatorState;
import lol.silencecute.event.PassiveService;
import lol.silencecute.registry.ModEntities;
import lol.silencecute.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MagneticBombService {

    public static float THROW_VELOCITY = 1.125F;

    public static float THROW_INACCURACY = 0.0F;

    public static float THROW_ARC_DEGREES = 0.0F;

    public static int RECHARGE_TICKS = 60 * 20;

    public static int MAX_CHARGES = 2;

    public static int MIN_THROW_INTERVAL_TICKS = 4;

    private static final Map<UUID, Long> LAST_THROW = new HashMap<>();

    private static final Map<UUID, ChargesState> STATES = new HashMap<>();

    private MagneticBombService() {
    }

    public static void throwBomb(ServerPlayer player) {
        if (player == null) return;
        if (!player.isAlive() || player.isSpectator() || !OperatorState.isWeilong(player.getUUID())) return;

        long now = player.serverLevel().getGameTime();
        boolean noCooldown = AccessService.isNoCooldown(player.getUUID());
        long last = LAST_THROW.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2L);
        if (!noCooldown && now - last < MIN_THROW_INTERVAL_TICKS) return;

        ChargesState state = state(player.getUUID());
        advanceRecovery(state, now);
        if (!noCooldown && state.charges <= 0) {
            DashService.syncCooldown(player);
            return;
        }

        LAST_THROW.put(player.getUUID(), now);
        if (!noCooldown) {
            state.charges--;
            if (state.charges == 0 && state.nextRestoreTick == 0L) {
                state.nextRestoreTick = now + RECHARGE_TICKS;
            }
        }
        DashService.syncCooldown(player);

        player.playNotifySound(ModSounds.C4_IN_HAND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        spawnBomb(player);
    }

    private static void spawnBomb(ServerPlayer player) {
        if (player == null || player.hasDisconnected() || !player.isAlive()) return;

        MagneticBombEntity bomb = new MagneticBombEntity(ModEntities.MAGNETIC_BOMB.get(), player.level());
        bomb.setOwner(player);

        Vec3 look = player.getLookAngle();
        double spawnX = player.getX() + look.x * 0.35D;
        double spawnY = player.getEyeY() - 0.12D + look.y * 0.25D;
        double spawnZ = player.getZ() + look.z * 0.35D;
        bomb.setPos(spawnX, spawnY, spawnZ);
        bomb.shootFromRotation(player, player.getXRot(), player.getYRot(),
                THROW_ARC_DEGREES, THROW_VELOCITY, THROW_INACCURACY);
        bomb.setThrowYaw(player.getYRot());

        if (!player.level().addFreshEntity(bomb)) return;

        PassiveService.triggerAfterBombThrow(player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.75F, 0.72F);
        DashService.syncCooldown(player);
    }

    public static int availableCharges(UUID playerUuid, long now) {
        if (AccessService.isNoCooldown(playerUuid)) return MAX_CHARGES;
        ChargesState state = state(playerUuid);
        advanceRecovery(state, now);
        return state.charges;
    }

    public static long nextRechargeRemainTicks(UUID playerUuid, long now) {
        if (AccessService.isNoCooldown(playerUuid)) return 0L;
        ChargesState state = state(playerUuid);
        advanceRecovery(state, now);
        if (state.nextRestoreTick == 0L) return 0L;
        return Math.max(0L, state.nextRestoreTick - now);
    }

    public static void clearCooldown(UUID playerUuid) {
        LAST_THROW.remove(playerUuid);
        STATES.remove(playerUuid);
    }

    private static ChargesState state(UUID playerUuid) {
        return STATES.computeIfAbsent(playerUuid, ignored -> new ChargesState());
    }

    private static void advanceRecovery(ChargesState state, long now) {
        while (state.nextRestoreTick > 0L && now >= state.nextRestoreTick && state.charges < MAX_CHARGES) {
            state.charges++;
            if (state.charges < MAX_CHARGES) state.nextRestoreTick += RECHARGE_TICKS;
            else state.nextRestoreTick = 0L;
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        LAST_THROW.remove(uuid);
        STATES.remove(uuid);
    }

    private static final class ChargesState {

        private int charges = MAX_CHARGES;

        private long nextRestoreTick = 0L;
    }
}
