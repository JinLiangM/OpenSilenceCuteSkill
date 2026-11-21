package lol.silencecute.api;

public enum DashMode {
    FOUR_WAY("四向"),
    EIGHT_WAY("八向");

    private final String displayName;

    DashMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
