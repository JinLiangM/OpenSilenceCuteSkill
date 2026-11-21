package lol.silencecute.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.phys.Vec3;

public final class TrailSimplify {

    public static final double DEFAULT_TOLERANCE = 0.35D;

    private TrailSimplify() {
    }

    public static List<Vec3> simplify(List<Vec3> points) {
        return simplify(points, DEFAULT_TOLERANCE);
    }

    public static List<Vec3> simplify(List<Vec3> points, double tolerance) {
        List<Vec3> result = new ArrayList<>();
        if (points == null || points.isEmpty()) return result;
        if (points.size() <= 2) {
            result.addAll(points);
            return result;
        }

        boolean[] keep = new boolean[points.size()];
        keep[0] = true;
        keep[points.size() - 1] = true;
        reduce(points, 0, points.size() - 1, tolerance, keep);

        for (int i = 0; i < keep.length; i++) {
            if (keep[i]) result.add(points.get(i));
        }
        return result;
    }

    private static void reduce(List<Vec3> points, int first, int last, double tolerance, boolean[] keep) {
        if (last <= first + 1) return;

        Vec3 a = points.get(first);
        Vec3 b = points.get(last);
        double worst = -1.0D;
        int worstIndex = -1;

        for (int i = first + 1; i < last; i++) {
            double distance = distanceToSegment(points.get(i), a, b);
            if (distance > worst) {
                worst = distance;
                worstIndex = i;
            }
        }

        if (worst > tolerance && worstIndex > 0) {
            keep[worstIndex] = true;
            reduce(points, first, worstIndex, tolerance, keep);
            reduce(points, worstIndex, last, tolerance, keep);
        }
    }

    private static double distanceToSegment(Vec3 point, Vec3 a, Vec3 b) {
        Vec3 segment = b.subtract(a);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-9D) return point.distanceTo(a);

        double t = point.subtract(a).dot(segment) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return point.distanceTo(a.add(segment.scale(t)));
    }
}
