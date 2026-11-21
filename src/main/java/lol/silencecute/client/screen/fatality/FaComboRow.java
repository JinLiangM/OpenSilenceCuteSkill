package lol.silencecute.client.screen.fatality;

import java.util.Locale;

import lol.silencecute.client.lang.Lang;
import lol.silencecute.client.screen.ModeValue;

final class FaComboRow extends FaRow {
    private final ModeValue value;
    private final FaListPopup.Model model;
    private final FaMotion chevron = new FaMotion(0.0f);

    private FaComboRow(ModeValue mode, FaListPopup.Model model) {
        super(mode, mode.displayName());
        this.value = mode;
        this.model = model;
    }

    static FaComboRow single(ModeValue mode) {
        FaListPopup.Model model = new FaListPopup.Model() {
            @Override public int size() { return mode.modes.length; }
            @Override public String label(int index) { return display(mode.modes[index]); }
            @Override public boolean selected(int index) { return mode.is(mode.modes[index]); }
            @Override public void click(int index) { mode.set(mode.modes[index]); }
        };
        return new FaComboRow(mode, model);
    }

    static String display(String mode) {
        if (mode == null || mode.isEmpty()) return "";
        String spaced = Lang.option(mode).replace('_', ' ').toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(spaced.length());
        boolean start = true;
        for (char c : spaced.toCharArray()) {
            out.append(start ? Character.toUpperCase(c) : c);
            start = c == ' ';
        }
        return out.toString();
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float fw = FaTheme.FIELD_W, fh = FaTheme.FIELD_H;
        float fx = fieldX(fw), fy = fieldY();
        drawLabel(ctx, x, centerY, fx - x - FaTheme.ROW_PAD - 6.0f, alpha);
        boolean open = ctx.gui.isPopupOpen(this);
        boolean hot = open || ctx.hovered(fx, fy, fw, fh);
        drawField(ctx, fx, fy, fw, fh, current(), hot, alpha);
        float turn = chevron.toward(open ? 1.0f : 0.0f, ctx.dt, 18.0f);
        ctx.draw.icon(turn > 0.5f ? FaTheme.I_CHEVRON_UP : FaTheme.I_CHEVRON_DOWN, fx + fw - 9.0f, centerY,
                9.5f, FaTheme.alpha(FaTheme.CHEVRON, alpha));
    }

    private String current() {
        for (int i = 0; i < model.size(); i++) if (model.selected(i)) return model.label(i);
        return model.size() > 0 ? model.label(0) : "";
    }

    static void drawField(FaContext ctx, float fx, float fy, float fw, float fh, String text, boolean hot,
                          float alpha) {
        ctx.draw.rect(fx, fy, fw, fh, FaTheme.FIELD_RADIUS,
                FaTheme.alpha(hot ? FaTheme.FIELD_HOVER : FaTheme.FIELD, alpha));
        FaFont font = FaTheme.field();
        String shown = FaTheme.fit(text, font, fw - 24.0f);
        ctx.draw.textMid(shown, fx + 7.0f, fy + fh * 0.5f, font,
                FaTheme.alpha(hot ? FaTheme.FIELD_TEXT_HOVER : FaTheme.FIELD_TEXT, alpha));
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        float fx = fieldX(FaTheme.FIELD_W), fy = fieldY();
        if (button != 0 || !shown || !FaContext.inside(mx, my, fx, fy, FaTheme.FIELD_W, FaTheme.FIELD_H)) {
            return false;
        }
        if (this.value.rejectLocked()) {
            return true;
        }
        ctx.gui.togglePopup(this, () -> new FaListPopup(this, model, FaTheme.FIELD_W), fx, fy, FaTheme.FIELD_W,
                FaTheme.FIELD_H, false);
        return true;
    }

    @Override
    boolean matches(String query) {
        if (super.matches(query)) return true;
        for (int i = 0; i < model.size(); i++) if (model.label(i).toLowerCase(Locale.ROOT).contains(query)) return true;
        return false;
    }
}
