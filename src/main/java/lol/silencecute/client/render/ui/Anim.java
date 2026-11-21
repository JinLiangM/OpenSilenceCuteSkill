package lol.silencecute.client.render.ui;

import lol.silencecute.client.render.kit.AnimationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class Anim {

    private Anim() {}

    public static void syncDelta() {
        float seconds;
        try {
            seconds = Minecraft.getInstance().getFrameTime();
        } catch (Exception e) {
            seconds = 0.016F;
        }
        AnimationUtils.setDelta(Mth.clamp(seconds * 1000.0F, 1.0F, 100.0F));
    }

    public static float smooth(float current, float target, float speed) {
        return AnimationUtils.animateSmooth(current, target, speed);
    }

    public static float smooth(float current, double target, float speed) {
        return AnimationUtils.animateSmooth(current, (float) target, speed);
    }

    public static float ease(float current, float target, float factor) {
        if (Math.abs(target - current) < 0.05F) return target;
        return current + (target - current) * Mth.clamp(factor, 0.0F, 1.0F);
    }

    public static float linear(float current, float target, float pixelsPerFrame) {
        float diff = target - current;
        float step = pixelsPerFrame * frameFactor();
        if (Math.abs(diff) <= step) return target;
        return current + Math.signum(diff) * step;
    }

    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    private static float frameFactor() {
        try {
            float seconds = Minecraft.getInstance().getFrameTime();
            return Mth.clamp(seconds * 60.0F, 0.25F, 3.0F);
        } catch (Exception e) {
            return 1.0F;
        }
    }
}
