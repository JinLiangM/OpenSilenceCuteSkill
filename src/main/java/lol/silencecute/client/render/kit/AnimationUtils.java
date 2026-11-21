package lol.silencecute.client.render.kit;

public final class AnimationUtils {

    public static float delta = 1.0F;

    private AnimationUtils() {
    }

    public static void setDelta(float frameMs) {
        delta = Math.max(0.1F, Math.min(10.0F, frameMs / 20.0F));
    }

    public static float getDelta() {
        return delta;
    }

    public static float animate(float target, float current, double speed) {
        boolean larger = target > current;
        if (speed < 0.0) {
            speed = 0.0;
        } else if (speed > 1.0) {
            speed = 1.0;
        }
        double dif = Math.max(target, current) - Math.min(target, current);
        double factor = dif * speed;
        if (factor < 0.1) {
            factor = 0.1;
        }
        if (larger) {
            current = (float) (current + (double) (delta * (float) factor));
        } else if (target < current) {
            current = (float) (current - (double) (delta * (float) factor));
        }
        return current;
    }

    public static double animate(double target, double current, double speed) {
        boolean larger = target > current;
        if (speed < 0.0) {
            speed = 0.0;
        } else if (speed > 1.0) {
            speed = 1.0;
        }
        double dif = Math.max(target, current) - Math.min(target, current);
        double factor = dif * speed;
        if (factor < 0.1) {
            factor = 0.1;
        }
        if (larger) {
            current += delta * factor;
        } else if (target < current) {
            current -= delta * factor;
        }
        return current;
    }

    public static float animateSmooth(float current, float target, double speed) {
        float delta = getDelta();
        float dif = target - current;
        float factor = (float) (dif * speed);
        if (factor < 0.0006F && factor > -0.0006F) {
            return target;
        }
        return current + factor * delta;
    }

    public static float animateSmooth(float current, float target, double speed, float minStep) {
        float delta = getDelta();
        float dif = target - current;
        float factor = (float) (dif * speed);
        if (Math.abs(factor) < minStep) {
            factor = Math.copySign(minStep, dif);
        }
        if (Math.abs(dif) <= minStep) {
            return target;
        }
        return current + factor * delta;
    }

    public static float calculateCompensation(float target, float current, long delta, int speed) {
        float diff = current - target;
        if (delta < 1L) {
            delta = 1L;
        }
        double speedCalc;
        if (diff > (float) speed) {
            speedCalc = (double) ((float) delta * ((float) speed / 16.6667F));
            current = (float) ((double) current - speedCalc);
            if (current < target) {
                current = target;
            }
        } else if (diff < (float) (-speed)) {
            speedCalc = (double) ((float) delta * ((float) speed / 16.6667F));
            current = (float) ((double) current + speedCalc);
            if (current > target) {
                current = target;
            }
        } else {
            current = target;
        }
        return current;
    }
}
