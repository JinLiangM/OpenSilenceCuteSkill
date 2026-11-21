package lol.silencecute.manager;

import lol.silencecute.entity.LunaGrenadeEntity;
import lol.silencecute.access.AccessService;
import lol.silencecute.event.OperatorState;
import lol.silencecute.util.LunaGrenadeBallistics;
import java.util.HashMap;

import java.util.Map;

import java.util.UUID;

import lol.silencecute.network.LunaGrenadeActionPacket;

import lol.silencecute.network.LunaGrenadeSyncPacket;

import lol.silencecute.registry.ModEntities;

import lol.silencecute.registry.ModNetwork;

import lol.silencecute.registry.ModSounds;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundSource;

import net.minecraft.util.RandomSource;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;

import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.network.PacketDistributor;

public final class LunaGrenadeService {

    public static int FULL_FUSE_TICKS = 5 * 20;

    public static int AUTO_THROW_REMAINING_TICKS = 12;

    public static int MAX_CHARGES = 2;

    public static int RECHARGE_TICKS = 45 * 20;

    public static float THROW_VELOCITY = 2.22F;

    public static float THROW_ARC_DEGREES = -6.0F;

    public static final UUID PRIMING_SPEED_MODIFIER_ID = UUID.fromString(
            "d3a13df4-4e27-4ce7-bb31-8a576d618a72");

    public static final AttributeModifier PRIMING_SPEED_MODIFIER = new AttributeModifier(
            PRIMING_SPEED_MODIFIER_ID, "Luna grenade priming slowdown", -0.50D,
            AttributeModifier.Operation.MULTIPLY_TOTAL);

    private static final RandomSource RANDOM = RandomSource.create();

    private static final Map<UUID, HeldGrenade> HELD = new HashMap<>();

    private static final Map<UUID, ChargesState> STATES = new HashMap<>();

    private static final Map<UUID, Long> GUN_LOCK_END = new HashMap<>();

    private static final Map<UUID, Integer> HELD_SLOT = new HashMap<>();

    public enum HeldState {

        NONE,

        EQUIPPED,

        PRIMED

    }

    private LunaGrenadeService() {

    }

    public static void handleAction(ServerPlayer player, LunaGrenadeActionPacket.Action action) {

        if (!player.isAlive() || player.isSpectator() || !OperatorState.isLuna(player.getUUID())) {

            clearHeld(player);

            return;

        }

        switch (action) {

            case EQUIP -> equip(player);

            case PRIME -> prime(player);

            case THROW -> throwGrenade(player, false);

            case CANCEL -> cancel(player);

        }

    }

    private static void equip(ServerPlayer player) {

        long now = player.serverLevel().getGameTime();

        HeldGrenade held = HELD.get(player.getUUID());

        if (held != null) {

            if (held.state == HeldState.PRIMED) {

                sync(player);

                return;

            }

            GUN_LOCK_END.put(player.getUUID(), now + 8L);

            clearHeld(player);

            sync(player);

            return;

        }

        if (availableCharges(player.getUUID(), now) <= 0) {

            sync(player);

            return;

        }

        HELD.put(player.getUUID(), new HeldGrenade(HeldState.EQUIPPED, -1L));

        HELD_SLOT.put(player.getUUID(), player.getInventory().selected);

        sync(player);

    }

    private static void prime(ServerPlayer player) {

        HeldGrenade held = HELD.get(player.getUUID());

        if (held == null || held.state != HeldState.EQUIPPED) {

            return;

        }

        held.state = HeldState.PRIMED;

        held.fuseEndTick = player.serverLevel().getGameTime() + FULL_FUSE_TICKS;

        setPrimingSlowdown(player, true);

        player.level().playSound(null, player.blockPosition(),

                ModSounds.LUNA_GRENADE_PIN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        sync(player);

    }

    private static void throwGrenade(ServerPlayer player, boolean forced) {

        HeldGrenade held = HELD.get(player.getUUID());

        if (held == null || held.state != HeldState.PRIMED) {

            return;

        }

        long now = player.serverLevel().getGameTime();

        int remainingFuse = (int) Math.max(1L, held.fuseEndTick - now);

        if (!forced && remainingFuse <= 0) {

            remainingFuse = 1;

        }

        LunaGrenadeEntity grenade = new LunaGrenadeEntity(ModEntities.LUNA_GRENADE.get(),

                player.level());

        grenade.setOwner(player);

        grenade.setFuse(remainingFuse);

        grenade.setPos(LunaGrenadeBallistics.origin(player, 1.0F));

        Vec3 velocity = LunaGrenadeBallistics.velocity(player, 1.0F);

        grenade.shoot(velocity.x, velocity.y, velocity.z, (float) velocity.length(), 0.0F);

        if (!player.level().addFreshEntity(grenade)) {

            return;

        }

        HELD.remove(player.getUUID());

        GUN_LOCK_END.put(player.getUUID(), now + 10L);

        setPrimingSlowdown(player, false);

        if (!AccessService.isNoCooldown(player.getUUID())) {

            ChargesState state = state(player.getUUID());

            state.charges = Math.max(0, state.charges - 1);

            if (state.charges == 0 && state.nextRestoreTick == 0L) {

                state.nextRestoreTick = now + RECHARGE_TICKS;

            }

        }

        player.level().playSound(null, player.blockPosition(),

                ModSounds.LUNA_GRENADE_THROW.get(), SoundSource.PLAYERS, 0.9F, 1.0F);

        playThrowVoice(player);

        sync(player);

    }

    private static void cancel(ServerPlayer player) {

        HeldGrenade held = HELD.get(player.getUUID());

        if (held == null || held.state == HeldState.PRIMED) {

            return;

        }

        GUN_LOCK_END.put(player.getUUID(), player.serverLevel().getGameTime() + 8L);

        clearHeld(player);

        sync(player);

    }

    @SubscribeEvent

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()

                || !(event.player instanceof ServerPlayer player)) {

            return;

        }

        UUID playerId = player.getUUID();

        long now = player.serverLevel().getGameTime();

        HeldGrenade held = HELD.get(playerId);

        if (!player.isAlive() || !OperatorState.isLuna(playerId)) {

            if (held != null) {

                clearHeld(player);

                sync(player);

            }

            return;

        }

        if (held != null && held.state == HeldState.PRIMED) {

            setPrimingSlowdown(player, true);

            if (held.fuseEndTick - now <= AUTO_THROW_REMAINING_TICKS) {

                throwGrenade(player, true);

                return;

            }

        } else {

            setPrimingSlowdown(player, false);

            if (held != null) {

                Integer slot = HELD_SLOT.get(playerId);

                if (slot != null && slot != player.getInventory().selected) {

                    cancel(player);

                    return;

                }

            }

        }

        if (now % 10L == 0L) {

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

    private static ChargesState state(UUID playerId) {

        return STATES.computeIfAbsent(playerId, ignored -> new ChargesState());

    }

    public static boolean blocksGunfire(UUID playerId, long now) {

        return HELD.containsKey(playerId) || GUN_LOCK_END.getOrDefault(playerId, 0L) > now;

    }

    public static HeldState heldState(UUID playerId) {

        HeldGrenade held = HELD.get(playerId);

        return held == null ? HeldState.NONE : held.state;

    }

    public static void sync(ServerPlayer player) {

        long now = player.serverLevel().getGameTime();

        HeldGrenade held = HELD.get(player.getUUID());

        HeldState state = held == null ? HeldState.NONE : held.state;

        int fuse = held == null || held.state != HeldState.PRIMED

                ? 0 : (int) Math.max(0L, held.fuseEndTick - now);

        int charges = availableCharges(player.getUUID(), now);

        int rechargeMillis = (int) (nextRechargeRemainTicks(player.getUUID(), now) * 50L);

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),

                new LunaGrenadeSyncPacket(state, fuse, charges, rechargeMillis));

    }

    private static void playThrowVoice(ServerPlayer player) {

        if (!RANDOM.nextBoolean()) {

            return;

        }

        player.playNotifySound(ModSounds.lunaGrenadeVoice(RANDOM.nextInt(6)),

                SoundSource.MASTER, 1.65F, 1.0F);

    }

    private static void setPrimingSlowdown(ServerPlayer player, boolean enabled) {

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (speed == null) {

            return;

        }

        speed.removeModifier(PRIMING_SPEED_MODIFIER_ID);

        if (enabled) {

            speed.addTransientModifier(PRIMING_SPEED_MODIFIER);

        }

    }

    private static void clearHeld(ServerPlayer player) {

        HELD.remove(player.getUUID());

        HELD_SLOT.remove(player.getUUID());

        setPrimingSlowdown(player, false);

    }

    public static void clearCooldown(UUID playerId) {

        STATES.remove(playerId);

    }

    @SubscribeEvent

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {

        UUID playerId = event.getEntity().getUUID();

        if (event.getEntity() instanceof ServerPlayer player) {

            clearHeld(player);

        }

        STATES.remove(playerId);

        GUN_LOCK_END.remove(playerId);

    }

    private static final class HeldGrenade {

        private HeldState state;

        private long fuseEndTick;

        private HeldGrenade(HeldState state, long fuseEndTick) {

            this.state = state;

            this.fuseEndTick = fuseEndTick;

        }

    }

    private static final class ChargesState {

        private int charges = MAX_CHARGES;

        private long nextRestoreTick = 0L;

    }

}

