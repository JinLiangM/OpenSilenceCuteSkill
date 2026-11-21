package lol.silencecute.client.screen;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModeValue extends Value<String> {

    private final Supplier<String> getter;
    private final Consumer<String> setter;
    public final String[] modes;

    public java.util.function.BooleanSupplier locked;

    public Runnable onLockedClick;

    public boolean isLocked() {
        return this.locked != null && this.locked.getAsBoolean();
    }

    public boolean rejectLocked() {
        if (!isLocked()) return false;
        Runnable action = this.onLockedClick;
        if (action != null) {
            try {
                action.run();
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public ModeValue(String name, Supplier<String> getter, Consumer<String> setter, String... modes) {
        super(name);
        this.getter = getter;
        this.setter = setter;
        this.modes = modes;

        this.height = 24.0F;
    }

    @Override
    public String get() {
        String value = getter.get();
        return value == null ? (modes.length > 0 ? modes[0] : "") : value;
    }

    public void set(String mode) {
        if (rejectLocked()) {
            return;
        }
        for (String candidate : modes) {
            if (candidate != null && candidate.equalsIgnoreCase(mode)) {
                setter.accept(candidate);
                return;
            }
        }
    }

    public void cycle() {
        if (rejectLocked()) {
            return;
        }
        if (modes.length == 0) return;
        String current = get();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(current)) {
                setter.accept(modes[(i + 1) % modes.length]);
                return;
            }
        }
        setter.accept(modes[0]);
    }

    public boolean is(String mode) {
        return get().equalsIgnoreCase(mode);
    }

    public float dropdownWidth(float textPadding) {
        float widest = 80.0F;
        for (String mode : modes) {
            if (mode == null) continue;
            widest = Math.max(widest, mode.length() * textPadding);
        }
        return widest;
    }
}
