package lol.silencecute.client.screen;

public abstract class Value<T> {

    public final String name;

    public float animation;

    public float height = 22.0F;

    protected java.util.function.BooleanSupplier available;

    public boolean navenOnly;

    public String modernName;
    private boolean hidden;

    public String displayName() {
        return modernName != null ? modernName : name;
    }

    protected Value(String name) {
        this.name = name;
    }

    public abstract T get();

    public String text() {
        T value = get();
        return value == null ? "" : String.valueOf(value);
    }

    public Value<T> availableWhen(java.util.function.BooleanSupplier condition) {
        this.available = condition;
        return this;
    }

    public Value<T> modern(String label) {
        this.modernName = label;
        return this;
    }

    public Value<T> navenOnly() {
        this.navenOnly = true;
        return this;
    }

    public boolean isAvailable() {
        return available == null || available.getAsBoolean();
    }

    public boolean isHidden() {
        return hidden;
    }

    public Value<T> hidden(boolean value) {
        this.hidden = value;
        return this;
    }

    public float getHeight() {
        return height;
    }
}
