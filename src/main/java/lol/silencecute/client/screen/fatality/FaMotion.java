package lol.silencecute.client.screen.fatality;

final class FaMotion {
    private float value;

    FaMotion(float initial) {
        value = initial;
    }

    float toward(float target, float dt, float speed) {
        float t = 1.0f - (float) Math.exp(-Math.max(0.0f, dt) * speed);
        value += (target - value) * t;
        if (Math.abs(target - value) < 0.001f) value = target;
        return value;
    }

    float value() {
        return value;
    }

    void snap(float value) {
        this.value = value;
    }
}
