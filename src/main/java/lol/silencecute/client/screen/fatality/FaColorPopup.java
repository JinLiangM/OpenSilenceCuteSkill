package lol.silencecute.client.screen.fatality;

import lol.silencecute.client.lang.Lang;

import java.awt.Color;
import java.util.Locale;

import lol.silencecute.client.screen.ColorValue;
import net.minecraft.client.Minecraft;

final class FaColorPopup extends FaPopup implements FaKeySink {
    private static final float PAD = 8.0f;
    private static final float SV_H = 118.0f;
    private static final float HUE_W = 9.0f;
    private static final float GAP = 6.0f;
    private static final float FOOT_H = 16.0f;
    private static final int[] HUES = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};

    private final ColorValue color;
    private final FaClickGui gui;
    private final FaTextInput hex = new FaTextInput(7);
    private float hue, saturation, brightness;
    private int lastWritten;
    private int dragging = -1;
    private boolean editing;

    private boolean dirty;

    FaColorPopup(Object owner, ColorValue color, FaClickGui gui) {
        super(owner);
        this.color = color;
        this.gui = gui;
        w = 180.0f;
        h = height();
        load(color.get());
        hex.onSubmit(this::applyHex).onCancel(() -> editing = false);
    }

    private float height() {
        return PAD + SV_H + GAP + FOOT_H + PAD - 2.0f;
    }

    private void load(int rgb) {
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
        if (hsb[1] > 0.001f && hsb[2] > 0.001f) hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        lastWritten = rgb & 0xFFFFFF;
    }

    private int current() {
        return Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
    }

    private void write() {
        int rgb = current();
        color.set(rgb);
        lastWritten = rgb;
        dirty = false;
    }

    private float svX() { return x + PAD; }
    private float svY() { return y + PAD; }
    private float svW() { return w - PAD * 2.0f - HUE_W - GAP; }
    private float hueX() { return svX() + svW() + GAP; }
    private float footY() { return svY() + SV_H + GAP; }

    @Override
    protected void layout(FaContext ctx) {
        h = height();
        if (editing || dragging >= 0) return;

        if (dirty) {
            write();
            return;
        }
        if (color.get() != lastWritten) load(color.get());
    }

    @Override
    protected void draw(FaContext ctx, float a) {
        float sx = svX(), sy = svY(), sw = svW();
        int pure = 0xFF000000 | (Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0xFFFFFF);
        ctx.draw.horizontal(sx, sy, sw, SV_H, FaTheme.alpha(0xFFFFFFFF, a), FaTheme.alpha(pure, a));
        ctx.draw.vertical(sx, sy, sw, SV_H, 0x00000000, FaTheme.alpha(0xFF000000, a));
        float knobX = sx + saturation * sw, knobY = sy + (1.0f - brightness) * SV_H;
        ctx.draw.outline(knobX - 3.5f, knobY - 3.5f, 7.0f, 7.0f, 3.5f, 1.0f, FaTheme.alpha(0xCC000000, a));
        ctx.draw.outline(knobX - 3.0f, knobY - 3.0f, 6.0f, 6.0f, 3.0f, 1.0f, FaTheme.alpha(0xFFFFFFFF, a));

        float hx = hueX();
        float segment = SV_H / 6.0f;
        for (int i = 0; i < 6; i++) {
            ctx.draw.vertical(hx, sy + i * segment, HUE_W, segment + 0.5f, FaTheme.alpha(HUES[i], a),
                    FaTheme.alpha(HUES[i + 1], a));
        }
        float hueY = sy + hue * SV_H;
        ctx.draw.rect(hx - 1.5f, hueY - 1.0f, HUE_W + 3.0f, 2.0f, 1.0f, FaTheme.alpha(0xFFFFFFFF, a));

        float fy = footY(), mid = fy + FOOT_H * 0.5f;
        boolean copyHover = FaContext.inside(ctx.mouseX, ctx.mouseY, svX(), fy, 14, FOOT_H);
        boolean pasteHover = FaContext.inside(ctx.mouseX, ctx.mouseY, svX() + 18, fy, 14, FOOT_H);
        ctx.draw.icon(FaTheme.I_COPY, svX() + 7.0f, mid, 10.0f,
                FaTheme.alpha(copyHover ? FaTheme.ICON_HOVER : FaTheme.ICON, a));
        ctx.draw.icon(FaTheme.I_PASTE, svX() + 25.0f, mid, 10.0f,
                FaTheme.alpha(pasteHover ? FaTheme.ICON_HOVER : FaTheme.ICON, a));
        FaFont font = FaTheme.field();
        float textX = svX() + 42.0f;
        ctx.draw.rect(textX - 6.0f, fy, x + w - PAD - textX + 6.0f, FOOT_H, FaTheme.FIELD_RADIUS,
                FaTheme.alpha(editing ? FaTheme.FIELD_HOVER : FaTheme.FIELD, a));
        if (editing) {
            hex.render(ctx.draw, textX, mid, x + w - PAD - textX, font, FaTheme.alpha(FaTheme.FIELD_TEXT_STRONG, a), true, "", 0);
        } else {
            ctx.draw.textMid(hexString(current()), textX, mid, font, FaTheme.alpha(FaTheme.FIELD_TEXT_STRONG, a));
        }
    }

    static String hexString(int rgb) {
        return String.format(Locale.ROOT, "#%06x", rgb & 0xFFFFFF);
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (button != 0) return true;
        if (FaContext.inside(mx, my, svX(), svY(), svW(), SV_H)) dragging = 0;
        else if (FaContext.inside(mx, my, hueX() - 2, svY(), HUE_W + 4, SV_H)) dragging = 1;
        if (dragging >= 0) {
            drag(mx, my);
            return true;
        }
        float fy = footY();
        if (FaContext.inside(mx, my, svX(), fy, 14, FOOT_H)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(hexString(current()));
            gui.toast(Lang.tr("gui.color.copied") + hexString(current()));
        } else if (FaContext.inside(mx, my, svX() + 18, fy, 14, FOOT_H)) {
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            Integer parsed = clipboard == null ? null : parseHex(clipboard.trim());
            if (parsed != null) {
                load(parsed);
                write();
            }
        } else if (FaContext.inside(mx, my, svX() + 36, fy, w - PAD * 2 - 36, FOOT_H)) {
            editing = true;
            hex.set(hexString(current()));
            hex.focused();
            gui.focus(this);
        }
        return true;
    }

    @Override
    void mouseDragged(FaContext ctx, double mx, double my) {
        if (dragging >= 0) drag(mx, my);
    }

    @Override
    void mouseReleased() {
        dragging = -1;
        if (dirty) write();
    }

    private void drag(double mx, double my) {
        switch (dragging) {
            case 0 -> {
                saturation = clamp((float) ((mx - svX()) / svW()));
                brightness = 1.0f - clamp((float) ((my - svY()) / SV_H));
            }
            case 1 -> hue = Math.min(0.9999f, clamp((float) ((my - svY()) / SV_H)));
            default -> {
                return;
            }
        }

        dirty = true;
    }

    private void applyHex(String text) {
        Integer parsed = parseHex(text.trim());
        if (parsed != null) {
            load(parsed);
            write();
        }
        editing = false;
        gui.focus(null);
    }

    static Integer parseHex(String text) {
        if (text == null) return null;
        String value = text.startsWith("#") ? text.substring(1) : text;
        try {
            if (value.length() == 3) {
                StringBuilder expanded = new StringBuilder();
                for (char c : value.toCharArray()) expanded.append(c).append(c);
                return Integer.parseInt(expanded.toString(), 16) & 0xFFFFFF;
            }
            if (value.length() == 6) return Integer.parseInt(value, 16) & 0xFFFFFF;
            if (value.length() == 8) return (int) (Long.parseLong(value, 16) & 0xFFFFFFL);
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        return editing && hex.keyPressed(key, modifiers);
    }

    @Override
    public boolean charTyped(char character) {
        return editing && (Character.digit(character, 16) >= 0 || character == '#') && hex.charTyped(character);
    }

    @Override
    public void focusLost() {
        editing = false;
    }

    @Override
    protected void onClose() {
        if (editing) gui.focus(null);
    }
}
