package lol.silencecute.client.screen;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextValue extends Value<String> {

    private final Supplier<String> getter;
    private final Consumer<String> setter;

    public String selectedString = "";

    public TextValue(String name, Supplier<String> getter, Consumer<String> setter) {
        super(name);
        this.getter = getter;
        this.setter = setter;
        this.height = 24.0F;
    }

    @Override
    public String get() {
        String value = getter.get();
        return value == null ? "" : value;
    }

    public void set(String value) {
        setter.accept(value == null ? "" : value);
    }

    public void append(char typed) {
        if (typed < ' ') return;
        String current = get();
        if (current.length() >= 32) return;
        set(current + typed);
    }

    public void backspace() {
        if (!selectedString.isEmpty()) {
            set(get().replace(selectedString, ""));
            selectedString = "";
            return;
        }
        String current = get();
        if (!current.isEmpty()) {
            set(current.substring(0, current.length() - 1));
        }
    }
}
