package lol.silencecute.entity;



import lol.silencecute.event.OperatorState;
import lol.silencecute.manager.LunaReconVoiceService;
import com.mojang.logging.LogUtils;

import lol.silencecute.network.LunaRevealPacket;

import lol.silencecute.registry.ModNetwork;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.network.protocol.Packet;

import net.minecraft.network.protocol.game.ClientGamePacketListener;

import net.minecraft.network.syncher.EntityDataAccessor;

import net.minecraft.network.syncher.EntityDataSerializers;

import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.projectile.ThrowableProjectile;

import net.minecraft.world.level.ClipContext;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;

import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.HitResult;

import net.minecraft.world.phys.Vec3;

import net.minecraftforge.network.NetworkHooks;

import net.minecraftforge.network.PacketDistributor;



import java.util.HashMap;

import java.util.HashSet;

import java.util.Map;

import java.util.Set;

import java.util.UUID;



public final class LunaReconArrowEntity extends ThrowableProjectile {


    public static float GRAVITY = 0.01F;
    public static double MAX_RADIUS = 80.0D;



    

    private static final int HUD2_TICK = 40;

    

    private static final int SAMPLE_BASE_TICK = 86;

    

    private static final int CHECK_TICK = 97;

    

    private static final double MOVE_THRESHOLD_SQR = 0.0001D;

    

    public static int MIN_LANDED_TICKS = 25;

    

    public static int MAX_LANDED_TICKS = 180;



    private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(

             LunaReconArrowEntity.class, EntityDataSerializers.BOOLEAN);



    private final Set<UUID> scannedTargets = new HashSet<>();

    private final Map<UUID, ScanState> trackedPlayers = new HashMap<>();



    private int bouncesLeft = 1;

    private int landedAge;





    private boolean hasPlayedVoice = false;



    private static class ScanState {

        int timeAlive = 0;

        boolean hud2Sent = false;

        boolean checked = false;

        boolean moved = false;

        

        double lastX = Double.NaN;

        double lastZ = Double.NaN;

    }



    public LunaReconArrowEntity(EntityType<? extends LunaReconArrowEntity> type, Level level) {

        super(type, level);

        this.noPhysics = true;

    }



    @Override

    protected void defineSynchedData() {

        this.entityData.define(LANDED, false);

    }



    public boolean isLanded() {

        return this.entityData.get(LANDED);

    }



    private void setLanded(boolean landed) {

        this.entityData.set(LANDED, landed);

    }



    @Override

    protected float getGravity() {

        return isLanded() ? 0.0F : GRAVITY;
    }



    @Override

    public void tick() {

        super.tick();

        if (level().isClientSide()) return;



        if (!isLanded()) {

            if (this.tickCount >= 120 || this.getDeltaMovement().lengthSqr() < 0.1D) {

                land();

            } else {

                performScan();

            }

        } else {

            this.setDeltaMovement(Vec3.ZERO);

            landedAge++;

            if (trackedPlayers.isEmpty() && landedAge >= MIN_LANDED_TICKS) {

                discard();

                return;

            }

            if (landedAge >= MAX_LANDED_TICKS) {

                discard();

                return;

            }

        }



        tickTrackedPlayers();

    }



    private void performScan() {

        if (!(level() instanceof ServerLevel)) return;

        AABB area = AABB.ofSize(position(), MAX_RADIUS * 2, MAX_RADIUS * 2, MAX_RADIUS * 2);



        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {

            if (target == getOwner()) continue;

            if (target instanceof Player p && (p.isCreative() || p.isSpectator())) continue;

            if (target.position().distanceToSqr(position()) > MAX_RADIUS * MAX_RADIUS) continue;



            UUID targetId = target.getUUID();

            if (scannedTargets.contains(targetId)) continue;



            Vec3 targetCenter = target.getBoundingBox().getCenter();

            BlockHitResult obstruction = level().clip(new ClipContext(

                    position(), targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));



            if (obstruction.getType() == HitResult.Type.MISS) {

                scannedTargets.add(targetId);

                triggerFirstReveal(target);

            }

        }

    }



    private void triggerFirstReveal(LivingEntity target) {

        ServerPlayer luna = getOwner() instanceof ServerPlayer sp ? sp : null;



        if (luna != null && OperatorState.isLuna(luna.getUUID())) {

            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> luna),

                    new LunaRevealPacket(target.getId(), 40));




            if (!hasPlayedVoice) {

                hasPlayedVoice = true;

                LunaReconVoiceService.playFoundVoice(luna, target);

            }

        }



        if (target instanceof ServerPlayer p) {

            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),

                    new LunaRevealPacket(-1, 40));

            trackedPlayers.put(p.getUUID(), new ScanState());

            LogUtils.getLogger().info("[LunaRecon] 开始追踪 {}", p.getName().getString());

        }

    }



    private void tickTrackedPlayers() {

        if (trackedPlayers.isEmpty()) return;



        trackedPlayers.entrySet().removeIf(entry -> {

            UUID id = entry.getKey();

            ScanState state = entry.getValue();

            ServerPlayer p = level().getServer().getPlayerList().getPlayer(id);

            ServerPlayer luna = getOwner() instanceof ServerPlayer sp ? sp : null;



            if (p == null || !p.isAlive()) {

                LogUtils.getLogger().info("[LunaRecon] 移除追踪 {}", id);

                return true;

            }



            state.timeAlive++;




            if (state.timeAlive >= HUD2_TICK && !state.hud2Sent) {

                state.hud2Sent = true;

                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),

                        new LunaRevealPacket(-2, 0));

                LogUtils.getLogger().info("[LunaRecon] t={} 发黄条", state.timeAlive);

            }




            if (state.timeAlive >= SAMPLE_BASE_TICK

                    && state.timeAlive < CHECK_TICK

                    && Double.isNaN(state.lastX)) {

                state.lastX = p.getX();

                state.lastZ = p.getZ();

                LogUtils.getLogger().info("[LunaRecon] t={} 采样基准 ({}, {})",

                        state.timeAlive, state.lastX, state.lastZ);

            }




            if (state.timeAlive > SAMPLE_BASE_TICK

                    && state.timeAlive < CHECK_TICK

                    && !Double.isNaN(state.lastX)) {

                double dx = p.getX() - state.lastX;

                double dz = p.getZ() - state.lastZ;

                if (dx * dx + dz * dz > MOVE_THRESHOLD_SQR) {

                    state.moved = true;

                }

                if (p.zza != 0.0F || p.xxa != 0.0F) {

                    state.moved = true;

                }

                state.lastX = p.getX();

                state.lastZ = p.getZ();

            }




            if (state.timeAlive >= CHECK_TICK && !state.checked) {

                state.checked = true;

                LogUtils.getLogger().info("[LunaRecon] t={} 判定 moved={} zza={} xxa={}",

                        state.timeAlive, state.moved, p.zza, p.xxa);

                if (state.moved) {

                    if (luna != null && OperatorState.isLuna(luna.getUUID())) {

                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> luna),

                                new LunaRevealPacket(p.getId(), 40));

                    }

                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),

                            new LunaRevealPacket(-1, 40));

                    LogUtils.getLogger().info("[LunaRecon] 触发二次红条");

                }

                return true;

            }



            return false;

        });

    }



    @Override

    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult hit) {}



    @Override

    protected void onHitBlock(BlockHitResult hit) {

        if (level().isClientSide() || isLanded()) return;

        if (bouncesLeft > 0) {

            bouncesLeft--;

            Vec3 movement = getDeltaMovement();

            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());

            double normalVelocity = movement.dot(normal);

            Vec3 bounced = movement.subtract(normal.scale(2.0D * normalVelocity)).scale(0.7D);

            setDeltaMovement(bounced);

            this.hasImpulse = true;

            setPos(hit.getLocation().add(normal.scale(0.1D)));

        } else {

            setPos(hit.getLocation());

            land();

        }

    }



    private void land() {

        this.landedAge = 0;

        setLanded(true);

        setDeltaMovement(Vec3.ZERO);

        setNoGravity(true);

    }



    @Override

    protected void addAdditionalSaveData(CompoundTag tag) {

        tag.putBoolean("Landed", isLanded());

        tag.putInt("BouncesLeft", bouncesLeft);

        tag.putInt("LandedAge", landedAge);

    }



    @Override

    protected void readAdditionalSaveData(CompoundTag tag) {

        setLanded(tag.getBoolean("Landed"));

        bouncesLeft = tag.getInt("BouncesLeft");

        landedAge = tag.getInt("LandedAge");

        if (isLanded()) {

            setNoGravity(true);

        }

    }



    @Override

    public Packet<ClientGamePacketListener> getAddEntityPacket() {

        return NetworkHooks.getEntitySpawningPacket(this);

    }

}
