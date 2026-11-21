package lol.silencecute.client.screen.fatality;

import java.util.Locale;

import lol.silencecute.client.screen.ColorValue;

final class FaColorRow extends FaRow {

    private static final float DOT = 11.0f;

    private final ColorValue color;

    FaColorRow(ColorValue color) {
        super(color, color.displayName());
        this.color = color;
    }

    private float dotX() {
        return right() - DOT;
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float cx = dotX(), cy = centerY - DOT * 0.5f;
        drawLabel(ctx, x, centerY, cx - x - FaTheme.ROW_PAD - 8.0f, alpha);
        int rgb = 0xFF000000 | color.get();

        ctx.draw.rect(cx, cy, DOT, DOT, DOT * 0.5f, FaTheme.alpha(rgb, alpha));
        int edge = FaTheme.mix(rgb, 0xFF000000, 0.35f);
        ctx.draw.outline(cx, cy, DOT, DOT, DOT * 0.5f, 1.0f, FaTheme.alpha(edge, alpha));
        boolean open = ctx.gui.isPopupOpen(this);
        if (open || ctx.hovered(cx - 2.0f, cy - 2.0f, DOT + 4.0f, DOT + 4.0f)) {
            ctx.draw.outline(cx - 1.5f, cy - 1.5f, DOT + 3.0f, DOT + 3.0f, (DOT + 3.0f) * 0.5f, 0.7f,
                    FaTheme.alpha(open ? FaTheme.ACCENT : FaTheme.CHIP_OUTLINE, alpha));
        }
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        float cx = dotX(), cy = centerY() - DOT * 0.5f;
        if (button != 0 || !shown || !FaContext.inside(mx, my, cx - 3.0f, cy - 3.0f, DOT + 6.0f, DOT + 6.0f)) {
            return false;
        }
        ctx.gui.togglePopup(this, () -> new FaColorPopup(this, color, ctx.gui), cx - 14.0f, cy, DOT, DOT, false);
        return true;
    }

    @Override
    boolean matches(String query) {
        return super.matches(query) || FaColorPopup.hexString(color.get()).toLowerCase(Locale.ROOT).contains(query);
    }
}
