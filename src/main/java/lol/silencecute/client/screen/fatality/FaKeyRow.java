package lol.silencecute.client.screen.fatality;

import lol.silencecute.client.lang.Lang;

import lol.silencecute.client.screen.KeyValue;
import org.lwjgl.glfw.GLFW;

final class FaKeyRow extends FaRow implements FaKeySink {
    private final KeyValue key;
    private boolean binding;
    private float lastFieldX, lastFieldW;

    FaKeyRow(KeyValue key) {
        super(key, key.displayName());
        this.key = key;
    }

    static String keyName(int key) {
        if (key <= 0) return Lang.tr("common.none");
        if (KeyValue.isMouse(key)) return "M" + (KeyValue.mouseButton(key) + 1);
        return KeyValue.displayName(key);
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        FaFont font = FaTheme.field();
        String text = binding ? "..." : keyName(key.get());
        float fw = Math.max(44.0f, font.width(text) + 18.0f);
        float fh = FaTheme.FIELD_H;
        float fx = fieldX(fw), fy = fieldY();
        drawLabel(ctx, x, centerY, fx - x - FaTheme.ROW_PAD - 6.0f, alpha);
        boolean hot = binding || ctx.hovered(fx, fy, fw, fh);
        ctx.draw.rect(fx, fy, fw, fh, FaTheme.FIELD_RADIUS,
                FaTheme.alpha(hot ? FaTheme.FIELD_HOVER : FaTheme.FIELD, alpha));
        if (binding) {
            ctx.draw.outline(fx, fy, fw, fh, FaTheme.FIELD_RADIUS, 0.5f, FaTheme.alpha(FaTheme.ACCENT_DIM, alpha));
        }
        int color = binding ? FaTheme.ACCENT : hot ? FaTheme.FIELD_TEXT_HOVER : FaTheme.FIELD_TEXT;
        ctx.draw.textCenter(text, fx + fw * 0.5f, centerY, font, FaTheme.alpha(color, alpha));
        lastFieldX = fx;
        lastFieldW = fw;
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {

        if (binding) {
            key.set(button == 1 ? 0 : KeyValue.mouseKey(button));
            binding = false;
            ctx.gui.focus(null);
            return true;
        }
        if (!shown || !FaContext.inside(mx, my, lastFieldX, fieldY(), lastFieldW, FaTheme.FIELD_H)) return false;
        if (button == 1) {
            key.set(0);
            ctx.gui.focus(null);
            return true;
        }
        if (button != 0) return false;
        binding = true;
        ctx.gui.focus(this);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int modifiers) {
        if (!binding) return false;
        boolean clear = keyCode == GLFW.GLFW_KEY_ESCAPE
                || keyCode == GLFW.GLFW_KEY_DELETE
                || keyCode == GLFW.GLFW_KEY_BACKSPACE;
        key.set(clear ? 0 : keyCode);
        binding = false;
        return true;
    }

    @Override
    public boolean mousePressed(double mouseX, double mouseY, int button) {

        if (!binding) return false;

        if (!FaContext.inside(mouseX, mouseY, lastFieldX, fieldY(), lastFieldW, FaTheme.FIELD_H)) {
            return false;
        }
        key.set(button == 1 ? 0 : KeyValue.mouseKey(button));
        binding = false;
        return true;
    }

    @Override
    public void focusLost() {
        binding = false;
    }
}
