package lol.silencecute.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lol.silencecute.access.AccessService;
import lol.silencecute.event.OperatorState;
import lol.silencecute.event.TempstPassiveService;
import lol.silencecute.network.TempstRollSyncPacket;
import lol.silencecute.registry.ModEffects;
import lol.silencecute.registry.ModNetwork;
import lol.silencecute.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public final class TempstRollService {

    public static int BASE_TICKS = 10;

    public static double BASE_DISTANCE = 10.0D;

    public static double PASSIVE_EXTRA_DISTANCE = 2.0D;

    public static double STAIR_EXTRA_DISTANCE = 2.0D;

    public static double STAIR_SPEED_MULT = 1.10D;

    public static int COOLDOWN_TICKS = 600;

    public static float FALL_DAMAGE_REDUCTION = 0.8F;

    public static int LANDING_GRACE_TICKS = 12;

    public static float PLAYER_KILL_REFUND = 0.9F;
    public static float ENTITY_KILL_REFUND = 0.5F;

    private static final Map<UUID, RollState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LANDING_GRACE = new HashMap<>();

    private TempstRollService() {
    }

    public record Profile(double[] steps, int ticks, boolean stairs) {

        public double step(int index) {
            if (steps.length == 0) return 0.0D;
            return steps[Math.max(0, Math.min(steps.length - 1, index))];
        }
    }

    public static double speedWeight(int tickIndex, int ticks) {
        if (ticks <= 1) return 1.0D;
        double u = tickIndex / (double) (ticks - 1);
        if (u <= 0.5D) {
            return 0.25D + 0.75D * (u / 0.5D);
        }
        return 1.0D - 0.80D * (u - 0.5D) / 0.5D;
    }

    public static double[] steps(double distance, int ticks) {
        int count = Math.max(1, ticks);
        double[] weights = new double[count];
        double sum = 0.0D;
        for (int i = 0; i < count; i++) {
            weights[i] = speedWeight(i, count);
            sum += weights[i];
        }
        if (sum <= 0.0D) {
            weights[0] = distance;
            return weights;
        }
        for (int i = 0; i < count; i++) {
            weights[i] = distance * weights[i] / sum;
        }
        return weights;
    }

    public static Profile profile(Player player, double dirX, double dirZ) {
        boolean stairs = onStairs(player, dirX, dirZ);
        double distance = BASE_DISTANCE + (stairs ? STAIR_EXTRA_DISTANCE : 0.0D);
        if (player.hasEffect(ModEffects.KINETIC_BOOST.get())) {
            distance += PASSIVE_EXTRA_DISTANCE;
        }
        double speed = (BASE_DISTANCE / BASE_TICKS) * (stairs ? STAIR_SPEED_MULT : 1.0D);
        int ticks = (int) Math.max(3L, Math.round(distance / speed));
        return new Profile(steps(distance, ticks), ticks, stairs);
    }

    private static boolean onStairs(Player player, double dirX, double dirZ) {
        if (isStair(player, player.blockPosition().below())) return true;
        BlockPos ahead = BlockPos.containing(player.getX() + dirX, player.getY() - 0.2D,
                player.getZ() + dirZ);
        return isStair(player, ahead);
    }

    private static boolean isStair(Player player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        return state.getBlock() instanceof StairBlock;
    }

    public static void start(ServerPlayer player, float directionX, float directionZ) {
        if (!OperatorState.isJifeng(player.getUUID())) return;
        if (!player.onGround()) return;
        if (player.isInWater() || player.isSwimming()) return;
        if (player.getPose() == Pose.SWIMMING || player.isVisuallyCrawling()) return;
        if (ACTIVE.containsKey(player.getUUID())) return;

        double length = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (!Double.isFinite(length) || length < 0.1D) return;

        long now = player.serverLevel().getGameTime();
        if (!AccessService.isNoCooldown(player.getUUID())
                && COOLDOWNS.getOrDefault(player.getUUID(), 0L) > now) {
            syncCooldown(player);
            return;
        }

        double x = directionX / length;
        double z = directionZ / length;
        Profile profile = profile(player, x, z);

        ACTIVE.put(player.getUUID(), new RollState(x, z, profile,
                player.maxUpStep()));
        LANDING_GRACE.put(player.getUUID(), now + profile.ticks() + LANDING_GRACE_TICKS);
        player.refreshDimensions();

        if (!AccessService.isNoCooldown(player.getUUID())) {
            COOLDOWNS.put(player.getUUID(), now + COOLDOWN_TICKS);
        }

        player.setDeltaMovement(Vec3.ZERO);
        player.clearFire();
        player.setSharedFlagOnFire(false);
        player.level().playSound(null, player.blockPosition(),
                ModSounds.TEMPST_DASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        syncCooldown(player);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new TempstRollSyncPacket(player.getId(), profile.ticks(), (float) x, (float) z));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        if (player instanceof ServerPlayer serverPlayer
                && OperatorState.isJifeng(player.getUUID())
                && serverPlayer.serverLevel().getGameTime() % 20 == 0) {
            syncCooldown(serverPlayer);
        }

        RollState state = ACTIVE.get(player.getUUID());

        Long grace = LANDING_GRACE.get(player.getUUID());
        long now = player.level().getGameTime();
        if (grace != null && grace - now > 60L && state == null && player.onGround()) {
            LANDING_GRACE.put(player.getUUID(), now + 2L);
        }

        if (state == null) return;

        if (!player.isAlive()) {
            player.setMaxUpStep(state.originalStepHeight);
            player.refreshDimensions();
            ACTIVE.remove(player.getUUID());
            return;
        }

        if (state.remainingTicks <= 0) {
            ACTIVE.remove(player.getUUID());
            finishRoll(player, state);
            return;
        }

        double step = state.profile.step(state.profile.ticks() - state.remainingTicks);
        double stepX = state.directionX * step;
        double stepZ = state.directionZ * step;

        boolean stepAhead = DashService.hasDashStepAhead(player, stepX, stepZ);
        player.setMaxUpStep(stepAhead ? DashService.DASH_STEP_HEIGHT
                : state.originalStepHeight);
        if (stepAhead) {
            player.setOnGround(true);
        }

        if (player.horizontalCollision && !stepAhead
                && state.profile.ticks() - state.remainingTicks >= 2) {
            ACTIVE.remove(player.getUUID());
            finishRoll(player, state);
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        double dy = stepAhead ? Math.max(movement.y, 0.35D)
                : Math.min(movement.y, 0.0D);
        player.setDeltaMovement(stepX, dy, stepZ);

        state.remainingTicks--;
        if (state.remainingTicks <= 0) {
            ACTIVE.remove(player.getUUID());
            finishRoll(player, state);
        }
    }

    private static void finishRoll(Player player, RollState state) {
        player.setMaxUpStep(state.originalStepHeight);
        player.refreshDimensions();
        if (!player.onGround()) {
            LANDING_GRACE.put(player.getUUID(),
                    player.level().getGameTime() + 1200L);
        } else {
            LANDING_GRACE.put(player.getUUID(),
                    player.level().getGameTime() + LANDING_GRACE_TICKS);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            TempstPassiveService.triggerFromSkill(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onFallHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getSource().is(DamageTypes.FALL)) return;
        if (!OperatorState.isJifeng(player.getUUID())) return;

        long now = player.serverLevel().getGameTime();
        boolean rolling = ACTIVE.containsKey(player.getUUID());
        long grace = LANDING_GRACE.getOrDefault(player.getUUID(), 0L);
        if (!rolling && grace <= now) return;

        event.setAmount(event.getAmount() * (1.0F - FALL_DAMAGE_REDUCTION));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (killer == event.getEntity() || !OperatorState.isJifeng(killer.getUUID())) return;

        float refund = event.getEntity() instanceof ServerPlayer
                ? PLAYER_KILL_REFUND : ENTITY_KILL_REFUND;
        long now = killer.serverLevel().getGameTime();
        long remaining = cooldownRemainTicks(killer.getUUID(), now);
        long reduced = Math.round(remaining * (1.0D - refund));
        COOLDOWNS.put(killer.getUUID(), now + reduced);
        syncCooldown(killer);
    }

    public static long cooldownRemainTicks(UUID playerUuid, long now) {
        if (lol.silencecute.access.AccessService.isNoCooldown(playerUuid)) return 0L;
        return Math.max(0L, COOLDOWNS.getOrDefault(playerUuid, 0L) - now);
    }

    public static void clearCooldown(UUID playerUuid) {
        COOLDOWNS.remove(playerUuid);
    }

    public static boolean isRolling(UUID playerUuid) {
        return ACTIVE.containsKey(playerUuid);
    }

    public static void syncCooldown(ServerPlayer player) {
        DashService.syncCooldown(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
        COOLDOWNS.remove(event.getEntity().getUUID());
        LANDING_GRACE.remove(event.getEntity().getUUID());
    }

    private static final class RollState {
        private final double directionX;
        private final double directionZ;
        private final Profile profile;
        private final float originalStepHeight;
        private int remainingTicks;

        private RollState(double directionX, double directionZ, Profile profile,
                          float originalStepHeight) {
            this.directionX = directionX;
            this.directionZ = directionZ;
            this.profile = profile;
            this.originalStepHeight = originalStepHeight;
            this.remainingTicks = profile.ticks();
        }
    }
}
