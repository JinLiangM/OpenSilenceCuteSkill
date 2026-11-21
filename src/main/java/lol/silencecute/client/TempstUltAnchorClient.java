package lol.silencecute.client;

import java.util.ArrayList;
import java.util.List;

import lol.silencecute.util.TrailSimplify;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class TempstUltAnchorClient {

    public static final long WINDOW_NANOS = 25_000_000_000L;

    private static final double SPACING = 2.5D;

    public static final double ROPE_HEIGHT = 1.0D;

    public static final double MOUNT_HEIGHT = 0.5D;

    public static final double WAIST_OFFSET = 0.22D;

    private static final double TRAIL_STEP = 0.25D;

    private static Vec3 anchorA;
    private static Vec3 anchorB;
    private static long startNanos;

    private static final List<Vec3> TRAIL_A = new ArrayList<>();
    private static final List<Vec3> TRAIL_B = new ArrayList<>();

    private static final List<Vec3> PATH_A = new ArrayList<>();
    private static final List<Vec3> PATH_B = new ArrayList<>();

    private static Vec3 lastFeet;

    private TempstUltAnchorClient() {
    }

    public static void begin(LocalPlayer player) {
        Vec3 feet = player.position();
        double yaw = Math.toRadians(player.getYRot());
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);

        anchorA = groundAt(player, feet.x + rightX * SPACING * 0.5D, feet.z + rightZ * SPACING * 0.5D);
        anchorB = groundAt(player, feet.x - rightX * SPACING * 0.5D, feet.z - rightZ * SPACING * 0.5D);

        TRAIL_A.clear();
        TRAIL_B.clear();
        PATH_A.clear();
        PATH_B.clear();
        lastFeet = null;

        startNanos = System.nanoTime();
        record(player);
    }

    public static void reset() {
        anchorA = null;
        anchorB = null;
        startNanos = 0L;
        lastFeet = null;
        TRAIL_A.clear();
        TRAIL_B.clear();
        PATH_A.clear();
        PATH_B.clear();
    }

    public static boolean isActive() {
        return anchorA != null && startNanos != 0L
                && System.nanoTime() - startNanos < WINDOW_NANOS;
    }

    public static float progress() {
        if (startNanos == 0L) return 0.0F;
        double p = (System.nanoTime() - startNanos) / (double) WINDOW_NANOS;
        return (float) Math.max(0.0D, Math.min(1.0D, p));
    }

    public static double distanceToAnchor(Vec3 playerPos) {
        if (anchorA == null || anchorB == null || playerPos == null) return 0.0D;
        Vec3 center = anchorA.add(anchorB).scale(0.5D);
        return Math.sqrt(center.distanceToSqr(playerPos.x, center.y, playerPos.z));
    }

    public static Vec3 anchorA() {
        return anchorA;
    }

    public static Vec3 anchorB() {
        return anchorB;
    }

    public static void tick(LocalPlayer player) {
        if (anchorA == null || startNanos == 0L) return;
        if (!isActive()) return;
        record(player);
    }

    private static void record(LocalPlayer player) {
        Vec3 feet = player.position();

        if (!player.onGround()) return;

        if (lastFeet != null && lastFeet.distanceTo(feet) < TRAIL_STEP) return;
        lastFeet = feet;

        TRAIL_A.add(waist(player, -1));
        TRAIL_B.add(waist(player, 1));
        rebuild();
    }

    private static void rebuild() {
        if (anchorA != null) {
            PATH_A.clear();
            PATH_A.add(mount(anchorA));
            PATH_A.addAll(TrailSimplify.simplify(TRAIL_A));
        }
        if (anchorB != null) {
            PATH_B.clear();
            PATH_B.add(mount(anchorB));
            PATH_B.addAll(TrailSimplify.simplify(TRAIL_B));
        }
    }

    public static Vec3 mount(Vec3 anchor) {
        return new Vec3(anchor.x, anchor.y + MOUNT_HEIGHT, anchor.z);
    }

    public static Vec3 waist(LocalPlayer player, int side) {
        Vec3 feet = player.position();
        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(
                feet.x + Math.cos(yaw) * WAIST_OFFSET * side,
                feet.y + ROPE_HEIGHT,
                feet.z + Math.sin(yaw) * WAIST_OFFSET * side);
    }

    public static List<Vec3> ropePath(LocalPlayer player, int index) {
        List<Vec3> path = index == 0 ? PATH_A : PATH_B;
        if (player == null || path.isEmpty()) return path;

        Vec3 live = waist(player, index == 0 ? -1 : 1);
        int last = path.size() - 1;
        if (path.get(last).distanceToSqr(live) > 1.0E-6D) {
            path.set(last, live);
        }
        return path;
    }

    private static Vec3 groundAt(LocalPlayer player, double x, double z) {
        Vec3 from = new Vec3(x, player.getY() + 2.0D, z);
        Vec3 to = new Vec3(x, player.getY() - 4.0D, z);
        var hit = player.level().clip(new net.minecraft.world.level.ClipContext(
                from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        double groundY = hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                ? hit.getLocation().y : player.getY();
        return new Vec3(x, groundY, z);
    }
}
