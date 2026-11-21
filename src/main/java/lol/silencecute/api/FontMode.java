package lol.silencecute.api;

public enum FontMode {

    MORDEN("Morden"),

    HARMONY("Harmony"),

    MINECRAFT("Minecraft (原版)");

    private final String displayName;

    FontMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static FontMode byDisplayName(String name) {
        if (name != null) {
            for (FontMode mode : values()) {
                if (mode.displayName.equalsIgnoreCase(name) || mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            String normalized = name.toLowerCase();
            if (normalized.contains("minecraft") || normalized.contains("vanilla")) {
                return MORDEN;
            }
        }
        return MORDEN;
    }
}
