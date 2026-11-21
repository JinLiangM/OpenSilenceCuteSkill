package lol.silencecute.client.screen;

public class LabelValue extends Value<String> {

    public LabelValue(String name) {
        super(name);
        this.height = 20.0F;
    }

    @Override
    public String get() {
        return name;
    }

    @Override
    public String text() {
        return name;
    }
}
