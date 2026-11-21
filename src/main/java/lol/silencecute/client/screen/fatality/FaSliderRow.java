package lol.silencecute.client.screen.fatality;

import lol.silencecute.client.screen.NumberValue;
import org.lwjgl.glfw.GLFW;

final class FaSliderRow extends FaRow implements FaKeySink {
    static final float FILL_INSET = 1.5f;

    static final float VALUE_DX = 0.0f;
    static final float VALUE_DY = 0.0f;

    private final NumberValue number;
    private final FaMotion fill;
    private final FaTextInput entry = new FaTextInput(16);
    private boolean dragging;
    private boolean editing;

    private float preview = Float.NaN;

    FaSliderRow(NumberValue number) {
        super(number, number.displayName());
        this.number = number;
        this.fill = new FaMotion(fraction());
        entry.onSubmit(this::applyEntry).onCancel(() -> editing = false);
    }

    private float fraction() {
        float min = number.min, max = number.max;
        return max <= min ? 0.0f : (current() - min) / (max - min);
    }

    private float current() {
        return Float.isNaN(preview) ? number.get() : preview;
    }

    private void commit() {
        if (Float.isNaN(preview)) return;
        float value = preview;
        preview = Float.NaN;
        number.set(value);
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float fw = FaTheme.FIELD_W, fh = FaTheme.SLIDER_H;
        float fx = fieldX(fw), fy = sliderY();
        drawLabel(ctx, x, centerY, fx - x - FaTheme.ROW_PAD - 6.0f, alpha);
        boolean hot = dragging || ctx.hovered(fx, fieldY(), fw, FaTheme.FIELD_H);
        ctx.draw.rect(fx, fy, fw, fh, FaTheme.FIELD_RADIUS,
                FaTheme.alpha(hot ? FaTheme.FIELD_HOVER : FaTheme.FIELD, alpha));
        FaFont font = FaTheme.field();

        if (!dragging && !Float.isNaN(preview)) commit();
        if (editing) {
            entry.render(ctx.draw, fx + 6.0f, centerY, fw - 12.0f, font, FaTheme.alpha(FaTheme.LABEL, alpha),
                    true, "", 0);
            ctx.draw.outline(fx, fy, fw, fh, FaTheme.FIELD_RADIUS, 0.5f, FaTheme.alpha(FaTheme.ACCENT_DIM, alpha));
            return;
        }
        float t = Math.max(0.0f, Math.min(1.0f, fill.toward(fraction(), ctx.dt, dragging ? 40.0f : 18.0f)));

        float inset = FILL_INSET, innerW = fw - inset * 2.0f, innerH = fh - inset * 2.0f;
        float fillW = innerW * t;
        if (fillW > 0.25f) {
            ctx.draw.rect(fx + inset, fy + inset, Math.max(fillW, Math.min(innerW, innerH * 0.6f)), innerH,
                    FaTheme.FIELD_RADIUS - 0.5f, FaTheme.alpha(FaTheme.ACCENT, alpha));
        }
        String text = formatNumber(current(), number.inc);

        float textX = fx + (fw - font.width(text)) * 0.5f + VALUE_DX;
        int grey = FaTheme.alpha(hot ? FaTheme.FIELD_TEXT_HOVER : FaTheme.FIELD_TEXT, alpha);
        int color = valueColor(fx + inset + fillW - textX, grey, FaTheme.alpha(FaTheme.SLIDER_TEXT, alpha));

        ctx.draw.textMid(text, textX, centerY + VALUE_DY - 1, font, color);
    }

    static int valueColor(float cover, int grey, int white) {
        return FaTheme.mix(grey, white, cover / 4.0f);
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        float fx = fieldX(FaTheme.FIELD_W), fy = fieldY();
        if (!shown || !FaContext.inside(mx, my, fx, fy, FaTheme.FIELD_W, FaTheme.FIELD_H)) return false;
        if (button == 1) {
            editing = true;
            entry.set(formatNumber(current(), number.inc));
            ctx.gui.focus(this);
            entry.focused();
            return true;
        }
        if (button != 0) return false;
        if (editing) return true;
        dragging = true;
        apply(mx);
        return true;
    }

    @Override
    void mouseDragged(FaContext ctx, double mx, double my) {
        if (dragging) apply(mx);
    }

    @Override
    void mouseReleased() {
        dragging = false;
        commit();
    }

    @Override
    boolean mouseScrolled(FaContext ctx, double mx, double my, double amount) {
        if (!shown || !FaContext.inside(mx, my, fieldX(FaTheme.FIELD_W), fieldY(), FaTheme.FIELD_W, FaTheme.FIELD_H)) {
            return false;
        }
        float step = number.inc > 0.0f ? number.inc : (number.max - number.min) / 100.0f;
        set(number.get() + Math.signum(amount) * step);
        return true;
    }

    private void apply(double mx) {
        float fx = fieldX(FaTheme.FIELD_W) + FILL_INSET;
        double t = Math.max(0.0, Math.min(1.0, (mx - fx) / (FaTheme.FIELD_W - FILL_INSET * 2.0f)));

        float raw = (float) (number.min + t * (number.max - number.min));
        preview = (float) (Math.round(raw * 1e6) / 1e6);
    }

    private void set(double raw) {

        double cleaned = Math.round(raw * 1e6) / 1e6;
        number.set((float) cleaned);
    }

    private void applyEntry(String text) {
        try {
            set(Double.parseDouble(text.trim().replace(',', '.')));
        } catch (NumberFormatException ignored) {
        }
        editing = false;
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        if (!editing) return false;
        boolean used = entry.keyPressed(key, modifiers);
        if (!editing) return true;
        return used || key == GLFW.GLFW_KEY_TAB;
    }

    @Override
    public boolean charTyped(char character) {
        return editing && (Character.isDigit(character) || character == '.' || character == '-' || character == ',')
                && entry.charTyped(character);
    }

    @Override
    public void focusLost() {
        if (editing) applyEntry(entry.text());
    }

    boolean editing() {
        return editing;
    }
}
