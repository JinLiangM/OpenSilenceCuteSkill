package lol.silencecute.client.screen;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class ColorValue extends Value<Integer> {

    private final IntSupplier getter;
    private final Consumer<Integer> setter;

    public boolean alphaChangeable;

    public ColorValue(String name, IntSupplier getter, Consumer<Integer> setter) {
        super(name);
        this.getter = getter;
        this.setter = setter;
    }

    public ColorValue alpha(boolean changeable) {
        this.alphaChangeable = changeable;
        return this;
    }

    @Override
    public Integer get() {
        return getter.getAsInt() & 0xFFFFFF;
    }

    public void set(int rgb) {
        try {
            setter.accept(rgb & 0xFFFFFF);
        } catch (Exception ignored) {

        }
    }

    public void setHsb(float hue, float saturation, float brightness) {
        set(Color.HSBtoRGB(hue, saturation, brightness));
    }

    public float[] hsb() {
        int rgb = get();
        return Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
    }
}
