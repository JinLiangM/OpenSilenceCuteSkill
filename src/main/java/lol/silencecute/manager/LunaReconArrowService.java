package lol.silencecute.manager;



import lol.silencecute.entity.LunaReconArrowEntity;
import lol.silencecute.access.AccessService;
import lol.silencecute.event.OperatorState;
import java.util.HashMap;

import java.util.HashSet;

import java.util.Map;

import java.util.Set;

import java.util.UUID;



import lol.silencecute.network.LunaReconArrowActionPacket;

import lol.silencecute.network.LunaReconArrowSyncPacket;

import lol.silencecute.registry.ModEntities;

import lol.silencecute.registry.ModNetwork;

import lol.silencecute.registry.ModSounds;


import lol.silencecute.util.BallisticAim;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundSource;

import net.minecraft.util.Mth;

import net.minecraft.world.level.ClipContext;

import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.HitResult;

import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;

import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.network.PacketDistributor;





public final class LunaReconArrowService {

    public static int MAX_CHARGES = 1;

    public static int RECHARGE_TICKS = 75 * 20;

    public static int FIRE_COOLDOWN_TICKS = 10;



    public static float MIN_SPEED = 1.05F;

    public static float MAX_SPEED = 4.35F;

    public static float TAP_SHOT_MAX_POWER = 1.0F / 3.0F;

    public static float TAP_SHOT_HORIZONTAL_SCALE = 1.5F;

    public static float TAP_SHOT_VERTICAL_SCALE = 1.22F;


    private static final double AIM_RANGE = 64.0D;


    private static final Set<UUID> DRAWN = new HashSet<>();

    private static final Map<UUID, Long> COOLDOWN_END = new HashMap<>();

    private static final Map<UUID, Integer> LAST_SEQUENCE = new HashMap<>();

    private static final Map<UUID, ChargesState> STATES = new HashMap<>();



    private LunaReconArrowService() {

    }



    public static void handleAction(ServerPlayer player, LunaReconArrowActionPacket.Action action,

                                    float power, int sequence) {

        LAST_SEQUENCE.put(player.getUUID(), sequence);

        if (!player.isAlive() || player.isSpectator() || !OperatorState.isLuna(player.getUUID())) {

            DRAWN.remove(player.getUUID());

            sync(player);

            return;

        }

        switch (action) {

            case TOGGLE -> toggle(player);

            case FIRE -> fire(player, power);

            case STOW -> {

                DRAWN.remove(player.getUUID());

                sync(player);

            }

        }

    }



    private static void toggle(ServerPlayer player) {

        UUID playerId = player.getUUID();

        if (!DRAWN.remove(playerId)) {

            if (!AccessService.isNoCooldown(playerId)

                    && availableCharges(playerId, player.serverLevel().getGameTime()) <= 0) {

                sync(player);

                return;

            }

            DRAWN.add(playerId);

        }

        sync(player);

    }



    private static void fire(ServerPlayer player, float rawPower) {

        UUID playerId = player.getUUID();

        if (!DRAWN.contains(playerId)) {

            sync(player);

            return;

        }

        long now = player.serverLevel().getGameTime();

        if (COOLDOWN_END.getOrDefault(playerId, 0L) > now || !Float.isFinite(rawPower)) {

            DRAWN.remove(playerId);

            sync(player);

            return;

        }

        boolean infinite = AccessService.isNoCooldown(playerId);

        ChargesState state = state(playerId);

        advanceRecovery(state, now);

        if (!infinite && state.charges <= 0) {

            DRAWN.remove(playerId);

            sync(player);

            return;

        }

        float power = Mth.clamp(rawPower, 0.0F, 1.0F);

        boolean tapShot = power < TAP_SHOT_MAX_POWER;



        LunaReconArrowEntity arrow = new LunaReconArrowEntity(

                ModEntities.LUNA_RECON_ARROW.get(), player.level());

        arrow.setOwner(player);

        Vec3 look = player.getViewVector(1.0F).normalize();

        Vec3 right = new Vec3(-look.z, 0.0D, look.x).normalize();

        Vec3 eye = player.getEyePosition(1.0F);


        BlockHitResult aimHit = player.level().clip(
                new ClipContext(eye, eye.add(look.scale(AIM_RANGE)),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 aim = aimHit.getType() == HitResult.Type.MISS
                ? eye.add(look.scale(AIM_RANGE)) : aimHit.getLocation();
        Vec3 origin = eye.add(look.scale(0.20D)).add(right.scale(0.16D)).add(0, -0.12D, 0);

        BlockHitResult obstruction = player.level().clip(

                new ClipContext(eye, origin, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (obstruction.getType() != HitResult.Type.MISS) {

            origin = obstruction.getLocation().subtract(origin.subtract(eye).normalize().scale(0.02D));

        }

        arrow.setPos(origin);

        float speed = MIN_SPEED + power * (MAX_SPEED - MIN_SPEED);

        Vec3 direction = BallisticAim.solve(origin, aim, look, speed,
                LunaReconArrowEntity.GRAVITY,
                tapShot ? TAP_SHOT_HORIZONTAL_SCALE : 1.0D,
                tapShot ? TAP_SHOT_VERTICAL_SCALE : 1.0D);
        arrow.shoot(direction.x, direction.y, direction.z, speed, 0.0F);

        if (tapShot) {

            Vec3 movement = arrow.getDeltaMovement();

            arrow.setDeltaMovement(movement.x * TAP_SHOT_HORIZONTAL_SCALE,

                    movement.y * TAP_SHOT_VERTICAL_SCALE,

                    movement.z * TAP_SHOT_HORIZONTAL_SCALE);

        }

        boolean spawned = player.level().addFreshEntity(arrow);

        DRAWN.remove(playerId);

        if (spawned) {

            COOLDOWN_END.put(playerId, now + FIRE_COOLDOWN_TICKS);

            if (!infinite) {

                state.charges--;

                if (state.charges == 0 && state.nextRestoreTick == 0L) {

                    state.nextRestoreTick = now + RECHARGE_TICKS;

                }

            }

            player.level().playSound(player, player.getX(), player.getEyeY(), player.getZ(),

                    ModSounds.LUNA_BOW_SHOOT.get(), SoundSource.PLAYERS, 0.72F, 1.18F);



            LunaReconVoiceService.playShootVoice(player);




        }

        sync(player);

    }



    @SubscribeEvent

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()

                || !(event.player instanceof ServerPlayer player)) {

            return;

        }

        UUID playerId = player.getUUID();

        if (!player.isAlive() || !OperatorState.isLuna(playerId)) {

            if (DRAWN.remove(playerId)) {

                sync(player);

            }

            return;

        }

        if (player.serverLevel().getGameTime() % 20L == 0L) {

            sync(player);

        }

    }



    public static int availableCharges(UUID playerId, long now) {

        if (AccessService.isNoCooldown(playerId)) {

            return MAX_CHARGES;

        }

        ChargesState state = state(playerId);

        advanceRecovery(state, now);

        return state.charges;

    }



    public static long nextRechargeRemainTicks(UUID playerId, long now) {

        if (AccessService.isNoCooldown(playerId)) {

            return 0L;

        }

        ChargesState state = state(playerId);

        advanceRecovery(state, now);

        if (state.nextRestoreTick == 0L) {

            return 0L;

        }

        return Math.max(0L, state.nextRestoreTick - now);

    }



    static boolean blocksGunfire(UUID playerId) {

        return DRAWN.contains(playerId);

    }



    public static void sync(ServerPlayer player) {

        long now = player.serverLevel().getGameTime();

        long remain = Math.max(0L, COOLDOWN_END.getOrDefault(player.getUUID(), 0L) - now);

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),

                new LunaReconArrowSyncPacket(

                        DRAWN.contains(player.getUUID()),

                        (int) remain * 50,

                        availableCharges(player.getUUID(), now),

                        (int) (nextRechargeRemainTicks(player.getUUID(), now) * 50L),

                        LAST_SEQUENCE.getOrDefault(player.getUUID(), 0)));

    }



    @SubscribeEvent

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {

        clear(event.getEntity().getUUID());

    }



    private static ChargesState state(UUID playerId) {

        return STATES.computeIfAbsent(playerId, ignored -> new ChargesState());

    }



    private static void advanceRecovery(ChargesState state, long now) {

        while (state.nextRestoreTick > 0L && now >= state.nextRestoreTick

                && state.charges < MAX_CHARGES) {

            state.charges++;

            if (state.charges < MAX_CHARGES) {

                state.nextRestoreTick += RECHARGE_TICKS;

            } else {

                state.nextRestoreTick = 0L;

            }

        }

    }



    public static void clearCooldown(UUID playerId) {
        COOLDOWN_END.remove(playerId);
        STATES.remove(playerId);
    }

    private static void clear(UUID playerId) {

        DRAWN.remove(playerId);

        COOLDOWN_END.remove(playerId);

        LAST_SEQUENCE.remove(playerId);

        STATES.remove(playerId);

    }



    private static final class ChargesState {

        private int charges = MAX_CHARGES;

        private long nextRestoreTick = 0L;

    }

}
