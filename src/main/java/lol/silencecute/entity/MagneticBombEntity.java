package lol.silencecute.entity;

import lol.silencecute.api.ModDamageTypes;
import lol.silencecute.entity.throwable.LrtStickyThrowableEntity;
import lol.silencecute.event.TempstPassiveService;
import lol.silencecute.registry.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class MagneticBombEntity extends LrtStickyThrowableEntity {

    public static int FUSE_TICKS = 3 * 20;

    public static int AIR_TIMEOUT_TICKS = 20 * 60;

    public static float MAX_DAMAGE = 100.0F;

    public static double DAMAGE_RADIUS = 6.0D;

    public static float GRAVITY = 0.07F;

    public static double KNOCKBACK_HORIZONTAL = 0.8D;

    public static double KNOCKBACK_VERTICAL = 0.25D;

    private int fuseElapsed;

    private boolean exploded;

    private boolean armed;

    private int fuseStart;

    private boolean countdownPlayed;

    public MagneticBombEntity(EntityType<? extends MagneticBombEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setGravity(GRAVITY);
        this.setShouldBounce(false);
        this.setLife(-1);
        this.setHitDamage(0.0F);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() || this.exploded) return;

        this.fuseElapsed++;
        if (!this.armed && this.fuseElapsed >= AIR_TIMEOUT_TICKS) {
            this.discard();
            return;
        }
        if (!this.armed && this.isStuck()) {
            this.armed = true;
            this.fuseStart = this.fuseElapsed;
            this.playCountdown();
        }
        if (this.armed && this.fuseElapsed - this.fuseStart >= FUSE_TICKS) {
            this.explode();
        }
    }


    @Override
    protected void onTossTick() {
        if (this.isStuck()) return;
        this.spawnSmoke(false);
    }

    @Override
    protected void onStuckTick() {
        if (this.level().isClientSide()) return;
        if (this.getStuckFace() != null) return;
        this.spawnSmoke(true);
    }

    private void spawnSmoke(boolean whileStationary) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (!whileStationary && this.getDeltaMovement().lengthSqr() < 1.0E-4D) return;
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                this.getX(), this.getY() + 0.06D, this.getZ(),
                1, 0.01D, 0.01D, 0.01D, 0.0D);
    }


    private void playCountdown() {
        if (this.countdownPlayed) return;
        this.countdownPlayed = true;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.C4_READY.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
    }

    @Override
    protected void onStuckToBlock(BlockHitResult blockResult) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.AMBIENT, 2.0F, 1.0F);
    }

    @Override
    protected void onStuckEntityLost() {
        this.explode();
    }


    private void explode() {
        if (this.exploded || this.isRemoved() || !(this.level() instanceof ServerLevel serverLevel)) return;
        this.exploded = true;

        Vec3 center = this.position();
        Entity owner = this.getOwner();
        DamageSource source = ModDamageTypes.explosion(this.level(), this, owner);

        TempstPassiveService.triggerFromExplosion(serverLevel, center, owner);

        AABB area = AABB.ofSize(center, DAMAGE_RADIUS * 2.0D, DAMAGE_RADIUS * 2.0D, DAMAGE_RADIUS * 2.0D);
        Entity stuckEntity = this.getStuckEntity();
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            double distance = Math.sqrt(target.getBoundingBox().distanceToSqr(center));
            if (distance > DAMAGE_RADIUS) continue;

            boolean stuckTarget = target == stuckEntity;
            boolean fullDamage = stuckTarget || isStandingOnBomb(target, center);
            float damage = fullDamage ? MAX_DAMAGE
                    : (float) (MAX_DAMAGE * (1.0D - distance / DAMAGE_RADIUS));

            if (damage > 0.0F && target.hurt(source, damage) && !stuckTarget) {
                applyKnockback(target, center, damage / MAX_DAMAGE);
            }
        }

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z,
                1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z,
                20, 0.75, 0.75, 0.75, 0.04);
        this.level().playSound(null, center.x, center.y, center.z,
                ModSounds.C4_BOOM.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
        this.discard();
    }

    private static boolean isStandingOnBomb(LivingEntity target, Vec3 center) {
        AABB box = target.getBoundingBox();
        boolean horizontallyAbove = center.x >= box.minX && center.x <= box.maxX
                && center.z >= box.minZ && center.z <= box.maxZ;
        return horizontallyAbove && box.minY >= center.y - 0.2D && box.minY <= center.y + 0.65D;
    }

    private static void applyKnockback(LivingEntity target, Vec3 center, float strength) {
        Vec3 away = target.position().subtract(center);
        double horizontal = Math.sqrt(away.x * away.x + away.z * away.z);
        if (horizontal < 1.0E-4D) return;
        target.push(away.x / horizontal * KNOCKBACK_HORIZONTAL * strength,
                KNOCKBACK_VERTICAL * strength,
                away.z / horizontal * KNOCKBACK_HORIZONTAL * strength);
        target.hurtMarked = true;
    }


    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FuseElapsed", this.fuseElapsed);
        tag.putBoolean("Armed", this.armed);
        tag.putInt("FuseStart", this.fuseStart);
        tag.putBoolean("CountdownPlayed", this.countdownPlayed);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.fuseElapsed = tag.getInt("FuseElapsed");
        this.armed = tag.getBoolean("Armed");
        this.fuseStart = tag.getInt("FuseStart");
        this.countdownPlayed = tag.getBoolean("CountdownPlayed");
    }
}
