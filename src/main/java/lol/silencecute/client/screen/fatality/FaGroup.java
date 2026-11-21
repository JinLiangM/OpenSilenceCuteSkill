package lol.silencecute.client.screen.fatality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import lol.silencecute.client.screen.BoolValue;
import lol.silencecute.client.screen.ColorValue;
import lol.silencecute.client.screen.KeyValue;
import lol.silencecute.client.screen.ModeValue;
import lol.silencecute.client.lang.Lang;
import lol.silencecute.modules.Module;
import lol.silencecute.client.screen.NumberValue;
import lol.silencecute.client.screen.TextValue;
import lol.silencecute.client.screen.Value;

final class FaGroup extends FaBox {

    private static final float TITLE_RISE = 1.5f;

    final Module module;
    private final List<FaRow> rows = new ArrayList<>();
    private final boolean[] matched;
    private final FaMotion x = new FaMotion(Float.NaN);
    private final FaMotion y = new FaMotion(Float.NaN);
    private final FaMotion flash = new FaMotion(0.0f);
    private float targetX, targetY;
    private float lastX, lastY, lastW, lastH;
    private boolean drawn;
    private boolean filtered;

    FaGroup(Module module) {
        this.module = module;
        if (module.enableRow) rows.add(new FaEnableRow(module));
        for (Value<?> value : module.values) add(value, rows);
        matched = new boolean[rows.size()];
        Arrays.fill(matched, true);
    }

    private static void add(Value<?> value, List<FaRow> into) {
        FaRow row = create(value);
        if (row != null) into.add(row);
    }

    private static FaRow create(Value<?> value) {

        if (value.navenOnly) return null;
        if (value instanceof BoolValue bool) {
            return new FaCheckRow(bool, bool.displayName(), bool::get, bool::set);
        }
        if (value instanceof KeyValue key) return new FaKeyRow(key);
        if (value instanceof NumberValue number) return new FaSliderRow(number);
        if (value instanceof ModeValue mode) return FaComboRow.single(mode);
        if (value instanceof ColorValue color) return new FaColorRow(color);
        if (value instanceof TextValue text) return new FaTextRow(text);
        return null;
    }

    String title() {

        return Lang.byZh(module.name);
    }

    boolean filter(String query) {
        filtered = !query.isEmpty();
        if (!filtered) {
            Arrays.fill(matched, true);
            return true;
        }
        boolean titleMatch = title().toLowerCase(Locale.ROOT).contains(query);
        boolean any = titleMatch;
        for (int i = 0; i < rows.size(); i++) {
            matched[i] = titleMatch || i == 0 || rows.get(i).matches(query);
            if (i > 0 && matched[i] && !titleMatch) any = true;
        }
        return any;
    }

    float targetHeight() {
        float height = FaTheme.GROUP_TITLE_H + FaTheme.GROUP_BOTTOM;
        for (int i = 0; i < rows.size(); i++) if (matched[i]) height += rows.get(i).targetHeight();
        return height;
    }

    void place(float targetX, float targetY, boolean snap) {
        this.targetX = targetX;
        this.targetY = targetY;
        if (snap || Float.isNaN(x.value())) {
            x.snap(targetX);
            y.snap(targetY);
        }
    }

    float targetY() {
        return targetY;
    }

    void flash() {
        flash.snap(1.0f);
    }

    void render(FaContext ctx, float originX, float originY, float width, float viewTop, float viewBottom, float alpha) {
        float gx = originX + x.toward(targetX, ctx.dt, 16.0f);
        float gy = originY + y.toward(targetY, ctx.dt, 16.0f);
        float height = FaTheme.GROUP_TITLE_H + FaTheme.GROUP_BOTTOM;
        float[] rowHeights = new float[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            rowHeights[i] = matched[i] ? rows.get(i).height(ctx.dt) : 0.0f;
            height += rowHeights[i];
        }
        lastX = gx;
        lastY = gy;
        lastW = width;
        lastH = height;
        drawn = gy + height >= viewTop && gy <= viewBottom;
        float glow = flash.toward(0.0f, ctx.dt, 2.2f);
        if (!drawn) {
            for (FaRow row : rows) row.shown = false;
            return;
        }

        ctx.draw.rect(gx, gy, width, height, FaTheme.GROUP_RADIUS, FaTheme.alpha(FaTheme.GROUP, alpha));
        ctx.draw.outline(gx, gy, width, height, FaTheme.GROUP_RADIUS, 0.5f,
                FaTheme.alpha(FaTheme.mix(FaTheme.GROUP_BORDER, FaTheme.ACCENT_DIM, glow), alpha));

        FaFont titleFont = FaTheme.title();
        ctx.draw.textMid(FaTheme.fit(title(), titleFont, width - 20.0f), gx + 13.5f, gy + TITLE_RISE + 9, titleFont,
                FaTheme.alpha(FaTheme.mix(FaTheme.TITLE, FaTheme.ACCENT, glow), alpha));

        float rowY = gy + FaTheme.GROUP_TITLE_H;
        for (int i = 0; i < rows.size(); i++) {
            float rowHeight = rowHeights[i];
            FaRow row = rows.get(i);
            if (rowHeight <= 0.01f || rowY + rowHeight < viewTop || rowY > viewBottom) {
                row.shown = false;
                rowY += rowHeight;
                continue;
            }
            float rowAlpha = alpha * row.visibility();
            if (rowHeight < FaTheme.ROW_H - 0.05f) {
                try (var clip = ctx.draw.clip(gx, rowY, width, rowHeight)) {
                    row.render(ctx, gx, rowY, width, rowAlpha);
                }
            } else {
                row.render(ctx, gx, rowY, width, rowAlpha);
            }
            rowY += rowHeight;
        }
    }

    boolean contains(double mx, double my) {
        return drawn && FaContext.inside(mx, my, lastX, lastY, lastW, lastH);
    }

    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (!contains(mx, my)) return false;
        for (int i = 0; i < rows.size(); i++) {
            if (matched[i] && rows.get(i).mouseClicked(ctx, mx, my, button)) return true;
        }
        return true;
    }

    void mouseDragged(FaContext ctx, double mx, double my) {
        for (FaRow row : rows) row.mouseDragged(ctx, mx, my);
    }

    void mouseReleased() {
        for (FaRow row : rows) row.mouseReleased();
    }

    boolean mouseScrolled(FaContext ctx, double mx, double my, double amount) {
        if (!contains(mx, my)) return false;
        for (int i = 0; i < rows.size(); i++) {
            if (matched[i] && rows.get(i).mouseScrolled(ctx, mx, my, amount)) return true;
        }
        return false;
    }
}
