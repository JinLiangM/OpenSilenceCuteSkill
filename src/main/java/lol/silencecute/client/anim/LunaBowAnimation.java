package lol.silencecute.client.anim;

public final class LunaBowAnimation {
    static final float EQUIP_SECONDS = 0.25F;
    public static final float AIM_SECONDS = 0.10F;
    public static final float RELEASE_SECONDS = 0.24F;

    public record Pose(float x, float y, float z, float pitch, float yaw, float roll,
                       float stringPull, float drawHandRelease, float aimBlend) { }

    private LunaBowAnimation() { }

    public static float smooth(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }

    public static Pose sample(float power, float equipSeconds, float releaseSeconds, float aimSeconds) {
        float aim = smooth(aimSeconds / AIM_SECONDS);
        float equip = 1.0F - smooth(equipSeconds / EQUIP_SECONDS);
        float lower = releaseSeconds < 0.0F ? 0.0F
                : smooth((releaseSeconds - 0.055F) / (RELEASE_SECONDS - 0.055F));
        float kick = releaseSeconds < 0.0F ? 0.0F
                : (float) Math.sin(Math.min(1.0F, releaseSeconds / 0.16F) * Math.PI);
        float release = releaseSeconds < 0.0F ? 0.0F : smooth(releaseSeconds / 0.13F);
        float stringPull = Math.max(0.0F, Math.min(1.0F, power)) * (1.0F - release) * aim;
        return new Pose(0.22F - aim * 0.02F,
                -0.40F + aim * 0.21F - equip * 1.35F - lower * 1.55F,
                -0.80F - aim * 0.02F + lower * 0.15F - kick * 0.018F,
                8.0F - aim * 8.0F + lower * 36.0F - kick * 2.0F,
                25.0F - aim * 24.0F,
                -42.0F + aim * 40.0F - lower * 10.0F,
                stringPull, release, aim);
    }
}
