package lol.silencecute.entity.throwable;

import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public abstract class LrtThrowableEntity extends Projectile implements IEntityAdditionalSpawnData {

    private int life = 100;
    private float gravity = 0.07F;
    private double bounceFactor = 0.75D;
    private boolean shouldBounce = true;
    private boolean brokeOnGround = false;
    private float hitDamage = 1.0F;
    @Nullable
    private ParticleOptions tailParticle;

    protected LrtThrowableEntity(EntityType<? extends Projectile> type, LivingEntity shooter, Level level, int lifeTime) {
        super(type, level);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        this.setOwner(shooter);
        this.life = lifeTime;
    }

    protected LrtThrowableEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", this.life);
        tag.putFloat("Gravity", this.gravity);
        tag.putDouble("BounceFactor", this.bounceFactor);
        tag.putBoolean("ShouldBounce", this.shouldBounce);
        tag.putBoolean("BrokeOnGround", this.brokeOnGround);
        tag.putFloat("HitDamage", this.hitDamage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Life")) this.life = tag.getInt("Life");
        if (tag.contains("Gravity")) this.gravity = tag.getFloat("Gravity");
        if (tag.contains("BounceFactor")) this.bounceFactor = tag.getDouble("BounceFactor");
        if (tag.contains("ShouldBounce")) this.shouldBounce = tag.getBoolean("ShouldBounce");
        if (tag.contains("BrokeOnGround")) this.brokeOnGround = tag.getBoolean("BrokeOnGround");
        if (tag.contains("HitDamage")) this.hitDamage = tag.getFloat("HitDamage");
    }


    @Override
    protected void onHit(HitResult result) {
        if (result.getType() != HitResult.Type.MISS) {
            boolean diedOnGround = this.brokeOnGround && result instanceof BlockHitResult blockHitResult
                    && blockHitResult.getDirection() == Direction.UP;
            if (!this.shouldBounce || diedOnGround) {
                this.onDeath(result);
                return;
            }
        }
        switch (result.getType()) {
            case BLOCK -> {
                BlockHitResult blockResult = (BlockHitResult) result;
                BlockState state = this.level().getBlockState(blockResult.getBlockPos());
                SoundEvent event = state.getBlock().getSoundType(state, this.level(), blockResult.getBlockPos(), this).getStepSound();
                if (this.getDeltaMovement().length() > 0.1D) {
                    this.level().playSound(null, blockResult.getLocation().x, blockResult.getLocation().y, blockResult.getLocation().z,
                            event, SoundSource.AMBIENT, 2.0F, 1.0F);
                }
                state.onProjectileHit(this.level(), state, blockResult, this);
            }
            case ENTITY -> {
                Entity entity = ((EntityHitResult) result).getEntity();
                if (entity == this.getOwner() || entity == this.getVehicle()) return;
                if (this.getDeltaMovement().length() > 0.1D) {
                    entity.hurt(entity.damageSources().thrown(this, this.getOwner()), this.getHitDamage());
                }
            }
            default -> {
            }
        }
    }

    public record BounceResult(Vec3 location, Vec3 deltaMovement) {
    }

    public BounceResult doMultiBounce(Vec3 deltaMovement) {
        Vec3 start = this.position();
        Vec3 end = start.add(deltaMovement);
        Vec3 endVecOffset = new Vec3(deltaMovement.x, deltaMovement.y, deltaMovement.z);
        for (int i = 0; i < 3; i++) {
            HitResult hitResult = this.getHitResult(start, end, endVecOffset, this::canHitEntity, this.level());
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) hitResult;
                Vec3 hit = blockResult.getLocation();
                if (blockResult.getDirection() == Direction.UP && start.y() - hit.y() < 0.01D) {
                    hit = new Vec3(hit.x(), start.y(), hit.z());
                }
                if (i < 2) {
                    start = start.lerp(hit, 0.8D);
                    Vec3 rest = end.subtract(start);
                    endVecOffset = this.bounce(blockResult.getDirection(), rest);
                    end = start.add(endVecOffset);
                    deltaMovement = this.bounce(blockResult.getDirection(), deltaMovement);
                } else {
                    end = start.lerp(hit, 0.8D);
                    deltaMovement = Vec3.ZERO;
                }
            } else if (hitResult.getType() == HitResult.Type.ENTITY) {
                Entity entity = ((EntityHitResult) hitResult).getEntity();
                if (entity == this.getOwner() || entity == this.getVehicle()) break;

                Direction direction = Direction.getNearest(endVecOffset.x(), endVecOffset.y(), endVecOffset.z()).getOpposite();
                Vec3 hit = hitResult.getLocation();
                start = start.lerp(hit, 0.8D);
                Vec3 rest = end.subtract(start);
                endVecOffset = this.bounce(direction, rest);
                end = start.add(endVecOffset);
                deltaMovement = this.bounce(direction, deltaMovement);
            } else if (hitResult.getType() == HitResult.Type.MISS) {
                break;
            }
            this.onHit(hitResult);
        }
        return new BounceResult(end, deltaMovement);
    }

    public Vec3 bounce(Direction direction, Vec3 deltaMovement) {
        double factor = this.getBounceFactor();
        return switch (direction.getAxis()) {
            case X -> deltaMovement.multiply(-factor / 1.5D, factor, factor);
            case Y -> {
                Vec3 newVec = deltaMovement.multiply(factor, -factor / 2.5D, factor);
                if (newVec.y() < this.getGravity()) {
                    newVec = newVec.multiply(1.0D, 0.0D, 1.0D);
                }
                yield newVec;
            }
            case Z -> deltaMovement.multiply(factor, factor, -factor / 1.5D);
        };
    }

    public HitResult getHitResult(Vec3 start, Vec3 end, Vec3 endVecOffset, Predicate<Entity> filter, Level level) {
        HitResult hitResult = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hitResult.getType() != HitResult.Type.MISS) {
            end = hitResult.getLocation();
        }
        EntityHitResult entityHit = findEntityHitResult(level, this, start, end,
                this.getBoundingBox().expandTowards(endVecOffset).inflate(1.0D), filter);
        if (entityHit != null) {
            hitResult = entityHit;
        }
        return hitResult;
    }

    @Nullable
    public static EntityHitResult findEntityHitResult(Level level, Entity projectile, Vec3 start, Vec3 end,
                                                     AABB boundingBox, Predicate<Entity> filter) {
        double best = Double.MAX_VALUE;
        Entity found = null;
        Vec3 hitPos = null;
        for (Entity candidate : level.getEntities(projectile, boundingBox, filter)) {
            AABB aabb = candidate.getBoundingBox().inflate(0.3D);
            Optional<Vec3> optional = aabb.clip(start, end);
            if (optional.isPresent()) {
                double distance = start.distanceToSqr(optional.get());
                if (distance < best) {
                    found = candidate;
                    best = distance;
                    hitPos = optional.get();
                }
            }
        }
        return found == null ? null : new EntityHitResult(found, hitPos);
    }


    @Override
    public void tick() {
        super.tick();
        BounceResult result = this.doMultiBounce(this.getDeltaMovement());

        this.checkInsideBlocks();

        Vec3 movement = result.deltaMovement();
        double x = result.location().x();
        double y = result.location().y();
        double z = result.location().z();

        this.setDeltaMovement(movement);
        this.updateRotation();

        float damping;
        if (this.isInWater()) {
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(ParticleTypes.BUBBLE,
                        x - movement.x * 0.25D, y - movement.y * 0.25D, z - movement.z * 0.25D,
                        movement.x, movement.y, movement.z);
            }
            damping = 0.8F;
        } else {
            damping = 0.99F;
        }

        this.setDeltaMovement(movement.scale(damping));
        if (!this.isNoGravity()) {
            Vec3 current = this.getDeltaMovement();
            this.setDeltaMovement(current.x, current.y - this.getGravity(), current.z);
        }

        this.setPos(x, y, z);

        if (this.life >= 0 && this.tickCount >= this.life && !this.level().isClientSide()) {
            this.onDeath(null);
        }

        this.onTossTick();
    }

    protected void onTossTick() {
        if (this.level().isClientSide() && this.getTailParticle() != null) {
            this.level().addParticle(this.getTailParticle(), true,
                    this.getX(), this.getY() + 0.35D, this.getZ(), 0.0D, 0.01D, 0.0D);
        }
    }

    public void onDeath(@Nullable HitResult hitResult) {
        if (!this.level().isClientSide()) {
            this.onExpire();
        }
        this.discard();
    }

    protected void onExpire() {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = this.getBoundingBox().getSize() * 4.0D;
        if (Double.isNaN(size)) {
            size = 4.0D;
        }
        size *= 64.0D;
        return distance < size * size;
    }


    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeInt(this.life);
        buffer.writeFloat(this.gravity);
        buffer.writeDouble(this.bounceFactor);
        buffer.writeBoolean(this.shouldBounce);
        buffer.writeBoolean(this.brokeOnGround);
        if (this.tailParticle != null) {
            buffer.writeBoolean(true);
            EntityDataSerializers.PARTICLE.write(buffer, this.tailParticle);
        } else {
            buffer.writeBoolean(false);
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.life = additionalData.readInt();
        this.gravity = additionalData.readFloat();
        this.bounceFactor = additionalData.readDouble();
        this.shouldBounce = additionalData.readBoolean();
        this.brokeOnGround = additionalData.readBoolean();
        if (additionalData.readBoolean()) {
            this.tailParticle = EntityDataSerializers.PARTICLE.read(additionalData);
        } else {
            this.tailParticle = null;
        }
    }


    public float getGravity() {
        return this.gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public double getBounceFactor() {
        return this.bounceFactor;
    }

    public void setBounceFactor(double bounceFactor) {
        this.bounceFactor = bounceFactor;
    }

    public boolean shouldBounce() {
        return this.shouldBounce;
    }

    public void setShouldBounce(boolean shouldBounce) {
        this.shouldBounce = shouldBounce;
    }

    public int getLife() {
        return this.life;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public float getHitDamage() {
        return this.hitDamage;
    }

    public void setHitDamage(float hitDamage) {
        this.hitDamage = hitDamage;
    }

    public boolean isBrokeOnGround() {
        return this.brokeOnGround;
    }

    public void setBrokeOnGround(boolean brokeOnGround) {
        this.brokeOnGround = brokeOnGround;
    }

    @Nullable
    public ParticleOptions getTailParticle() {
        return this.tailParticle;
    }

    public void setTailParticle(@Nullable ParticleOptions tailParticle) {
        this.tailParticle = tailParticle;
    }
}
