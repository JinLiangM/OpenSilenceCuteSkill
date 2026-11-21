package lol.silencecute.client.anim;

public final class LunaBowSway {
    public record Motion(float x, float y, float pitch, float yaw, float roll,
                         float cameraPitch, float cameraYaw, float cameraRoll) {
        public static final Motion STILL = new Motion(0, 0, 0, 0, 0, 0, 0, 0);

        public LunaBowAnimation.Pose apply(LunaBowAnimation.Pose pose) {
            return new LunaBowAnimation.Pose(pose.x() + x, pose.y() + y, pose.z(),
                    pose.pitch() + pitch, pose.yaw() + yaw, pose.roll() + roll,
                    pose.stringPull(), pose.drawHandRelease(), pose.aimBlend());
        }
    }

    private LunaBowSway() { }

    public static Motion sample(float ticks, float walk, float movement, float turnYaw, float turnPitch,
                                float aim, float envelope) {
        float breath = (float) Math.sin(ticks * 0.075F);
        float drift = (float) Math.sin(ticks * 0.047F);
        float stride = (float) Math.sin(walk * Math.PI * 2.0);
        float step = (float) Math.cos(walk * Math.PI * 4.0);
        float hand = envelope * (1.0F - 0.86F * aim);

        float camera = envelope * (1.0F - aim) * (1.0F - aim);
        return new Motion(
                hand * (drift * 0.003F + stride * movement * 0.009F - turnYaw * 0.0007F),
                hand * (breath * 0.0045F + step * movement * 0.006F + turnPitch * 0.0006F),
                hand * (breath * 0.24F + step * movement * 0.32F - turnPitch * 0.12F),
                hand * (drift * 0.24F - turnYaw * 0.14F),
                hand * (drift * 0.38F + stride * movement * 0.70F - turnYaw * 0.065F),
                camera * (breath * 0.055F + step * movement * 0.07F),
                camera * drift * 0.045F,
                camera * (drift * 0.16F + stride * movement * 0.20F));
    }
}
