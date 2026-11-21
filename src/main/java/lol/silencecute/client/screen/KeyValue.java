package lol.silencecute.client.screen;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.mojang.blaze3d.platform.InputConstants;

public class KeyValue extends Value<Integer> {

    public static final int MOUSE_BASE = 0x1000;

    public static int mouseKey(int button) {
        return MOUSE_BASE + Math.max(0, Math.min(7, button));
    }

    public static boolean isMouse(int key) {
        return key >= MOUSE_BASE && key < MOUSE_BASE + 8;
    }

    public static int mouseButton(int key) {
        return key - MOUSE_BASE;
    }

    public static String displayName(int key) {
        if (key <= 0) return "未绑定";
        if (isMouse(key)) return "M" + (mouseButton(key) + 1);
        try {
            String name = InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
            return name == null || name.isEmpty() ? "?" : name;
        } catch (RuntimeException exception) {
            return "?";
        }
    }

    public static int encode(InputConstants.Key key) {
        if (key == null) return 0;
        if (key.getType() == InputConstants.Type.MOUSE) return mouseKey(key.getValue());
        return key.getValue();
    }

    private final IntSupplier getter;
    private final IntConsumer setter;

    public KeyValue(String name, IntSupplier getter, IntConsumer setter) {
        super(name);
        this.getter = getter;
        this.setter = setter;
        this.height = 24.0F;
    }

    @Override
    public Integer get() {
        return getter.getAsInt();
    }

    public void set(int key) {
        setter.accept(key);
    }

    public boolean isUnbound() {
        return get() <= 0;
    }
}
