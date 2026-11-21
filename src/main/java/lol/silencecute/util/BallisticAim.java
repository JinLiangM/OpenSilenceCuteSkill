package lol.silencecute.util;

import net.minecraft.world.phys.Vec3;

public final class BallisticAim {

    private static final double INERTIA = 0.99D;

    private static final int MAX_TICKS = 400;

    private static final double SCAN_MIN_ANGLE = -1.0D;

    private static final double SCAN_MAX_ANGLE = 1.2D;

    private static final double SCAN_STEP = 0.02D;

    private static final int ITERATIONS = 24;

    public static final double MIN_SOLVE_DISTANCE = 1.5D;

    private BallisticAim() {
    }

    public static Vec3 solve(Vec3 origin, Vec3 aim, Vec3 look, float speed, double gravity,
                             double horizontalScale, double verticalScale) {
        Vec3 delta = aim.subtract(origin);
        double flat = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (flat < 1.0E-4D) {
            return new Vec3(0.0D, delta.y >= 0.0D ? 1.0D : -1.0D, 0.0D);
        }
        if (flat < MIN_SOLVE_DISTANCE) {
            return look.normalize();
        }
        Vec3 forward = new Vec3(delta.x / flat, 0.0D, delta.z / flat);
        double rise = delta.y;

        double previousAngle = SCAN_MIN_ANGLE;
        double previousHeight = heightAt(speed, flat, previousAngle, gravity,
                horizontalScale, verticalScale);
        double fallbackAngle = previousAngle;
        double fallbackHeight = previousHeight;
        double low = Double.NaN;
        double high = Double.NaN;
        for (double angle = SCAN_MIN_ANGLE + SCAN_STEP; angle <= SCAN_MAX_ANGLE;
             angle += SCAN_STEP) {
            double height = heightAt(speed, flat, angle, gravity, horizontalScale, verticalScale);
            if (height > fallbackHeight) {
                fallbackHeight = height;
                fallbackAngle = angle;
            }
            if (Double.isFinite(height) && Double.isFinite(previousHeight)
                    && previousHeight < rise && height >= rise) {
                low = previousAngle;
                high = angle;
                break;
            }
            previousAngle = angle;
            previousHeight = height;
        }
        if (!Double.isFinite(low)) {
            return launchVector(forward, fallbackAngle);
        }
        for (int i = 0; i < ITERATIONS; i++) {
            double mid = (low + high) * 0.5D;
            double height = heightAt(speed, flat, mid, gravity, horizontalScale, verticalScale);
            if (Double.isFinite(height) && height >= rise) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return launchVector(forward, (low + high) * 0.5D);
    }

    public static double heightAt(float speed, double distance, double angle, double gravity,
                                  double horizontalScale, double verticalScale) {
        double vx = speed * Math.cos(angle) * horizontalScale;
        double vy = speed * Math.sin(angle) * verticalScale;
        double x = 0.0D;
        double y = 0.0D;
        for (int i = 0; i < MAX_TICKS; i++) {
            if (x + vx >= distance) {
                double t = vx <= 1.0E-6D ? 0.0D : (distance - x) / vx;
                return y + vy * t;
            }
            if (vx < 1.0E-5D) {
                return Double.NEGATIVE_INFINITY;
            }
            x += vx;
            y += vy;
            vx *= INERTIA;
            vy = vy * INERTIA - gravity;
        }
        return Double.NEGATIVE_INFINITY;
    }

    private static Vec3 launchVector(Vec3 forward, double angle) {
        double horizontal = Math.cos(angle);
        double vertical = Math.sin(angle);
        return new Vec3(forward.x * horizontal, vertical, forward.z * horizontal).normalize();
    }
}
