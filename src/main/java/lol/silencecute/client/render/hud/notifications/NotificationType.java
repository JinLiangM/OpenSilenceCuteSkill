package lol.silencecute.client.render.hud.notifications;

public enum NotificationType {

    WARNING("红", 0xAA1A1A, 2000L),

    SCANNING("黄", 0xFFFFAA00, 2830L);

    private final String defaultContent;
    private final int themeColor;
    private final long defaultDuration;

    NotificationType(String defaultContent, int themeColor, long defaultDuration) {
        this.defaultContent = defaultContent;
        this.themeColor = themeColor;
        this.defaultDuration = defaultDuration;
    }

    public String getDefaultContent() { return defaultContent; }
    public int getThemeColor() { return themeColor; }
    public long getDefaultDuration() { return defaultDuration; }
}