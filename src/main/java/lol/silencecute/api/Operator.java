package lol.silencecute.api;

import java.util.Locale;

public enum Operator {
    NONE("none", "none", "无"),
    LUNA("Luna", "Luna", "露娜"),
    WEILONG("Vyron", "Vyron", "威龙"),
    JIFENG("Tempst", "Tempst", "疾风");

    private final String selectionName;
    private final String displayName;
    private final String zhName;

    Operator(String selectionName, String displayName, String zhName) {
        this.selectionName = selectionName;
        this.displayName = displayName;
        this.zhName = zhName;
    }

    public String selectionName() {
        return selectionName;
    }

    public String displayName() {
        return displayName;
    }

    public String zhName() {
        return zhName;
    }

    public static Operator byName(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String key = raw.toUpperCase(Locale.ROOT);
        for (Operator operator : values()) {
            if (operator.name().equals(key)) return operator;
        }
        return null;
    }

    public static Operator byInput(String raw) {
        if (raw == null) return null;
        String key = raw.trim();
        if (key.isEmpty()) return null;
        Operator byEnum = byName(key);
        if (byEnum != null) return byEnum;
        for (Operator operator : values()) {
            if (operator.selectionName.equalsIgnoreCase(key)
                    || operator.displayName.equalsIgnoreCase(key)
                    || operator.zhName.equals(key)) {
                return operator;
            }
        }
        return null;
    }

    public static Operator[] selectable() {
        return new Operator[]{LUNA, WEILONG, JIFENG};
    }

    public boolean selected() {
        return this != NONE;
    }

    public String ultimateSkillName() {
        return switch (this) {
            case LUNA -> "侦察箭矢";
            case WEILONG -> "虎蹲炮";
            default -> "";
        };
    }

    public String secondSkillName() {
        return switch (this) {
            case LUNA -> "电击箭矢";
            case WEILONG -> "动力推进";
            case NONE, JIFENG -> "";
        };
    }

    public String thirdSkillName() {
        return switch (this) {
            case LUNA -> "增强型破片手雷";
            case WEILONG -> "C4（快投）";
            case NONE, JIFENG -> "";
        };
    }
}
