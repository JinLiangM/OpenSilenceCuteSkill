package lol.silencecute.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lol.silencecute.access.AccessService;
import lol.silencecute.event.OperatorState;
import lol.silencecute.network.TempstUltPullPacket;
import lol.silencecute.registry.ModNetwork;
import lol.silencecute.registry.ModSounds;
import lol.silencecute.util.TrailSimplify;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public final class TempstUltimateService {

    public static boolean ENABLED = false;

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("silencecute_skill");

    public static int DURATION_TICKS = 500;

    public static int COOLDOWN_TICKS = 2400;

    public static int STAND_UP_TICKS = 8;

    private static final double PULL_SPEED = 1.0D;

    private static final double ARRIVE_DISTANCE = 0.55D;

    private static final double TRAIL_STEP = 0.25D;

    private static final Map<UUID, Long> ACTIVE_END = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, double[]> ANCHORS = new HashMap<>();
    private static final Map<UUID, float[]> CAST_ROT = new HashMap<>();
    private static final Map<UUID, List<Vec3>> TRAILS = new HashMap<>();
    private static final Map<UUID, Pull> PULLS = new HashMap<>();
    private static final Map<UUID, Long> STAND_UP_UNTIL = new HashMap<>();

    private TempstUltimateService() {
    }

    private static final class Pull {
        private final List<Vec3> points;
        private final double[] cumulative;
        private final double totalLength;
        private final long startTick;
        private final long deadlineTick;

        private Pull(List<Vec3> points, long now) {
            this.points = points;
            this.cumulative = new double[points.size()];
            double total = 0.0D;
            for (int i = 1; i < points.size(); i++) {
                total += points.get(i - 1).distanceTo(points.get(i));
                this.cumulative[i] = total;
            }
            this.totalLength = total;
            this.startTick = now;
            this.deadlineTick = now + (long) (total * 3.0D) + 80L;
        }

        private Vec3 end() {
            return points.isEmpty() ? null : points.get(points.size() - 1);
        }

        private double travelled(long now) {
            return Math.min(totalLength, (now - startTick) * PULL_SPEED);
        }

        private Vec3 tangentAt(double distance) {
            if (points.size() < 2) return Vec3.ZERO;
            Vec3 a = pointAt(Math.max(0.0D, distance - 0.4D));
            Vec3 b = pointAt(Math.min(totalLength, distance + 0.4D));
            if (a == null || b == null) return Vec3.ZERO;
            Vec3 direction = b.subtract(a);
            double length = direction.length();
            return length < 1.0E-4D ? Vec3.ZERO : direction.scale(1.0D / length);
        }

        private Vec3 pointAt(double distance) {
            if (points.isEmpty()) return null;
            if (points.size() == 1) return points.get(0);
            for (int i = 1; i < points.size(); i++) {
                if (distance <= cumulative[i]) {
                    double segmentLength = cumulative[i] - cumulative[i - 1];
                    double t = segmentLength <= 1.0E-6D
                            ? 0.0D : (distance - cumulative[i - 1]) / segmentLength;
                    Vec3 a = points.get(i - 1);
                    Vec3 b = points.get(i);
                    return a.add(b.subtract(a).scale(t));
                }
            }
            return points.get(points.size() - 1);
        }
    }

    public static boolean isActive(UUID playerId, long now) {
        Long end = playerId == null ? null : ACTIVE_END.get(playerId);
        return end != null && end > now;
    }

    public static long activeRemainTicks(UUID playerId, long now) {
        Long end = playerId == null ? null : ACTIVE_END.get(playerId);
        return end == null ? 0L : Math.max(0L, end - now);
    }

    public static long cooldownRemainTicks(UUID playerId, long now) {
        if (playerId == null) return 0L;
        if (AccessService.isNoCooldown(playerId)) return 0L;
        return Math.max(0L, COOLDOWNS.getOrDefault(playerId, 0L) - now);
    }

    public static void clearCooldown(UUID playerId) {
        if (playerId == null) return;
        COOLDOWNS.remove(playerId);
        ACTIVE_END.remove(playerId);
        ANCHORS.remove(playerId);
        TRAILS.remove(playerId);
        PULLS.remove(playerId);
        STAND_UP_UNTIL.remove(playerId);
        CAST_ROT.remove(playerId);
    }

    public static boolean tryStart(ServerPlayer player) {
        if (!ENABLED) return false;
        if (player == null) return false;
        if (!OperatorState.isJifeng(player.getUUID())) return false;
        if (!player.isAlive()) return false;
        if (!player.onGround()) return false;

        long now = player.serverLevel().getGameTime();
        boolean admin = AccessService.isNoCooldown(player.getUUID());
        if (!admin && isActive(player.getUUID(), now)) return false;
        if (cooldownRemainTicks(player.getUUID(), now) > 0L) return false;

        ACTIVE_END.put(player.getUUID(), now + DURATION_TICKS);
        COOLDOWNS.put(player.getUUID(), now + COOLDOWN_TICKS);
        ANCHORS.put(player.getUUID(),
                new double[]{player.getX(), player.getY(), player.getZ()});
        CAST_ROT.put(player.getUUID(), new float[]{player.getYRot(), player.getXRot()});

        List<Vec3> trail = new ArrayList<>();
        trail.add(player.position());
        TRAILS.put(player.getUUID(), trail);

        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TEMPST_ULT_START.get(), SoundSource.PLAYERS, 1.5F, 1.0F);

        DashService.syncCooldown(player);
        return true;
    }

    public static boolean startPullBack(ServerPlayer player) {
        if (!ENABLED) return false;
        if (player == null) return false;
        UUID id = player.getUUID();
        long now = player.serverLevel().getGameTime();
        if (!isActive(id, now)) return false;
        if (PULLS.containsKey(id)) return false;

        ACTIVE_END.remove(id);
        buildPull(player, id, now);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), pullPacket(id, player));
        return true;
    }

    private static TempstUltPullPacket pullPacket(UUID id, ServerPlayer player) {
        float[] rot = CAST_ROT.get(id);
        if (rot == null) return new TempstUltPullPacket(true, player.getYRot(), player.getXRot());
        return new TempstUltPullPacket(true, rot[0], rot[1]);
    }

    private static void buildPull(ServerPlayer player, UUID id, long now) {
        List<Vec3> trail = TRAILS.remove(id);
        Vec3 here = player.position();

        List<Vec3> path = new ArrayList<>();
        path.add(here);
        if (trail != null) {
            List<Vec3> simplified = TrailSimplify.simplify(trail);
            for (int i = simplified.size() - 1; i >= 0; i--) {
                Vec3 point = simplified.get(i);
                Vec3 last = path.get(path.size() - 1);
                if (last.distanceTo(point) < 0.12D) continue;
                path.add(point);
            }
        }

        LOGGER.info("[TempstUlt] pull path points={} length~{}", path.size(), path.size() * 0.25D);
        PULLS.put(id, new Pull(path, now));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        UUID id = player.getUUID();
        long now = player.serverLevel().getGameTime();

        boolean active = isActive(id, now);

        if (active) {
            List<Vec3> trail = TRAILS.get(id);
            if (trail != null && player.onGround()) {
                Vec3 position = player.position();
                Vec3 last = trail.get(trail.size() - 1);
                if (last.distanceTo(position) >= TRAIL_STEP) {
                    trail.add(position);
                }
            }
        } else if (ANCHORS.containsKey(id) && !PULLS.containsKey(id) && TRAILS.containsKey(id)) {
            buildPull(player, id, now);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), pullPacket(id, player));
        }

        Pull pull = PULLS.get(id);
        if (pull != null) {
            Vec3 position = player.position();

            double travelled = pull.travelled(now);

            Vec3 guard = pull.pointAt(travelled);
            if (guard != null && player.connection != null && player.getY() > guard.y + 0.8D) {
                player.connection.teleport(guard.x, guard.y, guard.z,
                        player.getYRot(), player.getXRot());
                player.setDeltaMovement(Vec3.ZERO);
            }
            Vec3 tangent = pull.tangentAt(travelled);
            if (tangent.lengthSqr() > 1.0E-6D) {
                player.setDeltaMovement(tangent.scale(PULL_SPEED));
                player.hurtMarked = true;
                player.hasImpulse = true;
            }

            player.setNoGravity(true);
            player.fallDistance = 0.0F;
            player.setPose(Pose.SLEEPING);
            player.setShiftKeyDown(false);

            ServerLevel level = player.serverLevel();
            if (now % 2 == 0) {
                level.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 0.6D, player.getZ(),
                        2, 0.25D, 0.15D, 0.25D, 0.01D);
            }

            Vec3 finish = pull.end();
            boolean arrived = travelled >= pull.totalLength - 1.0E-3D
                    && (finish == null || position.distanceTo(finish) <= 1.2D);
            if (arrived || now > pull.deadlineTick) {
                if (finish != null) {
                    player.teleportTo(finish.x, finish.y, finish.z);
                }
                PULLS.remove(id);
                ANCHORS.remove(id);
                TRAILS.remove(id);
                STAND_UP_UNTIL.put(id, now + STAND_UP_TICKS);
                player.setNoGravity(false);
                player.setDeltaMovement(Vec3.ZERO);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new TempstUltPullPacket(false, 0.0F, 0.0F));
            }
            return;
        }

        Long standingUp = STAND_UP_UNTIL.get(id);
        if (standingUp != null) {
            if (now >= standingUp) {
                STAND_UP_UNTIL.remove(id);
                player.setPose(Pose.STANDING);
                player.setShiftKeyDown(false);
            } else {
                player.setPose(Pose.CROUCHING);
                player.setShiftKeyDown(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearCooldown(event.getEntity().getUUID());
    }
}
