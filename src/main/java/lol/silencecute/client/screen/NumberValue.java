package lol.silencecute.client.screen;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.util.Mth;

public class NumberValue extends Value<Float> {

    public static final float TRACK_WIDTH = 70.0F;

    private final Supplier<Float> getter;
    private final Consumer<Float> setter;
    public final float min;
    public final float max;

    public final float inc;

    public float knobAnimation;

    public float animatedPercentage;
    public boolean sliding;

    public NumberValue(String name, Supplier<Float> getter, Consumer<Float> setter,
                       float min, float max) {
        this(name, getter, setter, min, max, 1.0F);
    }

    public NumberValue(String name, Supplier<Float> getter, Consumer<Float> setter,
                       float min, float max, float inc) {
        super(name);
        this.getter = getter;
        this.setter = setter;
        this.min = min;
        this.max = max;
        this.inc = inc <= 0.0F ? 1.0F : inc;
    }

    @Override
    public Float get() {
        Float value = getter.get();
        return value == null ? min : value;
    }

    public void set(float value) {
        float clamped = Mth.clamp(value, min, max);
        if (inc > 0.0F) {
            clamped = Math.round(clamped / inc) * inc;
        }
        setter.accept(Mth.clamp(clamped, min, max));
    }

    public float ratio() {
        float range = max - min;
        return range <= 0.0F ? 0.0F : Mth.clamp((get() - min) / range, 0.0F, 1.0F);
    }

    public String display() {
        String text = String.valueOf(get());
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        if (text.startsWith("0.")) {
            return "." + text.substring(2);
        }
        if (text.startsWith("-0.")) {
            return "-" + text.substring(3);
        }
        return text;
    }
}
