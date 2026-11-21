package lol.silencecute.api;

public enum GuiTheme {

    DARK("暗"),
    LIGHT("亮");

    public final String displayName;

    GuiTheme(String displayName) {
        this.displayName = displayName;
    }

    public static GuiTheme byDisplayName(String name) {
        for (GuiTheme theme : values()) {
            if (theme.displayName.equalsIgnoreCase(name) || theme.name().equalsIgnoreCase(name)) return theme;
        }
        return DARK;
    }
}
