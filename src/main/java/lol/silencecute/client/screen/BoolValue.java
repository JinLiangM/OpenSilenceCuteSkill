package lol.silencecute.client.screen;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BoolValue extends Value<Boolean> {

    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    public BoolValue(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(name);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Boolean get() {
        Boolean value = getter.get();
        return value != null && value;
    }

    public void set(boolean value) {
        setter.accept(value);
    }
}
