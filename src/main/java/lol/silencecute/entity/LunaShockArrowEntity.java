package lol.silencecute.entity;



import lol.silencecute.event.LunaPassiveService;
import lol.silencecute.event.OperatorState;
import lol.silencecute.manager.LunaShockArrowService;
import java.util.UUID;



import lol.silencecute.api.ModDamageTypes;

import lol.silencecute.registry.ModSounds;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import net.minecraft.world.level.ClipContext;

import net.minecraft.world.phys.HitResult;

import net.minecraft.network.protocol.Packet;

import net.minecraft.network.protocol.game.ClientGamePacketListener;

import net.minecraft.network.syncher.EntityDataAccessor;

import net.minecraft.network.syncher.EntityDataSerializers;

import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundSource;

import net.minecraft.world.damagesource.DamageSource;

import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.projectile.ThrowableProjectile;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;

import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.EntityHitResult;

import net.minecraft.world.phys.Vec3;

import net.minecraftforge.network.NetworkHooks;





public final class LunaShockArrowEntity extends ThrowableProjectile {

    

    public static float GRAVITY = 0.060F;

    

    public static int MAX_FLIGHT_TICKS = 130;

    

    public static int MAX_BOUNCES = 1;

    public static double BOUNCE_FACTOR = 0.62D;



    private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(

            LunaShockArrowEntity.class, EntityDataSerializers.BOOLEAN);



    private static final EntityDataAccessor<Float> LANDED_YAW = SynchedEntityData.defineId(

            LunaShockArrowEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> LANDED_PITCH = SynchedEntityData.defineId(

            LunaShockArrowEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> ATTACHED_ID = SynchedEntityData.defineId(

            LunaShockArrowEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> ATTACH_X = SynchedEntityData.defineId(

            LunaShockArrowEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> ATTACH_Y = SynchedEntityData.defineId(

            LunaShockArrowEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> ATTACH_Z = SynchedEntityData.defineId(

            LunaShockArrowEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> ATTACH_BODY_YAW = SynchedEntityData.defineId(

            LunaShockArrowEntity.class, EntityDataSerializers.FLOAT);

    private UUID attachedUuid;

    

    private Vec3 landedPosition;


    private BlockPos attachedBlockPos;


    

    private int dischargeAge;

    

    private int bouncesLeft;



    public LunaShockArrowEntity(EntityType<? extends LunaShockArrowEntity> type, Level level) {

        super(type, level);

    }



    

    public void setBounces(int bounces) {

        this.bouncesLeft = Math.max(0, Math.min(MAX_BOUNCES, bounces));

    }



    public boolean isLanded() {

        return this.entityData.get(LANDED);

    }



    private void setLanded(boolean landed) {

        this.entityData.set(LANDED, landed);

    }



    @Override

    protected void defineSynchedData() {

        this.entityData.define(LANDED, false);

        this.entityData.define(LANDED_YAW, 0.0F);

        this.entityData.define(LANDED_PITCH, 0.0F);

        this.entityData.define(ATTACHED_ID, -1);

        this.entityData.define(ATTACH_X, 0.0F);

        this.entityData.define(ATTACH_Y, 0.0F);

        this.entityData.define(ATTACH_Z, 0.0F);

        this.entityData.define(ATTACH_BODY_YAW, 0.0F);

    }



    public boolean isAttached() {

        return this.entityData.get(ATTACHED_ID) >= 0 || this.attachedUuid != null;

    }



    private Entity attachedTarget() {

        int targetId = this.entityData.get(ATTACHED_ID);

        Entity target = targetId < 0 ? null : level().getEntity(targetId);

        if (!level().isClientSide() && attachedUuid != null

                && (target == null || !attachedUuid.equals(target.getUUID()))

                && level() instanceof ServerLevel server) {

            target = server.getEntity(attachedUuid);

            if (target != null) this.entityData.set(ATTACHED_ID, target.getId());

        }

        return target;

    }



    private static float bodyYaw(Entity target, float partialTick) {

        return target instanceof LivingEntity living

                ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot)

                : Mth.rotLerp(partialTick, target.yRotO, target.getYRot());

    }



    

    public Vec3 attachedRenderPosition(float partialTick) {

        Entity target = attachedTarget();

        if (target == null || !target.isAlive()) return null;

        float angle = bodyYaw(target, partialTick) - this.entityData.get(ATTACH_BODY_YAW);

        Vec3 offset = new Vec3(this.entityData.get(ATTACH_X), this.entityData.get(ATTACH_Y),

                this.entityData.get(ATTACH_Z)).yRot(-angle * Mth.DEG_TO_RAD);

        return target.getPosition(partialTick).add(offset);

    }



    public float attachedRenderYaw(float partialTick) {

        Entity target = attachedTarget();


        return this.entityData.get(LANDED_YAW) - (target == null ? 0.0F

                : bodyYaw(target, partialTick) - this.entityData.get(ATTACH_BODY_YAW));

    }



    private void followAttachedTarget() {

        if (attachedUuid == null && this.entityData.get(ATTACHED_ID) < 0) return;

        Vec3 attachedPosition = attachedRenderPosition(1.0F);

        if (attachedPosition != null) {

            this.landedPosition = attachedPosition;

            setPos(attachedPosition);

        } else if (!level().isClientSide()) {


            discard();

        }

    }



    @Override

    protected float getGravity() {

        return isLanded() ? 0.0F : GRAVITY;

    }



    @Override

    public void tick() {

        if (!level().isClientSide() && isLanded() && this.attachedBlockPos != null
                && level().getBlockState(this.attachedBlockPos).isAir()) {
            releaseFromSupport();
        }

        if (isLanded()) {


            this.baseTick();

        } else {

            super.tick();

        }

        if (isLanded()) {

            this.setDeltaMovement(Vec3.ZERO);

            followAttachedTarget();

            if (isRemoved()) return;

            if (!level().isClientSide() && landedPosition != null) {

                setPos(landedPosition);

            }

            setYRot(attachedRenderYaw(1.0F));

            setXRot(this.entityData.get(LANDED_PITCH));

            this.yRotO = getYRot();

            this.xRotO = getXRot();

            if (level().isClientSide()) {

                return;

            }

            this.dischargeAge++;

            if (this.dischargeAge % LunaShockArrowService.PULSE_TICKS == 0) {

                pulseDamage();

            }

            if (this.dischargeAge >= LunaShockArrowService.DISCHARGE_TICKS) {

                discard();

            }

            return;

        }

        if (level().isClientSide()) {

            return;

        }

        if (this.tickCount >= MAX_FLIGHT_TICKS) {

            land();

        }

    }



    @Override

    protected void onHitBlock(BlockHitResult hit) {

        if (level().isClientSide()) {

            return;

        }

        if (this.bouncesLeft > 0) {

            this.bouncesLeft--;

            Vec3 movement = getDeltaMovement();

            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());

            double normalVelocity = movement.dot(normal);

            Vec3 bounced = movement.subtract(normal.scale(2.0D * normalVelocity)).scale(BOUNCE_FACTOR);

            setDeltaMovement(bounced);

            this.hasImpulse = true;

            setPos(hit.getLocation().add(normal.scale(0.06D)));

            return;

        }


        setPos(hit.getLocation().subtract(getDeltaMovement().normalize().scale(0.18D)));

        this.attachedBlockPos = hit.getBlockPos().immutable();
        land();

    }



    @Override

    protected void onHitEntity(EntityHitResult hit) {

        if (level().isClientSide()) {

            return;

        }

        Entity target = hit.getEntity();

        Entity owner = getOwner();

        if (target == owner && this.tickCount < 4) {

            return;

        }

        Vec3 movement = getDeltaMovement();

        Vec3 direction = movement.normalize();

        AABB box = target.getBoundingBox();

        Vec3 contact = box.clip(position(), position().add(movement)).orElse(hit.getLocation());


        contact = new Vec3(Mth.clamp(contact.x, box.minX, box.maxX),

                Mth.clamp(contact.y, box.minY, box.maxY),

                Mth.clamp(contact.z, box.minZ, box.maxZ));


        BlockHitResult obstruction = level().clip(new ClipContext(position(), contact,

                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (obstruction.getType() == HitResult.Type.BLOCK) {

            onHitBlock(obstruction);

            return;

        }

        if (target instanceof LivingEntity living && canDamage(owner, living)) {

            hitLiving(living);

        }

        if (!target.isAlive()) {

            discard();

            return;

        }


        setPos(contact.subtract(direction.scale(0.08D)));

        land();

        Vec3 offset = position().subtract(target.position());

        this.attachedBlockPos = null;
        this.attachedUuid = target.getUUID();

        this.entityData.set(ATTACHED_ID, target.getId());

        this.entityData.set(ATTACH_X, (float) offset.x);

        this.entityData.set(ATTACH_Y, (float) offset.y);

        this.entityData.set(ATTACH_Z, (float) offset.z);

        this.entityData.set(ATTACH_BODY_YAW, bodyYaw(target, 1.0F));

    }



    

    private void land() {

        if (level().isClientSide()) {

            return;

        }

        Vec3 movement = getDeltaMovement();

        if (movement.lengthSqr() > 1.0E-8D) {

            setYRot((float) Math.toDegrees(Math.atan2(movement.x, movement.z)));

            setXRot((float) Math.toDegrees(Math.atan2(movement.y, movement.horizontalDistance())));

        }

        this.entityData.set(LANDED_YAW, getYRot());

        this.entityData.set(LANDED_PITCH, getXRot());

        this.landedPosition = position();

        setLanded(true);

        setDeltaMovement(Vec3.ZERO);

        setNoGravity(true);

        this.noPhysics = true;


        level().playSound(null, this.getX(), this.getY(), this.getZ(),

                ModSounds.LUNA_SHOCK_DISCHARGE.get(), SoundSource.PLAYERS, 2.0F, 0.9F);

    }



    

    private void releaseFromSupport() {
        this.attachedBlockPos = null;
        this.landedPosition = null;
        this.dischargeAge = 0;
        this.tickCount = 0;
        this.noPhysics = false;
        setNoGravity(false);
        setDeltaMovement(0.0D, -0.02D, 0.0D);
        setLanded(false);
    }

    private void pulseDamage() {

        if (!(level() instanceof ServerLevel)) {

            return;

        }

        Vec3 center = position();

        Entity owner = getOwner();

        AABB area = AABB.ofSize(center,

                LunaShockArrowService.FIELD_RADIUS * 2.0D,

                LunaShockArrowService.FIELD_RADIUS * 2.0D,

                LunaShockArrowService.FIELD_RADIUS * 2.0D);

        for (LivingEntity living : level().getEntitiesOfClass(

                LivingEntity.class, area, LivingEntity::isAlive)) {

            if (!canDamage(owner, living)) {

                continue;

            }

            if (living.position().distanceToSqr(center)

                    > LunaShockArrowService.FIELD_RADIUS * LunaShockArrowService.FIELD_RADIUS) {

                continue;

            }

            if (hasUnblockedPath(living)) {

                hitLiving(living);

            }

        }

    }



    

    private boolean hasUnblockedPath(LivingEntity living) {

        Vec3 center = position();

        AABB box = living.getBoundingBox();

        for (double fraction : new double[] {0.2D, 0.5D, 0.85D}) {

            Vec3 point = new Vec3((box.minX + box.maxX) * 0.5D,

                    box.minY + box.getYsize() * fraction, (box.minZ + box.maxZ) * 0.5D);

            if (level().clip(new ClipContext(center, point, ClipContext.Block.COLLIDER,

                    ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {

                return true;

            }

        }

        return false;

    }



    private boolean canDamage(Entity owner, LivingEntity living) {

        if (living.isDeadOrDying()) {

            return false;

        }

        if (living instanceof Player player) {

            return !player.isCreative() && !player.isSpectator();

        }

        return true;

    }



    

    private void hitLiving(LivingEntity living) {

        DamageSource source = ModDamageTypes.shock(level(), this, getOwner());

        Vec3 movementBeforeHit = living.getDeltaMovement();

        int invulnerableTimeBeforeHit = living.invulnerableTime;




        float rawDamage = living.getMaxHealth() * LunaShockArrowService.MAX_HEALTH_DAMAGE_RATIO;

        float damage = Mth.clamp(rawDamage, LunaShockArrowService.MIN_DAMAGE, LunaShockArrowService.MAX_DAMAGE);



        boolean hurt = living.hurt(source, damage);

        living.invulnerableTime = invulnerableTimeBeforeHit;

        if (hurt) {

            living.setDeltaMovement(movementBeforeHit);

            living.hurtMarked = true;

        }

        if (hurt && living != getOwner()

                && getOwner() instanceof ServerPlayer luna

                && OperatorState.isLuna(luna.getUUID())) {

            LunaPassiveService.revealTarget(luna, living);

            LunaPassiveService.playShockArrowVoice(luna);

        }

    }



    @Override

    protected void addAdditionalSaveData(CompoundTag tag) {

        super.addAdditionalSaveData(tag);

        tag.putBoolean("Landed", isLanded());

        tag.putInt("DischargeAge", this.dischargeAge);

        tag.putInt("BouncesLeft", this.bouncesLeft);

        tag.putFloat("LandedYaw", this.entityData.get(LANDED_YAW));

        tag.putFloat("LandedPitch", this.entityData.get(LANDED_PITCH));

        if (this.attachedUuid != null) {

            tag.putUUID("AttachedTarget", this.attachedUuid);

            tag.putFloat("AttachX", this.entityData.get(ATTACH_X));

            tag.putFloat("AttachY", this.entityData.get(ATTACH_Y));

            tag.putFloat("AttachZ", this.entityData.get(ATTACH_Z));

            tag.putFloat("AttachBodyYaw", this.entityData.get(ATTACH_BODY_YAW));

        }

    }



    @Override

    protected void readAdditionalSaveData(CompoundTag tag) {

        super.readAdditionalSaveData(tag);

        setLanded(tag.getBoolean("Landed"));

        this.dischargeAge = tag.getInt("DischargeAge");

        this.bouncesLeft = Math.max(0, Math.min(MAX_BOUNCES, tag.getInt("BouncesLeft")));

        this.entityData.set(LANDED_YAW, tag.contains("LandedYaw") ? tag.getFloat("LandedYaw") : getYRot());

        this.entityData.set(LANDED_PITCH, tag.contains("LandedPitch") ? tag.getFloat("LandedPitch") : getXRot());

        if (tag.hasUUID("AttachedTarget")) {

            this.attachedUuid = tag.getUUID("AttachedTarget");

            this.entityData.set(ATTACH_X, tag.getFloat("AttachX"));

            this.entityData.set(ATTACH_Y, tag.getFloat("AttachY"));

            this.entityData.set(ATTACH_Z, tag.getFloat("AttachZ"));

            this.entityData.set(ATTACH_BODY_YAW, tag.getFloat("AttachBodyYaw"));

        }

        if (isLanded()) {

            this.landedPosition = position();

            setNoGravity(true);

            this.noPhysics = true;

        }

    }



    @Override

    public Packet<ClientGamePacketListener> getAddEntityPacket() {

        return NetworkHooks.getEntitySpawningPacket(this);

    }

}
