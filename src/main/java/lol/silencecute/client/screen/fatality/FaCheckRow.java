package lol.silencecute.client.screen.fatality;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import lol.silencecute.client.screen.Value;

class FaCheckRow extends FaRow {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final FaMotion check;

    FaCheckRow(Value<?> value, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(value, label);
        this.getter = getter;
        this.setter = setter;
        this.check = new FaMotion(getter.getAsBoolean() ? 1.0f : 0.0f);
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float t = check.toward(getter.getAsBoolean() ? 1.0f : 0.0f, ctx.dt, 22.0f);
        float size = FaTheme.CHECK;
        float boxX = right() - size;
        float used = accessory(ctx, boxX - 6.0f, centerY, alpha);
        drawLabel(ctx, x, centerY, boxX - used - x - FaTheme.ROW_PAD - 6.0f, alpha);
        int box = FaTheme.mix(FaTheme.FIELD, FaTheme.FIELD_HOVER, hover.value());
        ctx.draw.rect(boxX, centerY - size * 0.5f, size, size, 2.0f, FaTheme.alpha(box, alpha));
        if (t > 0.01f) {

            try (var scope = ctx.draw.push()) {
                ctx.draw.translate(boxX + size * 0.5f, centerY);
                ctx.draw.scale(0.7f + 0.3f * t);
                ctx.draw.icon(FaTheme.I_CHECK, 0.0f, 0.0f, 10.0f, FaTheme.alpha(FaTheme.TICK, alpha * t));
            }
        }
    }

    protected float accessory(FaContext ctx, float right, float centerY, float alpha) {
        return 0.0f;
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (button != 0 || !hit(mx, my)) return false;
        setter.accept(!getter.getAsBoolean());
        return true;
    }
}
