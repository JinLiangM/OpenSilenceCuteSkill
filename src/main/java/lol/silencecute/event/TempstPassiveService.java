package lol.silencecute.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lol.silencecute.api.ModDamageTypes;
import lol.silencecute.registry.ModEffects;
import lol.silencecute.registry.ModSounds;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TempstPassiveService {

    public static final double TRIGGER_RADIUS = 3.0D;

    public static final double EXPLOSION_RADIUS = 5.0D;

    public static final int COOLDOWN_TICKS = 60;

    public static final int BOOST_TICKS = 30;

    public static final double SWIM_SPEED_BONUS = 0.20D;

    private static final UUID SWIM_MODIFIER_ID =
            UUID.fromString("2c0a58e7-6f21-4b1f-9c4d-3f1a5d7c8e21");

    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private record TrackedBullet(ResourceKey<Level> dimension, Vec3 pos, boolean moving, UUID owner) {
    }

    private static final Map<Integer, TrackedBullet> TACZ_TRACK = new HashMap<>();

    private TempstPassiveService() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        Entity entity = event.getEntity();
        if (!isTaczBullet(entity)) return;
        TACZ_TRACK.put(entity.getId(), new TrackedBullet(
                ((ServerLevel) event.getLevel()).dimension(),
                entity.position(),
                entity.getDeltaMovement().lengthSqr() > 0.005D,
                ownerUuidOf(entity)));
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!isTriggerProjectile(event.getProjectile())) return;
        if (event.getProjectile().level().isClientSide) return;

        HitResult hit = event.getRayTraceResult();
        if (hit == null) return;
        if (hit.getType() == HitResult.Type.ENTITY) return;

        Vec3 pos = hit.getLocation();
        if (pos == null) return;

        AABB area = new AABB(
                pos.x - TRIGGER_RADIUS, pos.y - TRIGGER_RADIUS, pos.z - TRIGGER_RADIUS,
                pos.x + TRIGGER_RADIUS, pos.y + TRIGGER_RADIUS, pos.z + TRIGGER_RADIUS);
        double radiusSqr = TRIGGER_RADIUS * TRIGGER_RADIUS;

        UUID thrower = ownerUuidOf(event.getProjectile());

        for (ServerPlayer player : event.getProjectile().level()
                .getEntitiesOfClass(ServerPlayer.class, area)) {
            if (!OperatorState.isJifeng(player.getUUID())) continue;
            if (player.distanceToSqr(pos) > radiusSqr) continue;
            if (thrower != null && thrower.equals(player.getUUID())) continue;
            tryTrigger(player);
        }
    }

    private static boolean isTriggerProjectile(Entity entity) {
        return entity instanceof AbstractArrow
                || entity instanceof ThrownEgg
                || entity instanceof Snowball
                || isTaczBullet(entity);
    }

    private static boolean isTaczBullet(Entity entity) {
        if (entity == null) return false;
        String name = entity.getClass().getName();
        return name.startsWith("com.tacz.") || name.contains("KineticBullet");
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (TACZ_TRACK.isEmpty()) return;

        ResourceKey<Level> dimension = level.dimension();
        Iterator<Map.Entry<Integer, TrackedBullet>> iterator = TACZ_TRACK.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrackedBullet> entry = iterator.next();
            TrackedBullet tracked = entry.getValue();
            if (!dimension.equals(tracked.dimension())) continue;

            Entity entity = level.getEntity(entry.getKey());
            if (entity != null && !entity.isRemoved()) {
                if (!isTaczBullet(entity)) {
                    iterator.remove();
                    continue;
                }
                boolean moving = entity.getDeltaMovement().lengthSqr() > 0.005D;
                if (tracked.moving() && !moving) {
                    triggerNear(level, entity.position(), tracked.owner());
                }
                entry.setValue(new TrackedBullet(dimension, entity.position(), moving, tracked.owner()));
                continue;
            }

            iterator.remove();
            if (!tracked.moving()) continue;
            triggerNear(level, tracked.pos(), tracked.owner());
        }
    }

    private static void triggerNear(ServerLevel level, Vec3 pos, UUID owner) {
        AABB area = new AABB(pos, pos).inflate(TRIGGER_RADIUS);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            if (!OperatorState.isJifeng(player.getUUID())) continue;
            if (owner != null && owner.equals(player.getUUID())) continue;
            double distanceSqr = player.distanceToSqr(pos);
            if (distanceSqr > TRIGGER_RADIUS * TRIGGER_RADIUS) continue;
            if (distanceSqr < 1.0D) continue;
            tryTrigger(player);
        }
    }

    @SubscribeEvent
    public static void onProjectileHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!OperatorState.isJifeng(player.getUUID())) return;
        Entity direct = event.getSource().getDirectEntity();
        if (!isTriggerProjectile(direct)) return;
        UUID owner = ownerUuidOf(direct);
        if (owner == null || owner.equals(player.getUUID())) return;
        tryTrigger(player);
    }

    @SubscribeEvent
    public static void onExplosionHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                || event.getSource().is(ModDamageTypes.LUNA_EXPLOSION))) {
            return;
        }
        if (!OperatorState.isJifeng(player.getUUID())) return;
        if (isSelfCaused(event.getSource(), player)) return;
        tryTrigger(player);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity direct = event.getExplosion().getDirectSourceEntity();
        triggerFromExplosion(level, event.getExplosion().getPosition(),
                direct == null ? null : ownerUuidOf(direct));
    }

    public static void triggerFromExplosion(ServerLevel level, Vec3 center) {
        triggerFromExplosion(level, center, (UUID) null);
    }

    public static void triggerFromExplosion(ServerLevel level, Vec3 center, Entity source) {
        triggerFromExplosion(level, center, source == null ? null : ownerUuidOf(source));
    }

    public static void triggerFromExplosion(ServerLevel level, Vec3 center, UUID owner) {
        if (level == null || center == null) return;
        AABB area = AABB.ofSize(center,
                EXPLOSION_RADIUS * 2.0D, EXPLOSION_RADIUS * 2.0D, EXPLOSION_RADIUS * 2.0D);
        double radiusSqr = EXPLOSION_RADIUS * EXPLOSION_RADIUS;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            if (!OperatorState.isJifeng(player.getUUID())) continue;
            if (player.distanceToSqr(center) > radiusSqr) continue;
            if (owner != null && owner.equals(player.getUUID())) continue;
            tryTrigger(player);
        }
    }

    private static boolean isSelfCaused(net.minecraft.world.damagesource.DamageSource source, ServerPlayer player) {
        if (source == null) return false;
        UUID self = player.getUUID();
        UUID direct = ownerUuidOf(source.getDirectEntity());
        if (direct != null && direct.equals(self)) return true;
        UUID causing = ownerUuidOf(source.getEntity());
        return causing != null && causing.equals(self);
    }

    static UUID ownerUuidOf(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof ServerPlayer player) return player.getUUID();
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner != null) return owner.getUUID();
        }
        Entity reflected = reflectiveOwner(entity);
        return reflected == null ? null : reflected.getUUID();
    }

    private static Entity reflectiveOwner(Entity entity) {
        for (String name : new String[]{"getOwner", "getShooter"}) {
            try {
                java.lang.reflect.Method method = entity.getClass().getMethod(name);
                Object result = method.invoke(entity);
                if (result instanceof Entity owner) return owner;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        boolean active = OperatorState.isJifeng(player.getUUID())
                && (player.isInWater() || player.isSwimming());
        boolean hasModifier = speed.getModifier(SWIM_MODIFIER_ID) != null;

        if (active && !hasModifier) {
            speed.addTransientModifier(new AttributeModifier(SWIM_MODIFIER_ID,
                    "tempst_swim_speed", SWIM_SPEED_BONUS,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!active && hasModifier) {
            speed.removeModifier(SWIM_MODIFIER_ID);
        }
    }

    public static void triggerFromSkill(ServerPlayer player) {
        if (!OperatorState.isJifeng(player.getUUID())) return;
        tryTrigger(player);
    }

    private static void tryTrigger(ServerPlayer player) {
        if (player.hasEffect(ModEffects.KINETIC_BOOST.get())) return;

        long now = player.serverLevel().getGameTime();
        if (COOLDOWNS.getOrDefault(player.getUUID(), 0L) > now) return;

        COOLDOWNS.put(player.getUUID(), now + COOLDOWN_TICKS);

        player.addEffect(new MobEffectInstance(ModEffects.KINETIC_BOOST.get(),
                BOOST_TICKS, 0, false, false, false));

        player.level().playSound(null, player.blockPosition(),
                ModSounds.TEMPST_PASSIVE.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        COOLDOWNS.remove(event.getEntity().getUUID());
    }
}
