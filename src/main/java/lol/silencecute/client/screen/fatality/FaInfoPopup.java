package lol.silencecute.client.screen.fatality;

import lol.silencecute.client.lang.Lang;

import lol.silencecute.config.ModConfigData;
import net.minecraft.client.Minecraft;

final class FaInfoPopup extends FaPopup {
    private static final float PAD = 10.0f;
    private static final float LINE = 15.0f;
    private static final float ACTION_H = 20.0f;
    private final FaClickGui gui;
    private final FaMotion[] hovers = {new FaMotion(0.0f)};

    FaInfoPopup(Object owner, FaClickGui gui) {
        super(owner);
        this.gui = gui;
        w = 190.0f;
        h = PAD + 20.0f + LINE * 3 + 6.0f + ACTION_H + PAD;
    }

    private float actionY(int index) {
        return y + PAD + 20.0f + LINE * 3 + 6.0f + index * ACTION_H;
    }

    @Override
    protected void draw(FaContext ctx, float alpha) {
        String title = FaClickGui.CLIENT_NAME.toUpperCase();
        FaFont logo = FaTheme.tab();
        ctx.draw.textMid(title, x + PAD, y + PAD + 7.0f, logo, FaTheme.alpha(FaTheme.LOGO, alpha));
        ctx.draw.textMid("v" + FaClickGui.CLIENT_VERSION, x + PAD + logo.width(title) + 6.0f, y + PAD + 7.5f,
                FaTheme.field(), FaTheme.alpha(FaTheme.ACCENT, alpha));
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long max = runtime.maxMemory() / (1024L * 1024L);
        String[][] lines = {
                {"FPS", String.valueOf(Minecraft.getInstance().getFps())},
                {Lang.tr("info.memory"), used + " / " + max + " MB"},
                {Lang.tr("info.operator"), ModConfigData.operator().name()},
        };
        float lineY = y + PAD + 20.0f;
        FaFont font = FaTheme.field();
        for (String[] line : lines) {
            ctx.draw.textMid(line[0], x + PAD, lineY + LINE * 0.5f, font, FaTheme.alpha(FaTheme.MUTED, alpha));
            ctx.draw.textRightMid(line[1], x + w - PAD, lineY + LINE * 0.5f, font,
                    FaTheme.alpha(FaTheme.LABEL, alpha));
            lineY += LINE;
        }
        action(ctx, 0, Lang.tr("gui.layout"), Lang.tr("gui.reset"), alpha);
    }

    private void action(FaContext ctx, int index, String label, String value, float alpha) {
        float ay = actionY(index), mid = ay + ACTION_H * 0.5f;
        boolean hovered = ctx.mouseX >= x && ctx.mouseX <= x + w && ctx.mouseY >= ay && ctx.mouseY < ay + ACTION_H;
        float hover = hovers[index].toward(hovered ? 1.0f : 0.0f, ctx.dt, 18.0f);
        if (hover > 0.01f) {
            ctx.draw.rect(x + 3.0f, ay, w - 6.0f, ACTION_H, 3.0f, FaTheme.alpha(FaTheme.FIELD_HOVER, alpha * hover));
        }
        FaFont font = FaTheme.field();
        ctx.draw.textMid(label, x + PAD, mid, font,
                FaTheme.alpha(FaTheme.mix(FaTheme.LABEL, FaTheme.LABEL_HOVER, hover), alpha));
        ctx.draw.textRightMid(value, x + w - PAD, mid, font,
                FaTheme.alpha(FaTheme.mix(FaTheme.FIELD_TEXT_HOVER, FaTheme.ACCENT, hover), alpha));
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (button != 0) return true;
        if (FaContext.inside(mx, my, x, actionY(0), w, ACTION_H)) {
            gui.resetLayout();
            toast();
        }
        return true;
    }

    private void toast() {
        gui.toast(Lang.tr("gui.layout.reset"));
    }
}
