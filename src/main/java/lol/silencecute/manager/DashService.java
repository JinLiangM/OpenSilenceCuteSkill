package lol.silencecute.manager;

import lol.silencecute.access.AccessService;
import lol.silencecute.event.OperatorState;
import lol.silencecute.event.PassiveService;
import java.util.HashMap;

import java.util.Map;

import java.util.UUID;

import lol.silencecute.network.SkillSyncPacket;

import lol.silencecute.registry.ModNetwork;

import lol.silencecute.registry.ModSounds;

import net.minecraft.core.BlockPos;

import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundSource;

import net.minecraft.world.entity.Pose;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.level.block.SlabBlock;

import net.minecraft.world.level.block.StairBlock;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.block.state.properties.SlabType;

import net.minecraft.world.phys.AABB;

import net.minecraft.world.phys.Vec3;

import net.minecraft.world.phys.shapes.CollisionContext;

import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraftforge.event.TickEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.network.PacketDistributor;

public final class DashService {

    public static int DASH_TICKS = 10;

    

    public static int CONTROL_LOCK_TICKS = (DASH_TICKS * 2 + 2) / 3;

    

    public static double TOTAL_DISTANCE = 8.0D;

    

    public static double END_SPEED_RATIO = 0.3D;

    

    public static double PEAK_STEP_PER_TICK =

            TOTAL_DISTANCE / (DASH_TICKS * (1.0D + END_SPEED_RATIO) / 2.0D);

    public static void recomputeDerived() {
        CONTROL_LOCK_TICKS = (DASH_TICKS * 2 + 2) / 3;
        PEAK_STEP_PER_TICK = TOTAL_DISTANCE / (DASH_TICKS * (1.0D + END_SPEED_RATIO) / 2.0D);
    }

    

    public static double TOTAL_RISE = 0.5D;

    

    public static float DASH_STEP_HEIGHT = 1.1F;

    

    public static int COOLDOWN_TICKS = 300;

    

    public static float PLAYER_KILL_REFUND = 0.9F;

    

    public static float ENTITY_KILL_REFUND = 0.5F;

    private static final Map<UUID, DashState> ACTIVE = new HashMap<>();

    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private DashService() {

    }

    public static void start(ServerPlayer player, float directionX, float directionZ) {


        if (player.isInWater() || player.isSwimming()) {

            return;

        }

        if (!OperatorState.isWeilong(player.getUUID())) {

            return;

        }

        if (player.getPose() == Pose.SWIMMING || player.isVisuallyCrawling()) {

            return;

        }

        double length = Math.sqrt(directionX * directionX + directionZ * directionZ);

        if (!Double.isFinite(length) || length < 0.1D || ACTIVE.containsKey(player.getUUID())) {

            return;

        }

        long now = player.serverLevel().getGameTime();

        if (!AccessService.isNoCooldown(player.getUUID())

                && COOLDOWNS.getOrDefault(player.getUUID(), 0L) > now) {

            syncCooldown(player);

            return;

        }

        double x = directionX / length;

        double z = directionZ / length;

        ACTIVE.put(player.getUUID(), new DashState(

                x, z, player.getY(), player.maxUpStep(), DASH_TICKS));

        if (!AccessService.isNoCooldown(player.getUUID())) {

            COOLDOWNS.put(player.getUUID(), now + COOLDOWN_TICKS);

        }

        player.setDeltaMovement(Vec3.ZERO);

        player.fallDistance = 0.0F;

        player.level().playSound(player, player.blockPosition(),

                ModSounds.WEILONG_DASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        syncCooldown(player);

    }

    @SubscribeEvent

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {

            return;

        }

        Player player = event.player;

        if (player instanceof ServerPlayer serverPlayer

                && OperatorState.isWeilong(player.getUUID())

                && serverPlayer.serverLevel().getGameTime() % 20 == 0) {

            syncCooldown(serverPlayer);

        }

        DashState state = ACTIVE.get(player.getUUID());

        if (state == null) {

            return;

        }

        if (!player.isAlive()) {

            player.setMaxUpStep(state.originalStepHeight);

            ACTIVE.remove(player.getUUID());

            return;

        }

        if (state.remainingTicks <= 0) {

            Vec3 movement = player.getDeltaMovement();

            player.setDeltaMovement(movement.x, 0.0D, movement.z);

            ACTIVE.remove(player.getUUID());

            finishDash(player, state);

            return;

        }

        int tickIndex = DASH_TICKS - state.remainingTicks;

        double step = stepForTick(tickIndex);

        double stepX = state.directionX * step;

        double stepZ = state.directionZ * step;

        boolean stepAhead = hasDashStepAhead(player, stepX, stepZ);

        player.setMaxUpStep(stepAhead ? DASH_STEP_HEIGHT : state.originalStepHeight);

        if (stepAhead) {

            player.setOnGround(true);

        }

        if (player.horizontalCollision && !stepAhead) {

            ACTIVE.remove(player.getUUID());

            finishDash(player, state);

            return;

        }

        double targetY = state.startY + riseAfterTick(tickIndex);

        player.setDeltaMovement(stepX,

                Math.max(0.0D, targetY - player.getY()),

                stepZ);

        player.fallDistance = 0.0F;

        if (player.level() instanceof ServerLevel serverLevel) {

            serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.8D,

                    player.getZ(), 7, 0.28D, 0.35D, 0.28D, 0.02D);

            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.9D,

                    player.getZ(), 3, 0.18D, 0.25D, 0.18D, 0.02D);

        }

        state.remainingTicks--;

    }

    

    public static boolean hasDashStepAhead(Player player, double stepX, double stepZ) {

        AABB path = player.getBoundingBox().expandTowards(stepX, 0.0D, stepZ)

                .inflate(0.02D, 0.05D, 0.02D);

        int minX = (int) Math.floor(path.minX);

        int maxX = (int) Math.floor(path.maxX);

        int minY = (int) Math.floor(player.getY() - 0.05D);

        int maxY = (int) Math.floor(player.getY() + DASH_STEP_HEIGHT);

        int minZ = (int) Math.floor(path.minZ);

        int maxZ = (int) Math.floor(path.maxZ);

        CollisionContext context = CollisionContext.of(player);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {

            BlockState blockState = player.level().getBlockState(pos);

            boolean dashStep = blockState.getBlock() instanceof StairBlock

                    || blockState.getBlock() instanceof SlabBlock

                    && blockState.getValue(SlabBlock.TYPE) != SlabType.DOUBLE;

            if (!dashStep) {

                continue;

            }

            VoxelShape shape = blockState.getCollisionShape(player.level(), pos, context);

            if (!shape.isEmpty() && shape.bounds().move(pos).intersects(path)) {

                return true;

            }

        }

        return false;

    }

    

    private static void finishDash(Player player, DashState state) {

        player.setMaxUpStep(state.originalStepHeight);

        forceStanding(player);

        if (player instanceof ServerPlayer serverPlayer) {

            PassiveService.triggerAfterDash(serverPlayer);

        }

    }

    

    private static void forceStanding(Player player) {

        player.setShiftKeyDown(false);

        player.setPose(Pose.STANDING);

        player.refreshDimensions();

    }

    

    public static double stepForTick(int tickIndex) {

        if (tickIndex <= 0) {

            return PEAK_STEP_PER_TICK;

        }

        if (tickIndex >= DASH_TICKS - 1) {

            return PEAK_STEP_PER_TICK * END_SPEED_RATIO;

        }

        double progress = tickIndex / (double) (DASH_TICKS - 1);

        return PEAK_STEP_PER_TICK * (1.0D - (1.0D - END_SPEED_RATIO) * progress);

    }

    

    public static double riseAfterTick(int tickIndex) {

        int completedTicks = Math.max(1, Math.min(DASH_TICKS, tickIndex + 1));

        double progress = completedTicks / (double) DASH_TICKS;

        return TOTAL_RISE * progress * progress;

    }

    

    @SubscribeEvent

    public static void onLivingDeath(LivingDeathEvent event) {

        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {

            return;

        }

        if (killer == event.getEntity() || !OperatorState.isWeilong(killer.getUUID())) {

            return;

        }

        float refund = event.getEntity() instanceof ServerPlayer

                ? PLAYER_KILL_REFUND : ENTITY_KILL_REFUND;

        long now = killer.serverLevel().getGameTime();

        long remaining = cooldownRemainTicks(killer.getUUID(), now);

        long reduced = Math.round(remaining * (1.0D - refund));

        COOLDOWNS.put(killer.getUUID(), now + reduced);

        syncCooldown(killer);

    }

    public static long cooldownRemainTicks(UUID playerUuid, long now) {

        if (AccessService.isNoCooldown(playerUuid)) {

            return 0L;

        }

        return Math.max(0L, COOLDOWNS.getOrDefault(playerUuid, 0L) - now);

    }

    public static void clearCooldown(UUID playerUuid) {

        COOLDOWNS.remove(playerUuid);

    }

    

    public static void syncCooldown(ServerPlayer player) {

        long now = player.serverLevel().getGameTime();

        long remainTicks = OperatorState.isJifeng(player.getUUID())
                ? TempstRollService.cooldownRemainTicks(player.getUUID(), now)
                : cooldownRemainTicks(player.getUUID(), now);
        int dashMs = (int) (remainTicks * 50L);

        int passiveMs = (int) (PassiveService.cooldownRemainTicks(player.getUUID(), now) * 50L);

        int bombCharges = MagneticBombService.availableCharges(player.getUUID(), now);

        int bombRechargeMs = (int) (MagneticBombService.nextRechargeRemainTicks(

                player.getUUID(), now) * 50L);

        int ultMs = OperatorState.isJifeng(player.getUUID())
                ? (int) (TempstUltimateService.cooldownRemainTicks(player.getUUID(), now) * 50L)
                : (int) (LunaReconArrowService.nextRechargeRemainTicks(player.getUUID(), now) * 50L);

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),

                new SkillSyncPacket(dashMs, passiveMs, bombCharges, bombRechargeMs, ultMs));

    }

    @SubscribeEvent

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {

        ACTIVE.remove(event.getEntity().getUUID());

        COOLDOWNS.remove(event.getEntity().getUUID());

    }

    private static final class DashState {

        private final double directionX;

        private final double directionZ;

        private final double startY;

        private final float originalStepHeight;

        private int remainingTicks;

        private DashState(double directionX, double directionZ, double startY,

                          float originalStepHeight, int remainingTicks) {

            this.directionX = directionX;

            this.directionZ = directionZ;

            this.startY = startY;

            this.originalStepHeight = originalStepHeight;

            this.remainingTicks = remainingTicks;

        }

    }

}
