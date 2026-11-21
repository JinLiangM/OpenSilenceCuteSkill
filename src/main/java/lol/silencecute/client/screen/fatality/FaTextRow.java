package lol.silencecute.client.screen.fatality;

import lol.silencecute.client.screen.TextValue;

final class FaTextRow extends FaRow implements FaKeySink {
    private final TextValue text;
    private final FaTextInput input = new FaTextInput(128);
    private boolean editing;
    private String before = "";

    FaTextRow(TextValue text) {
        super(text, text.displayName());
        this.text = text;
        input.onChange(text::set)
                .onSubmit(value -> editing = false)
                .onCancel(() -> {
                    text.set(before);
                    editing = false;
                });
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float fw = FaTheme.FIELD_W, fh = FaTheme.FIELD_H;
        float fx = fieldX(fw), fy = fieldY();
        drawLabel(ctx, x, centerY, fx - x - FaTheme.ROW_PAD - 6.0f, alpha);
        boolean hot = editing || ctx.hovered(fx, fy, fw, fh);
        ctx.draw.rect(fx, fy, fw, fh, FaTheme.FIELD_RADIUS,
                FaTheme.alpha(hot ? FaTheme.FIELD_HOVER : FaTheme.FIELD, alpha));
        FaFont font = FaTheme.field();
        if (editing) {
            input.render(ctx.draw, fx + 6.0f, centerY, fw - 12.0f, font, FaTheme.alpha(FaTheme.LABEL, alpha),
                    true, "", 0);
            ctx.draw.outline(fx, fy, fw, fh, FaTheme.FIELD_RADIUS, 0.5f, FaTheme.alpha(FaTheme.ACCENT_DIM, alpha));
        } else {
            String value = text.get() == null || text.get().isEmpty() ? "Empty" : text.get();
            String shown = FaTheme.fit(value, font, fw - 12.0f);
            ctx.draw.textCenter(shown, fx + fw * 0.5f, centerY, font,
                    FaTheme.alpha(hot ? FaTheme.FIELD_TEXT_HOVER : FaTheme.FIELD_TEXT, alpha));
        }
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        float fx = fieldX(FaTheme.FIELD_W);
        if (button != 0 || !shown || !FaContext.inside(mx, my, fx, fieldY(), FaTheme.FIELD_W, FaTheme.FIELD_H)) {
            return false;
        }
        if (!editing) {
            before = text.get() == null ? "" : text.get();
            input.set(before);
            input.focused();
            editing = true;
            ctx.gui.focus(this);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        if (!editing) return false;
        input.keyPressed(key, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(char character) {
        return editing && input.charTyped(character);
    }

    @Override
    public void focusLost() {
        editing = false;
    }

    boolean editing() {
        return editing;
    }
}
