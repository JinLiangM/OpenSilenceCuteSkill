package lol.silencecute.client.screen.fatality;

import lol.silencecute.api.GuiTheme;
import lol.silencecute.api.FontMode;
import lol.silencecute.modules.Category;
import lol.silencecute.config.ModConfigData;

final class FaTheme {

    static int WINDOW;
    static int WINDOW_SHADOW;
    static int HEADER_TOP;
    static int HEADER;
    static int FOOTER;
    static int DIVIDER;
    static int GROUP;
    static int GROUP_BORDER;
    static int FIELD;
    static int FIELD_HOVER;
    static int TAB_ACTIVE;
    static int POPUP;
    static int POPUP_BORDER;
    static int SHADOW;

    static int LOGO;
    static int TITLE;
    static int LABEL;
    static int LABEL_HOVER;
    static int LABEL_DISABLED;
    static int MUTED;
    static int FIELD_TEXT;
    static int FIELD_TEXT_HOVER;

    static int FIELD_TEXT_STRONG;
    static int TAB_TEXT;
    static int TAB_TEXT_HOVER;
    static int TAB_TEXT_ACTIVE;
    static int SIDE_TEXT;
    static int SIDE_TEXT_ENABLED;
    static int SIDE_TEXT_ACTIVE;
    static int ICON;
    static int ICON_FAINT;
    static int ICON_HOVER;
    static int CHEVRON;
    static int TICK;

    static int ACCENT;

    static int ACCENT_DIM;

    static int ACCENT_LIGHT;

    static final int ORANGE = 0xFFF5A35D;

    static int SHADE_DARK;
    static int SHADE_CLEAR;

    static int USER_NAME;

    static int PORTRAIT_BG;

    static int GRIP;

    static int SCROLLBAR;

    static int SLIDER_TEXT;

    static int LIST_TEXT;
    static int LIST_TEXT_HOVER;

    static int CHIP_OUTLINE;

    static final float DEFAULT_W = 744.0f;
    static final float DEFAULT_H = 540.0f;
    static final float MIN_W = 500.0f;
    static final float MIN_H = 320.0f;
    static final float RADIUS = 4.0f;
    static final float HEADER_H = 40.0f;
    static final float FOOTER_H = 22.0f;
    static final float SIDEBAR_W = 98.0f;
    static final float CONTENT_PAD = 10.0f;
    static final float COLUMN_GAP = 13.5f;
    static final float GROUP_GAP = 16.0f;
    static final float GROUP_RADIUS = 3.5f;

    static final float GROUP_TITLE_H = 15.0f;
    static final float GROUP_BOTTOM = 10.0f;
    static final float ROW_H = 25.5f;
    static final float ROW_PAD = 15.0f;
    static final float FIELD_W = 72.0f;
    static final float FIELD_H = 19.0f;
    static final float SLIDER_H = 13.0f;
    static final float FIELD_RADIUS = 2.5f;
    static final float CHECK = 10.5f;
    static final float CHIP_W = 17.5f;
    static final float CHIP_H = 9.0f;
    static final float GEAR = 10.5f;
    static final float MIN_COLUMN_W = 188.0f;

    static final String I_COMBAT = "\uE0B0";
    static final String I_MOVEMENT = "\uE3BC";
    static final String I_PLAYER = "\uE19E";
    static final String I_WORLD = "\uE0EB";
    static final String I_RENDER = "\uE0BE";
    static final String I_MISC = "\uE157";

    static final String I_HUD = "\uE156";
    static final String I_GEAR = "\uE157";
    static final String I_SEARCH = "\uE154";
    static final String I_SAVE = "\uE150";
    static final String I_FOLDER = "\uE0DC";
    static final String I_INFO = "\uE0FE";
    static final String I_CHEVRON_DOWN = "\uE071";
    static final String I_CHEVRON_UP = "\uE074";
    static final String I_CHECK = "\uE070";
    static final String I_COPY = "\uE0A2";
    static final String I_PASTE = "\uE3EB";
    static final String I_CLOSE = "\uE1B1";
    static final String I_PLUS = "\uE140";
    static final String I_KEYBOARD = "\uE283";
    static final String I_EYE = "\uE0BE";
    static final String I_EYE_OFF = "\uE0BF";

    private static GuiTheme appliedTheme;
    private static int appliedAccent = -1;

    private FaTheme() {
    }

    static void refresh() {
        GuiTheme theme = GuiTheme.DARK;
        int accent = 0xFF615A;
        try {
            theme = ModConfigData.guiTheme();
            accent = ModConfigData.guiAccent() & 0xFFFFFF;
        } catch (Exception ignored) {

        }
        if (theme == appliedTheme && accent == appliedAccent) return;
        appliedTheme = theme;
        appliedAccent = accent;
        if (theme == GuiTheme.LIGHT) light(accent);
        else dark(accent);
    }

    private static void dark(int rgb) {
        WINDOW = 0xFF151515;
        WINDOW_SHADOW = 0xA0000000;
        HEADER_TOP = 0xFF191919;
        HEADER = 0xFF1C1C1C;
        FOOTER = 0xFF1C1C1C;
        DIVIDER = 0xFF111111;
        GROUP = 0xFF1C1C1C;
        GROUP_BORDER = 0xFF222222;
        FIELD = 0xFF161616;
        FIELD_HOVER = 0xFF191919;
        TAB_ACTIVE = 0xFF222222;
        POPUP = 0xFF1C1C1C;
        POPUP_BORDER = 0xFF242424;
        SHADOW = 0x8C000000;

        LOGO = 0xFFF2F0F5;
        TITLE = 0xFFEDEDED;
        LABEL = 0xFFE4E4E4;
        LABEL_HOVER = 0xFFF4F4F4;
        LABEL_DISABLED = 0xFF3A3A3A;
        MUTED = 0xFF666666;
        FIELD_TEXT = 0xFF686868;
        FIELD_TEXT_HOVER = 0xFF959595;
        FIELD_TEXT_STRONG = 0xFFDDDDDE;
        TAB_TEXT = 0xFF666666;
        TAB_TEXT_HOVER = 0xFF9A9A9A;
        TAB_TEXT_ACTIVE = 0xFFEAEAEA;
        SIDE_TEXT = 0xFF666666;
        SIDE_TEXT_ENABLED = 0xFF9C9C9C;
        SIDE_TEXT_ACTIVE = 0xFFEDEDED;
        ICON = 0xFF6A6A6A;
        ICON_FAINT = 0xFF444444;
        ICON_HOVER = 0xFFD2D2D2;
        CHEVRON = 0xFFCACACA;
        TICK = 0xFFF6F6F6;

        SHADE_DARK = 0xB0101010;
        SHADE_CLEAR = 0x00101010;
        USER_NAME = 0xFFDDDDDD;
        PORTRAIT_BG = 0xFF2A2A2A;
        GRIP = 0xFF2E2E2E;
        SCROLLBAR = 0xFF333333;
        SLIDER_TEXT = 0xFFFFFFFF;
        LIST_TEXT = 0xFFB4B4B4;
        LIST_TEXT_HOVER = 0xFFF4F4F4;
        CHIP_OUTLINE = 0x66FFFFFF;

        accent(rgb);
    }

    private static void light(int rgb) {
        WINDOW = 0xFFEDEDF0;
        WINDOW_SHADOW = 0x33000000;
        HEADER_TOP = 0xFFF8F8FA;
        HEADER = 0xFFF2F2F5;
        FOOTER = 0xFFF2F2F5;
        DIVIDER = 0xFFD9D9DF;
        GROUP = 0xFFF9F9FB;
        GROUP_BORDER = 0xFFE1E1E7;
        FIELD = 0xFFEBEBEF;
        FIELD_HOVER = 0xFFE2E2E8;
        TAB_ACTIVE = 0xFFE5E5EA;
        POPUP = 0xFFFBFBFC;
        POPUP_BORDER = 0xFFDFDFE4;
        SHADOW = 0x40000000;

        LOGO = 0xFF101013;
        TITLE = 0xFF1C1C20;
        LABEL = 0xFF2E2E33;
        LABEL_HOVER = 0xFF0C0C0F;
        LABEL_DISABLED = 0xFFB8B8BE;
        MUTED = 0xFF8A8A92;
        FIELD_TEXT = 0xFF8A8A92;
        FIELD_TEXT_HOVER = 0xFF4A4A52;
        FIELD_TEXT_STRONG = 0xFF2A2A30;
        TAB_TEXT = 0xFF8A8A92;
        TAB_TEXT_HOVER = 0xFF5A5A62;
        TAB_TEXT_ACTIVE = 0xFF14141A;
        SIDE_TEXT = 0xFF8A8A92;
        SIDE_TEXT_ENABLED = 0xFF5A5A62;
        SIDE_TEXT_ACTIVE = 0xFF14141A;
        ICON = 0xFF7A7A82;
        ICON_FAINT = 0xFFB4B4BA;
        ICON_HOVER = 0xFF3A3A42;
        CHEVRON = 0xFF5A5A62;
        TICK = 0xFF2A2A30;

        SHADE_DARK = 0x18000000;
        SHADE_CLEAR = 0x00000000;
        USER_NAME = 0xFF2E2E33;
        PORTRAIT_BG = 0xFFD8D8DE;
        GRIP = 0xFFCFCFD6;
        SCROLLBAR = 0xFFC4C4CB;
        SLIDER_TEXT = 0xFF101013;
        LIST_TEXT = 0xFF6A6A72;
        LIST_TEXT_HOVER = 0xFF14141A;
        CHIP_OUTLINE = 0x66000000;

        accent(rgb);
    }

    private static void accent(int rgb) {
        ACCENT = 0xFF000000 | (rgb & 0xFFFFFF);
        ACCENT_DIM = mix(ACCENT, 0xFF000000, 0.45f);
        ACCENT_LIGHT = mix(ACCENT, 0xFFFFFFFF, 0.35f);
    }

    static FaFont logo() { return FaFont.GAME.logo(); }

    static boolean vanilla() {
        return ModConfigData.guiFont() == FontMode.MINECRAFT;
    }

    static FaFont tab() { return FaFont.GAME.tab(); }
    static FaFont title() { return FaFont.GAME.title(); }
    static FaFont label() { return FaFont.GAME.label(); }
    static FaFont field() { return FaFont.GAME.field(); }
    static FaFont small() { return FaFont.GAME.small(); }
    static FaFont icon(float size) { return FaFont.GAME.icon(size); }

    static String icon(Category category) {
        return switch (category) {
            case RENDER -> I_RENDER;
            case MISC -> I_MISC;
            case ADMIN -> I_MISC;
        };
    }

    static int alpha(int argb, float alpha) {
        int a = Math.round((argb >>> 24) * Math.max(0.0f, Math.min(1.0f, alpha)));
        return (argb & 0x00FFFFFF) | a << 24;
    }

    static int mix(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int a = Math.round(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * t);
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    static String fit(String text, FaFont font, float width) {
        if (text == null) return "";
        if (font.width(text) <= width) return text;
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + ellipsis) > width) end--;
        return end <= 0 ? ellipsis : text.substring(0, end).stripTrailing() + ellipsis;
    }
}