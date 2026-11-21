package lol.silencecute.entity.throwable;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public abstract class LrtStickyThrowableEntity extends LrtThrowableEntity {

    private static final EntityDataAccessor<Boolean> STICKED = SynchedEntityData.defineId(
            LrtStickyThrowableEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STUCK_ENTITY_ID = SynchedEntityData.defineId(
            LrtStickyThrowableEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Rotations> STUCK_OFFSET = SynchedEntityData.defineId(
            LrtStickyThrowableEntity.class, EntityDataSerializers.ROTATIONS);
    private static final EntityDataAccessor<Rotations> STUCK_ROTATION = SynchedEntityData.defineId(
            LrtStickyThrowableEntity.class, EntityDataSerializers.ROTATIONS);
    private static final EntityDataAccessor<Byte> STUCK_FACE = SynchedEntityData.defineId(
            LrtStickyThrowableEntity.class, EntityDataSerializers.BYTE);

    @Nullable
    private BlockPos stuckBlockPos;
    @Nullable
    private UUID stuckEntityUUID;

    private float throwYaw = Float.NaN;

    protected LrtStickyThrowableEntity(EntityType<? extends Projectile> type, LivingEntity shooter, Level level, int lifeTime) {
        super(type, shooter, level, lifeTime);
    }

    protected LrtStickyThrowableEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STICKED, false);
        this.entityData.define(STUCK_ENTITY_ID, -1);
        this.entityData.define(STUCK_OFFSET, new Rotations(0, 0, 0));
        this.entityData.define(STUCK_ROTATION, new Rotations(0, 0, 0));
        this.entityData.define(STUCK_FACE, (byte) -1);
    }

    @Override
    public boolean shouldBounce() {
        return false;
    }

    public boolean isStuck() {
        return this.entityData.get(STICKED);
    }

    @Nullable
    public Direction getStuckFace() {
        byte value = this.entityData.get(STUCK_FACE);
        return value >= 0 && value <= 5 ? Direction.from3DDataValue(value) : null;
    }

    public Rotations getStuckOffset() {
        return this.entityData.get(STUCK_OFFSET);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isStuck()) {
            this.tickStuck();
        }
    }

    protected void tickStuck() {
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);

        if (!this.level().isClientSide() && this.stuckBlockPos != null) {
            BlockState state = this.level().getBlockState(this.stuckBlockPos);
            if (state.isAir()) {
                this.detach();
                return;
            }
        }

        int entityId = this.entityData.get(STUCK_ENTITY_ID);
        if (entityId != -1) {
            Entity entity = this.level().getEntity(entityId);
            if (entity == null && !this.level().isClientSide()
                    && this.stuckEntityUUID != null && this.level() instanceof ServerLevel serverLevel) {
                entity = serverLevel.getEntity(this.stuckEntityUUID);
                if (entity != null) {
                    this.entityData.set(STUCK_ENTITY_ID, entity.getId());
                }
            }

            if (entity != null && entity.isAlive()) {
                Rotations offsetRot = this.entityData.get(STUCK_OFFSET);
                Vec3 offset = new Vec3(offsetRot.getX(), offsetRot.getY(), offsetRot.getZ());
                float yRot = getEntityRotation(entity);
                Vec3 rotatedOffset = offset.yRot(-yRot * Mth.DEG_TO_RAD);
                this.setPos(entity.position().add(0, entity.getBbHeight() * 0.5, 0).add(rotatedOffset));

                Rotations relativeRot = this.entityData.get(STUCK_ROTATION);
                this.setYRot(yRot + relativeRot.getY());
                this.setXRot(relativeRot.getX());
            } else if (!this.level().isClientSide()) {
                this.onStuckEntityLost();
            }
        }

        this.onStuckTick();
    }

    protected void onStuckTick() {
    }

    protected void onStuckEntityLost() {
    }

    @Override
    protected void updateRotation() {
        if (this.isStuck()) {
            return;
        }
        super.updateRotation();
    }

    protected void alignToVec(Vec3 vec) {
        if (vec.lengthSqr() < 1.0E-7D) return;
        Vec3 normalized = vec.normalize();
        this.setYRot((float) (Mth.atan2(-normalized.x, normalized.z) * (double) (180F / (float) Math.PI)));
        this.setXRot((float) (-Mth.atan2(normalized.y,
                Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z))
                * (double) (180F / (float) Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private void alignFlatSurfaceToThrower(Direction face) {
        if (Float.isNaN(this.throwYaw)) return;
        if (face.getAxis() != Direction.Axis.Y) return;

        this.setYRot(face == Direction.UP ? this.throwYaw + 180.0F : this.throwYaw);
        this.yRotO = this.getYRot();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!Float.isNaN(this.throwYaw)) {
            tag.putFloat("ThrowYaw", this.throwYaw);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.throwYaw = tag.contains("ThrowYaw") ? tag.getFloat("ThrowYaw") : Float.NaN;
    }

    public float getThrowYaw() {
        return this.throwYaw;
    }

    public void setThrowYaw(float throwYaw) {
        this.throwYaw = throwYaw;
    }

    protected static float getEntityRotation(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.yBodyRot;
        }
        return entity.getYRot();
    }

    @Override
    public BounceResult doMultiBounce(Vec3 deltaMovement) {
        if (this.isStuck()) {
            return new BounceResult(this.position(), Vec3.ZERO);
        }

        Vec3 start = this.position();
        Vec3 end = start.add(deltaMovement);
        HitResult hitResult = this.getHitResult(start, end, deltaMovement, this::canHitEntity, this.level());

        if (hitResult.getType() != HitResult.Type.MISS) {
            if (!this.level().isClientSide()) {
                this.onHit(hitResult);
            }
            return new BounceResult(this.position(), Vec3.ZERO);
        }

        return new BounceResult(end, deltaMovement);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int posRotationIncrements, boolean teleport) {
        if (this.isStuck() && this.entityData.get(STUCK_ENTITY_ID) != -1) {
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, posRotationIncrements, teleport);
    }

    protected void detach() {
        this.entityData.set(STICKED, false);
        this.entityData.set(STUCK_ENTITY_ID, -1);
        this.entityData.set(STUCK_FACE, (byte) -1);
        this.setNoGravity(false);
        this.stuckBlockPos = null;
        this.stuckEntityUUID = null;
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.isStuck()) return;
        if (result.getType() == HitResult.Type.MISS) return;

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockResult = (BlockHitResult) result;
            BlockPos resultPos = blockResult.getBlockPos();
            BlockState state = this.level().getBlockState(resultPos);
            SoundEvent event = state.getBlock().getSoundType(state, this.level(), resultPos, this).getStepSound();
            this.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    event, SoundSource.AMBIENT, 2.0F, 1.0F);

            Direction face = blockResult.getDirection();
            this.entityData.set(STICKED, true);
            this.entityData.set(STUCK_FACE, (byte) face.get3DDataValue());
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(result.getLocation().subtract(
                    -face.getStepX() * 0.15D,
                    -face.getStepY() * 0.15D + 0.15D,
                    -face.getStepZ() * 0.15D));

            this.stuckBlockPos = resultPos;

            this.alignToVec(Vec3.atLowerCornerOf(face.getNormal()));
            this.alignFlatSurfaceToThrower(face);
            this.onStuckToBlock(blockResult);
        } else if (result.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) result).getEntity();
            if (entity == this.getOwner() || entity == this.getVehicle()) return;

            if (this.getHitDamage() > 0.0F) {
                entity.hurt(entity.damageSources().thrown(this, this.getOwner()), this.getHitDamage());
            }

            this.entityData.set(STICKED, true);
            this.entityData.set(STUCK_FACE, (byte) -1);
            this.setNoGravity(true);
            this.setDeltaMovement(result.getLocation().subtract(this.position()));

            this.stuckEntityUUID = entity.getUUID();
            this.entityData.set(STUCK_ENTITY_ID, entity.getId());

            Vec3 globalOffset = result.getLocation().subtract(
                    entity.position().add(0, entity.getBbHeight() * 0.5, 0));
            this.alignToVec(this.horizontalOutward(globalOffset));
            float entityYRot = getEntityRotation(entity);
            Vec3 localOffset = globalOffset.yRot(entityYRot * Mth.DEG_TO_RAD).scale(0.75F);

            this.entityData.set(STUCK_OFFSET,
                    new Rotations((float) localOffset.x, (float) localOffset.y, (float) localOffset.z));
            this.entityData.set(STUCK_ROTATION,
                    new Rotations(this.getXRot(), this.getYRot() - entityYRot, 0));
            this.onStuckToEntity(entity);
        }
    }

    private Vec3 horizontalOutward(Vec3 offset) {
        Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
        if (horizontal.lengthSqr() < 1.0E-4D && !Float.isNaN(this.throwYaw)) {
            float rad = this.throwYaw * Mth.DEG_TO_RAD;
            horizontal = new Vec3(Mth.sin(rad), 0.0D, -Mth.cos(rad));
        }
        return horizontal;
    }

    protected void onStuckToBlock(BlockHitResult blockResult) {
    }

    protected void onStuckToEntity(Entity entity) {
    }

    @Nullable
    public BlockPos getStuckBlockPos() {
        return this.stuckBlockPos;
    }

    @Nullable
    public Entity getStuckEntity() {
        int id = this.entityData.get(STUCK_ENTITY_ID);
        return id < 0 ? null : this.level().getEntity(id);
    }
}
