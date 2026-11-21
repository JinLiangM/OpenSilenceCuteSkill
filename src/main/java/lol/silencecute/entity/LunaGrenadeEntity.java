package lol.silencecute.entity;

import lol.silencecute.event.LunaPassiveService;
import lol.silencecute.manager.LunaGrenadeService;
import lol.silencecute.util.LunaGrenadeBallistics;
import lol.silencecute.api.ModDamageTypes;

import lol.silencecute.registry.ModSounds;

import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.network.protocol.Packet;

import net.minecraft.network.protocol.game.ClientGamePacketListener;

import net.minecraft.network.syncher.EntityDataAccessor;

import net.minecraft.network.syncher.EntityDataSerializers;

import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundEvents;

import net.minecraft.sounds.SoundSource;

import net.minecraft.world.damagesource.DamageSource;

import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.EquipmentSlot;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.projectile.ThrowableProjectile;

import net.minecraft.world.item.ArmorItem;

import net.minecraft.world.item.ArmorMaterials;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.Items;

import net.minecraft.world.level.Explosion;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;

import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.EntityHitResult;

import net.minecraft.world.phys.Vec3;

import net.minecraftforge.network.NetworkHooks;

public final class LunaGrenadeEntity extends ThrowableProjectile {

    public static double DAMAGE_RADIUS = 8.0D;

    

    public float spinRot = 0.0F;

    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(

            LunaGrenadeEntity.class, EntityDataSerializers.INT);

    private static final double BOUNCE_FACTOR = 0.48D;

    private static final double FLOOR_FRICTION = 0.76D;

    private static final double MAX_BOUNCE_VELOCITY_Y = 0.30D;

    public LunaGrenadeEntity(EntityType<? extends LunaGrenadeEntity> type, Level level) {

        super(type, level);

    }

    @Override

    protected void defineSynchedData() {

        this.entityData.define(FUSE, LunaGrenadeService.FULL_FUSE_TICKS);

    }

    public void setFuse(int fuseTicks) {

        this.entityData.set(FUSE, Math.max(1, fuseTicks));

    }

    public int getFuse() {

        return this.entityData.get(FUSE);

    }

    @Override

    protected float getGravity() {

        return LunaGrenadeBallistics.GRAVITY;

    }

    @Override
    public void tick() {
        super.tick();

        if (getDeltaMovement().lengthSqr() > 0.01D) {
            spinRot += 14.0F;
        }

        if (isNoGravity() && getDeltaMovement().lengthSqr() < 0.01D) {
            Vec3 start = this.position();

            Vec3 end = start.subtract(0.0D, 0.1D, 0.0D);

            net.minecraft.world.phys.BlockHitResult hitResult = level().clip(
                    new net.minecraft.world.level.ClipContext(start, end,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, this));

            if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {

                setNoGravity(false);

                setDeltaMovement(0.0D, -0.05D, 0.0D);
            }
        }

        if (level().isClientSide()) {
            if (getFuse() <= 20 && tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.08D, getZ(),
                        0.0D, 0.015D, 0.0D);
            }
            return;
        }
        int fuse = getFuse() - 1;
        this.entityData.set(FUSE, fuse);
        if (fuse <= 0) {
            explode();
        }
    }

    @Override

    protected void onHitBlock(BlockHitResult hit) {

        if (level().isClientSide()) return;

        Vec3 movement = getDeltaMovement();

        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());

        double normalVelocity = movement.dot(normal);

        Vec3 bounced = movement.subtract(normal.scale(2.0D * normalVelocity)).scale(BOUNCE_FACTOR);

        if (hit.getDirection().getAxis().isVertical()) {

            bounced = new Vec3(bounced.x * FLOOR_FRICTION, bounced.y, bounced.z * FLOOR_FRICTION);

            if (Math.abs(bounced.y) < 0.15D) {

                bounced = new Vec3(bounced.x, 0, bounced.z);

            }

        }

        if (bounced.y > MAX_BOUNCE_VELOCITY_Y) {

            bounced = new Vec3(bounced.x, MAX_BOUNCE_VELOCITY_Y, bounced.z);

        }

        if (bounced.lengthSqr() < 0.015D) {
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);

            setPos(hit.getLocation().add(normal.scale(0.05D)));
        } else {

            setNoGravity(false);

            setDeltaMovement(bounced);

            this.hasImpulse = true;

            setPos(hit.getLocation().add(normal.scale(0.05D)));

            if (bounced.lengthSqr() > 0.05D) {

                level().playSound(null, blockPosition(), SoundEvents.STONE_HIT, SoundSource.PLAYERS,

                        0.55F, 1.25F + random.nextFloat() * 0.2F);

            }

        }

    }

    @Override

    protected void onHitEntity(EntityHitResult hit) {

        if (level().isClientSide() || hit.getEntity() == getOwner() && tickCount < 5) return;

        Vec3 movement = getDeltaMovement();

        setDeltaMovement(movement.scale(-0.32D).add(0.0D, 0.08D, 0.0D));

        this.hasImpulse = true;

        setNoGravity(false);

        level().playSound(null, blockPosition(), SoundEvents.STONE_HIT, SoundSource.PLAYERS,

                0.45F, 1.4F);

    }

    private void explode() {

        if (isRemoved() || !(level() instanceof ServerLevel serverLevel)) return;

        Vec3 center = position();

        Entity owner = getOwner();

        DamageSource source = ModDamageTypes.explosion(level(), this, owner);

        AABB area = AABB.ofSize(center, DAMAGE_RADIUS * 2.0D, DAMAGE_RADIUS * 2.0D, DAMAGE_RADIUS * 2.0D);

        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {

            if (isProtectedTeammate(owner, target)) continue;

            if (Explosion.getSeenPercent(center, target) <= 0.01D) continue;

            double distance = target.position().distanceTo(center);

            if (distance > DAMAGE_RADIUS) continue;

            if (target instanceof Player player && (player.isCreative() || player.isSpectator())) continue;

            if (target instanceof Player player) {

                handlePlayerDamage(player, distance, source, center);

            } else {

                applyTrueDamage(target, 40.0F, source, center);

            }

        }

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

        lol.silencecute.event.TempstPassiveService.triggerFromExplosion(serverLevel, center, getOwner());

        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 36, 1.15D, 1.15D, 1.15D, 0.06D);

        serverLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 28, 1.0D, 0.8D, 1.0D, 0.08D);

        level().playSound(null, center.x, center.y, center.z, ModSounds.LUNA_GRENADE_EXPLOSION.get(), SoundSource.BLOCKS, 3.5F, 1.0F);

        discard();

    }

    private void handlePlayerDamage(Player player, double distance, DamageSource source, Vec3 center) {

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        ArmorItem armorItem = chest.getItem() instanceof ArmorItem a ? a : null;

        boolean hasNetherite = armorItem != null && armorItem.getMaterial() == ArmorMaterials.NETHERITE;

        boolean hasDiamond = armorItem != null && armorItem.getMaterial() == ArmorMaterials.DIAMOND;

        boolean hasIron = armorItem != null && armorItem.getMaterial() == ArmorMaterials.IRON;

        float maxHealth = player.getMaxHealth();

        float damageToDeal = 0.0F;

        float armorKeepPercent = -1.0F;

        if (distance <= 2.0D) {

            if (hasNetherite) {

                damageToDeal = maxHealth * 0.35F; armorKeepPercent = 0.5F;

                if (player.getHealth() - damageToDeal < maxHealth * 0.2F) damageToDeal = Math.max(0, player.getHealth() - maxHealth * 0.2F);

            } else if (hasDiamond) {

                damageToDeal = player.getHealth() * 0.5F; armorKeepPercent = 0.2F;

            } else {

                damageToDeal = 10000F; armorKeepPercent = 0.0F;

            }

        } else if (distance <= 4.0D) {

            if (hasNetherite || hasDiamond) { damageToDeal = maxHealth * 0.20F; armorKeepPercent = 0.8F; }

            else { damageToDeal = maxHealth * 0.70F; armorKeepPercent = 0.3F; }

        } else if (distance <= 6.0D) {

            if (hasNetherite || hasDiamond || hasIron) { damageToDeal = maxHealth * 0.40F; armorKeepPercent = 0.7F; }

            else { damageToDeal = maxHealth * 0.50F; armorKeepPercent = 0.5F; }

        } else {

            damageToDeal = maxHealth * 0.50F; armorKeepPercent = 0.9F;

        }

        if (armorKeepPercent >= 0.0F) {

            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST}) {

                ItemStack stack = player.getItemBySlot(slot);

                if (!stack.isEmpty() && stack.isDamageableItem()) {

                    int maxDam = stack.getMaxDamage();

                    int curDam = stack.getDamageValue();

                    int curDur = maxDam - curDam;

                    int newDur = (int) (curDur * armorKeepPercent);

                    stack.setDamageValue(maxDam - newDur);

                }

            }

        }

        applyTrueDamage(player, damageToDeal, source, center);

    }

    private void applyTrueDamage(LivingEntity target, float damage, DamageSource source, Vec3 center) {

        target.invulnerableTime = 0;

        float currentHealth = target.getHealth();

        float finalHealth = currentHealth - damage;

        if (finalHealth <= 0.0F) {

            boolean hurtSuccess = target.hurt(source, currentHealth + 50.0F);

            if (!hurtSuccess && target.isAlive()) {

                boolean hasTotem = false;

                if (target instanceof Player p) {

                    hasTotem = p.getMainHandItem().is(Items.TOTEM_OF_UNDYING) || p.getOffhandItem().is(Items.TOTEM_OF_UNDYING);

                }

                if (!hasTotem) {

                    target.setHealth(0.0F);

                    target.die(source);

                }

            }

        } else {

            target.hurt(source, 0.1F);

            target.setHealth(finalHealth);

        }

        float knockbackStrength = Math.min(1.0F, damage / 40.0F);

        applyKnockback(target, center, Math.max(0.5F, knockbackStrength));

        if (getOwner() instanceof ServerPlayer luna && target != getOwner()) {

            LunaPassiveService.revealGrenadeTarget(luna, target);

        }

    }

    private static boolean isProtectedTeammate(Entity owner, LivingEntity target) {

        return owner != null && target != owner && owner.isAlliedTo(target);

    }

    private static void applyKnockback(LivingEntity target, Vec3 center, float strength) {

        Vec3 away = target.position().subtract(center);

        double horizontal = Math.sqrt(away.x * away.x + away.z * away.z);

        if (horizontal < 1.0E-4D) return;

        target.push(away.x / horizontal * 1.25D * strength, 0.48D * strength, away.z / horizontal * 1.25D * strength);

        target.hurtMarked = true;

    }

    @Override

    protected void addAdditionalSaveData(CompoundTag tag) {

        super.addAdditionalSaveData(tag);

        tag.putInt("Fuse", getFuse());

    }

    @Override

    protected void readAdditionalSaveData(CompoundTag tag) {

        super.readAdditionalSaveData(tag);

        if (tag.contains("Fuse")) setFuse(tag.getInt("Fuse"));

    }

    @Override

    public Packet<ClientGamePacketListener> getAddEntityPacket() {

        return NetworkHooks.getEntitySpawningPacket(this);

    }

}

