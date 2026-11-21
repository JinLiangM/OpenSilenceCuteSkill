package lol.silencecute.config;

import lol.silencecute.api.DashMode;
import lol.silencecute.api.FontMode;
import lol.silencecute.api.Operator;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ModConfigData {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<Operator> OPERATOR;
    public static final ForgeConfigSpec.BooleanValue OPERATOR_SELECTION_INITIALIZED;
    public static final ForgeConfigSpec.EnumValue<DashMode> DASH_MODE;
    public static final ForgeConfigSpec.BooleanValue GRENADE_FOV_EFFECT;

    public static final ForgeConfigSpec.BooleanValue SHOW_TRAJECTORY;
    public static final ForgeConfigSpec.BooleanValue SHOW_HEXAGON;
    public static final ForgeConfigSpec.BooleanValue LUNA_CROSSHAIR;
    public static final ForgeConfigSpec.BooleanValue SHOW_SHOCK_RADIUS;
    public static final ForgeConfigSpec.EnumValue<FontMode> FONT_MODE;

    public static final ForgeConfigSpec.EnumValue<FontMode> GUI_FONT;

    public static final ForgeConfigSpec.EnumValue<lol.silencecute.api.GuiTheme> GUI_THEME;

    public static final ForgeConfigSpec.IntValue GUI_ACCENT_RED;
    public static final ForgeConfigSpec.IntValue GUI_ACCENT_GREEN;
    public static final ForgeConfigSpec.IntValue GUI_ACCENT_BLUE;

    public static final ForgeConfigSpec.BooleanValue CHAMS_TEAM_COLOR;
    public static final ForgeConfigSpec.IntValue CHAMS_ALPHA;
    public static final ForgeConfigSpec.IntValue CHAMS_RED;
    public static final ForgeConfigSpec.IntValue CHAMS_GREEN;
    public static final ForgeConfigSpec.IntValue CHAMS_BLUE;

    public static final ForgeConfigSpec.BooleanValue CHAMS_MARKER;

    public static final ForgeConfigSpec.ConfigValue<String> MODULE_KEYS;

    public static final ForgeConfigSpec.BooleanValue GUI_CHINESE;

    public static final ForgeConfigSpec.ConfigValue<String> GUI_STYLE;

    public static final ForgeConfigSpec.IntValue GUI_BIND;

    public static final ForgeConfigSpec.IntValue GUI_BIND_SCAN;

    public static final ForgeConfigSpec.BooleanValue NV_BLUR;

    public static final ForgeConfigSpec.DoubleValue NV_BLUR_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue NV_BLUR_FPS;

    public static final ForgeConfigSpec.IntValue NV_BG_COLOR;

    public static final ForgeConfigSpec.DoubleValue NV_BG_ALPHA;

    public static final ForgeConfigSpec.BooleanValue HUD_SKILLS;

    public static final ForgeConfigSpec.BooleanValue HUD_PASSIVE;

    public static final ForgeConfigSpec.BooleanValue HUD_HEALTH;

    public static final ForgeConfigSpec.ConfigValue<String> HEALTH_DISPLAY_MODE;

    public static final ForgeConfigSpec.BooleanValue HUD_CHAT;

    public static final ForgeConfigSpec.BooleanValue HUD_PERMISSION;

    public static final ForgeConfigSpec.IntValue HUD_CHAT_MAX_MESSAGES;

    public static final ForgeConfigSpec.IntValue HUD_CHAT_FADE_TIME;

    public static final ForgeConfigSpec.BooleanValue NOTIFICATION_REA1;

    public static final ForgeConfigSpec.BooleanValue OPERATOR_ENABLED;

    public static final ForgeConfigSpec.BooleanValue RENDERER_ENABLED;

    public static final ForgeConfigSpec.BooleanValue OPERATOR_RENDER_ENABLED;

    public static final ForgeConfigSpec.BooleanValue WEILONG_PLACEHOLDER;

    public static final ForgeConfigSpec.BooleanValue JIFENG_PLACEHOLDER;

    public static final ForgeConfigSpec.ConfigValue<String> OPERATOR_VOICE_LANGUAGE;

    public static final ForgeConfigSpec.ConfigValue<String> YSM_ACTION_COMPAT;

    public static final ForgeConfigSpec.BooleanValue NOTIFICATION_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("干员技能系统偏好设置").push("operator_skills");

        OPERATOR = builder.comment("当前使用的干员；默认 none，必须使用指令选择")
                .defineEnum("operator", Operator.NONE);
        OPERATOR_SELECTION_INITIALIZED = builder.comment("内部迁移标记").define("operator_selection_initialized", false);
        DASH_MODE = builder.comment("动力推进方向判定").defineEnum("dash_mode", DashMode.FOUR_WAY);
        SHOW_TRAJECTORY = builder.comment("是否显示露娜手雷的抛物线").define("show_trajectory", true);
        SHOW_HEXAGON = builder.comment("是否显示抛物线终点的六边形").define("show_trajectory_hexagon", true);
        LUNA_CROSSHAIR = builder.comment("掏出露娜电击箭时替换准心").define("luna_crosshair", false);
        SHOW_SHOCK_RADIUS = builder.comment("是否以落地电箭为中心显示伤害范围红圈").define("show_shock_radius", false);
        FONT_MODE = builder.comment("GUI 字体：morden / FontManager / Minecraft").defineEnum("font_mode", FontMode.MORDEN);
        GUI_FONT = builder.comment("ClickGui 字体：morden / Minecraft（仅 ClickGui，HUD 固定 Minecraft）").defineEnum("gui_font", FontMode.MORDEN);
        GUI_THEME = builder.comment("ClickGui 整体色调：DARK / LIGHT").defineEnum("gui_theme", lol.silencecute.api.GuiTheme.DARK);
        GUI_ACCENT_RED = builder.comment("ClickGui 副颜色 - 红").defineInRange("gui_accent_red", 54, 0, 255);
        GUI_ACCENT_GREEN = builder.comment("ClickGui 副颜色 - 绿").defineInRange("gui_accent_green", 98, 0, 255);
        GUI_ACCENT_BLUE = builder.comment("ClickGui 副颜色 - 蓝").defineInRange("gui_accent_blue", 236, 0, 255);

        CHAMS_TEAM_COLOR = builder.comment("露娜被动显形是否跟随实体名字/队伍颜色").define("chams_team_color", false);
        CHAMS_ALPHA = builder.comment("露娜被动显形透明度 (0-255)").defineInRange("chams_alpha", 200, 0, 255);
        CHAMS_RED = builder.comment("露娜被动显形默认颜色 - 红").defineInRange("chams_red", 255, 0, 255);
        CHAMS_GREEN = builder.comment("露娜被动显形默认颜色 - 绿").defineInRange("chams_green", 50, 0, 255);
        CHAMS_BLUE = builder.comment("露娜被动显形默认颜色 - 蓝").defineInRange("chams_blue", 50, 0, 255);
        CHAMS_MARKER = builder.comment("露娜被动显形是否显示头顶黑红菱形标记").define("chams_marker", true);
        GRENADE_FOV_EFFECT = builder
                .comment("手雷进入引信时增大FOV；关闭则抵消减速带来的视野缩小，保持固定FOV")
                .define("grenade_fov_effect", true);
        MODULE_KEYS = builder.comment("ClickGui 模块快捷键，格式 模块名=键码;模块名=键码").define("module_keys", "");
        GUI_CHINESE = builder.comment("ClickGui 界面语言：true = 简体中文, false = English").define("gui_chinese", true);
        GUI_STYLE = builder.comment("ClickGui 风格：MODERN = Fatality 风格, NAVEN = Naven 风格").define("gui_style", "NAVEN");
        GUI_BIND = builder.comment("打开 ClickGui 的按键（GLFW 键码，344 = 右 Shift，-1 表示只能靠扫描码认）").defineInRange("gui_bind", 344, -1, 400);
        GUI_BIND_SCAN = builder.comment("ClickGui 打开键的扫描码（GLFW 扩展键带 0x100 位，右 Shift = 310）").defineInRange("gui_bind_scan", 310, -1, 400);
        NV_BLUR = builder.comment("Naven 模式背景模糊（仅 Naven ClickGui）").define("naven_blur", false);
        NV_BLUR_STRENGTH = builder.comment("Naven 背景模糊力度 0..15").defineInRange("naven_blur_strength", 2.0, 0.0, 19.0);
        NV_BLUR_FPS = builder
            .comment("Naven blur refresh rate (higher = smoother, more expensive)")
            .defineInRange("naven_blur_fps", 90.0, 15.0, 120.0);

    NV_BG_COLOR = builder.comment("Naven ClickGui 背景颜色 RGB").defineInRange("naven_bg_color", 0x9A9A9A, 0x000000, 0xFFFFFF);
        NV_BG_ALPHA = builder.comment("Naven ClickGui 背景不透明度 0..1").defineInRange("naven_bg_alpha", 0.35, 0.0, 1.0);
        HUD_SKILLS = builder.comment("显示干员技能 HUD").define("hud_skills", true);
        HUD_PASSIVE = builder.comment("显示被动技能 HUD").define("hud_passive", true);
        HUD_HEALTH = builder.comment("显示玩家血量 HUD（头像 + 血条 + 名字）").define("hud_health", true);
        HEALTH_DISPLAY_MODE = builder
                .comment("玩家血量 HUD 的上限显示方式：ALWAYS_100 = 始终 100（百分比）, TRUE_MAX = 真上限（原始点数）")
                .define("health_display_mode", "TRUE_MAX");
        HUD_CHAT = builder.comment("显示聊天框 HUD（接管原版聊天显示）").define("hud_chat", true);
        HUD_PERMISSION = builder.comment("显示权限信息 HUD（屏幕右侧）").define("hud_permission", true);
        HUD_CHAT_MAX_MESSAGES = builder.comment("聊天框最多显示多少条消息（5..15）")
                .defineInRange("hud_chat_max_messages", 10, 5, 15);
        HUD_CHAT_FADE_TIME = builder.comment("多少秒没有新消息后自动隐藏聊天框（1..60）")
                .defineInRange("hud_chat_fade_time", 15, 1, 60);
        NOTIFICATION_REA1 = builder.comment("信息通知（右下角消息条）").define("notification_rea1", true);
        OPERATOR_ENABLED = builder.comment("干员系统总开关（关闭后所有干员技能与相关 HUD 都不生效）")
                .define("operator_enabled", true);
        RENDERER_ENABLED = builder.comment("渲染器设置总开关（关闭后背景模糊不生效）")
                .define("renderer_enabled", true);
        OPERATOR_RENDER_ENABLED = builder.comment("干员技能渲染（露娜手雷/电箭/被动显形）")
                .define("operator_render_enabled", true);
        WEILONG_PLACEHOLDER = builder.comment("威龙技能渲染里的占位开关（暂无功能）")
                .define("weilong_placeholder", false);
        JIFENG_PLACEHOLDER = builder.comment("疾风技能渲染里的占位开关（技能暂未制作）")
                .define("jifeng_placeholder", false);
        OPERATOR_VOICE_LANGUAGE = builder.comment("干员语音语言：英语 / 中文（中文语音暂未制作）")
                .define("operator_voice_language", "英语");
        YSM_ACTION_COMPAT = builder.comment("改变动作时对 YSM（是，史蒂夫模型）的兼容方式：原版翻滚 / YSM蹲姿")
                .define("ysm_action_compat", "原版翻滚");
        NOTIFICATION_ENABLED = builder.comment("显示信息通知")
                .define("notification_enabled", true);
        builder.pop();
        SPEC = builder.build();
    }
    public static Operator operator() {
        ensureOperatorSelectionInitialized();
        Operator op = OPERATOR.get();
        return op == null || op == Operator.NONE ? Operator.LUNA : op;
    }
    private static synchronized void ensureOperatorSelectionInitialized() {
        if (!OPERATOR_SELECTION_INITIALIZED.get()) {
            OPERATOR.set(Operator.NONE); OPERATOR_SELECTION_INITIALIZED.set(true); SPEC.save();
        }
    }
    public static DashMode dashMode() { return DASH_MODE.get(); }
    public static String moduleKeys() { try { return MODULE_KEYS.get(); } catch (Exception e) { return ""; } }
    public static void saveModuleKeys(String value) {
        try { MODULE_KEYS.set(value == null ? "" : value); SPEC.save(); } catch (Exception ignored) { }
    }
    public static void selectOperator(Operator operator) {
        if (operator == null || operator == Operator.NONE) return;
        lol.silencecute.client.AccessClient.requestOperator(operator);
    }

    public static void selectOperatorLocally(Operator operator) { OPERATOR.set(operator); OPERATOR_SELECTION_INITIALIZED.set(true); SPEC.save(); }
    public static void selectDashMode(DashMode mode) { DASH_MODE.set(mode); SPEC.save(); }
    public static boolean lunaCrosshair() { return operatorRenderEnabled() && LUNA_CROSSHAIR.get(); }
    public static boolean showShockRadius() { return operatorRenderEnabled() && SHOW_SHOCK_RADIUS.get(); }
    public static boolean grenadeFovEffect() {
        return operatorRenderEnabled() && GRENADE_FOV_EFFECT.get();
    }
    public static int chamsAlpha() { return operatorRenderEnabled() ? CHAMS_ALPHA.get() : 0; }
    public static boolean chamsTeamColor() { return CHAMS_TEAM_COLOR.get(); }
    public static int chamsColor() { return (CHAMS_RED.get() << 16) | (CHAMS_GREEN.get() << 8) | CHAMS_BLUE.get(); }
    public static boolean chamsMarker() { return CHAMS_MARKER.get(); }
    public static FontMode guiFont() {
        try {
            FontMode mode = GUI_FONT.get();
            return mode == FontMode.MINECRAFT || mode == null ? FontMode.MORDEN : mode;
        } catch (Exception e) {
            return FontMode.MORDEN;
        }
    }
    public static boolean guiChinese() { return GUI_CHINESE.get(); }

    public static String guiStyle() {
        try {
            String style = GUI_STYLE.get();
            if (style != null && !style.isBlank()) cachedStyle = style;
        } catch (Exception ignored) {
        }
        return cachedStyle;
    }

    public static void setGuiStyle(String style) {
        try {
            GUI_STYLE.set(style == null ? "NAVEN" : style);
            SPEC.save();
        } catch (Exception ignored) {
        }
    }
    public static int guiBind() { try { return GUI_BIND.get(); } catch (Exception e) { return 344; } }
    public static int guiBindScan() { try { return GUI_BIND_SCAN.get(); } catch (Exception e) { return 54; } }
    public static void setGuiBindScan(int scan) { try { GUI_BIND_SCAN.set(scan); } catch (Exception ignore) { } }

    public static boolean navenBlur() { try { return NV_BLUR.get(); } catch (Exception e) { return false; } }
    public static void setNavenBlur(boolean on) { try { NV_BLUR.set(on); } catch (Exception ignore) { } }
    public static boolean hudSkills() { try { return HUD_SKILLS.get(); } catch (Exception e) { return true; } }
    public static void setHudSkills(boolean on) { try { HUD_SKILLS.set(on); } catch (Exception ignore) { } }
    public static boolean hudPassive() { try { return HUD_PASSIVE.get(); } catch (Exception e) { return true; } }
    public static void setHudPassive(boolean on) { try { HUD_PASSIVE.set(on); } catch (Exception ignore) { } }
    public static boolean hudHealth() { try { return HUD_HEALTH.get(); } catch (Exception e) { return true; } }
    public static void setHudHealth(boolean on) { try { HUD_HEALTH.set(on); } catch (Exception ignore) { } }

    public static String healthDisplayMode() {
        try {
            String mode = HEALTH_DISPLAY_MODE.get();
            if (mode != null && !mode.isBlank()) return mode;
        } catch (Exception ignored) {
        }
        return "TRUE_MAX";
    }
    public static void setHealthDisplayMode(String mode) {
        try { HEALTH_DISPLAY_MODE.set(mode == null ? "TRUE_MAX" : mode); } catch (Exception ignore) { }
    }

    public static boolean hudChat() { try { return HUD_CHAT.get(); } catch (Exception e) { return true; } }
    public static boolean hudPermission() { try { return HUD_PERMISSION.get(); } catch (Exception e) { return true; } }
    public static void setHudPermission(boolean on) { try { HUD_PERMISSION.set(on); } catch (Exception ignore) { } }
    public static void setHudChat(boolean on) { try { HUD_CHAT.set(on); } catch (Exception ignore) { } }

    public static int hudChatMaxMessages() {
        try { return Math.max(5, Math.min(15, HUD_CHAT_MAX_MESSAGES.get())); } catch (Exception e) { return 10; }
    }
    public static void setHudChatMaxMessages(int value) {
        try {
            HUD_CHAT_MAX_MESSAGES.set(Math.max(5, Math.min(15, value)));
            SPEC.save();
        } catch (Exception ignore) { }
    }

    public static int hudChatFadeTime() {
        try { return Math.max(1, Math.min(60, HUD_CHAT_FADE_TIME.get())); } catch (Exception e) { return 15; }
    }
    public static void setHudChatFadeTime(int seconds) {
        try {
            HUD_CHAT_FADE_TIME.set(Math.max(1, Math.min(60, seconds)));
            SPEC.save();
        } catch (Exception ignore) { }
    }
    public static boolean notificationRea1() { try { return NOTIFICATION_REA1.get(); } catch (Exception e) { return true; } }
    public static void setNotificationRea1(boolean on) { try { NOTIFICATION_REA1.set(on); } catch (Exception ignore) { } }
    public static boolean operatorEnabled() {
        return true;
    }
    public static void setOperatorEnabled(boolean on) { try { OPERATOR_ENABLED.set(on); } catch (Exception ignore) { } }
    public static boolean rendererEnabled() { try { return RENDERER_ENABLED.get(); } catch (Exception e) { return true; } }
    public static void setRendererEnabled(boolean on) { try { RENDERER_ENABLED.set(on); } catch (Exception ignore) { } }
    public static boolean operatorRenderEnabled() { try { return OPERATOR_RENDER_ENABLED.get(); } catch (Exception e) { return true; } }
    public static void setOperatorRenderEnabled(boolean on) { try { OPERATOR_RENDER_ENABLED.set(on); } catch (Exception ignore) { } }
    public static boolean weilongPlaceholder() { try { return WEILONG_PLACEHOLDER.get(); } catch (Exception e) { return false; } }
    public static boolean jifengPlaceholder() { try { return JIFENG_PLACEHOLDER.get(); } catch (Exception e) { return false; } }
    public static String operatorVoiceLang() {
        try {
            String v = OPERATOR_VOICE_LANGUAGE.get();
            return v == null || v.isEmpty() ? "英语" : v;
        } catch (Exception e) {
            return "英语";
        }
    }
    public static String yssmCompatMode() {
        try {
            String v = YSM_ACTION_COMPAT.get();
            if (v == null || v.isEmpty()) return "原版翻滚";
            if ("蹲姿翻滚".equals(v) || "智能".equals(v) || "调用API".equals(v) || "Smart".equals(v)) {
                return "YSM蹲姿";
            }
            if ("无动作".equals(v) || "No Animation".equals(v)) return "原版翻滚";
            return v;
        } catch (Exception e) {
            return "原版翻滚";
        }
    }
    public static void setYssmCompatMode(String mode) {
        try {
            YSM_ACTION_COMPAT.set(mode == null || mode.isEmpty() ? "原版翻滚" : mode);
            SPEC.save();
        } catch (Exception ignore) { }
    }
    public static void setOperatorVoiceLanguage(String language) {
        try {
            OPERATOR_VOICE_LANGUAGE.set(language == null || language.isEmpty() ? "英语" : language);
            SPEC.save();
        } catch (Exception ignore) { }
    }
    public static boolean notificationEnabled() { try { return NOTIFICATION_ENABLED.get(); } catch (Exception e) { return true; } }
    public static void setNotificationEnabled(boolean on) { try { NOTIFICATION_ENABLED.set(on); } catch (Exception ignore) { } }
    public static void setWeilongPlaceholder(boolean on) { try { WEILONG_PLACEHOLDER.set(on); } catch (Exception ignore) { } }
    public static void setJifengPlaceholder(boolean on) { try { JIFENG_PLACEHOLDER.set(on); } catch (Exception ignore) { } }
    public static float navenBlurStrength() { try { return NV_BLUR_STRENGTH.get().floatValue(); } catch (Exception e) { return 0.5f; } }
    public static void setNavenBlurStrength(float v) { try { NV_BLUR_STRENGTH.set((double) v); } catch (Exception ignore) { } }
    public static float navenBlurFps() {
        try {
            return NV_BLUR_FPS.get().floatValue();
        } catch (Exception e) {

            return 90.0f;
        }
    }
    public static int navenBgColor() {
        if (bgColorPreview != null) return bgColorPreview;
        try { return NV_BG_COLOR.get(); } catch (Exception e) { return 0x9A9A9A; }
    }
    public static void setAccentPreview(int rgb) { accentPreview = rgb & 0xFFFFFF; }
    public static void clearAccentPreview() { accentPreview = null; }
    public static void setBgColorPreview(int rgb) { bgColorPreview = rgb & 0xFFFFFF; }
    public static void clearBgColorPreview() { bgColorPreview = null; }
    public static void setNavenBgColor(int rgb) { try { NV_BG_COLOR.set(rgb & 0xFFFFFF); } catch (Exception ignore) { } }
    public static float navenBgAlpha() { try { return NV_BG_ALPHA.get().floatValue(); } catch (Exception e) { return 0.35f; } }
    public static void setNavenBgAlpha(float v) { try { NV_BG_ALPHA.set((double) v); } catch (Exception ignore) { } }
    public static void setGuiBind(int code) { try { GUI_BIND.set(code); SPEC.save(); } catch (Exception ignored) { } }
    public static lol.silencecute.api.GuiTheme guiTheme() { return GUI_THEME.get(); }

    private static String cachedStyle = "NAVEN";
    private static int cachedAccent = 0x3662EC;

    private static Integer accentPreview;
    private static Integer bgColorPreview;
    public static int guiAccent() {
        if (accentPreview != null) return accentPreview;
        try {
            int rgb = (GUI_ACCENT_RED.get() << 16) | (GUI_ACCENT_GREEN.get() << 8) | GUI_ACCENT_BLUE.get();
            if (rgb >= 0) cachedAccent = rgb;
        } catch (Exception ignored) {
        }
        return cachedAccent;
    }
    public static void setGuiAccent(int rgb) {
        GUI_ACCENT_RED.set((rgb >> 16) & 0xFF);
        GUI_ACCENT_GREEN.set((rgb >> 8) & 0xFF);
        GUI_ACCENT_BLUE.set(rgb & 0xFF);
    }

    private ModConfigData() {}
    public static void setNavenBlurFps(float v) {
        try { NV_BLUR_FPS.set((double) v); } catch (Exception ignore) { }
    }

}