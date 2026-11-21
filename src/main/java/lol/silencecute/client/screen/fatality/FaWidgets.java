package lol.silencecute.client.screen.fatality;

import com.mojang.blaze3d.vertex.PoseStack;
import lol.silencecute.client.render.naven.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

public final class FaWidgets {

    private static float hoverAmount;
    private static float popAmount;
    private static boolean wasPressed;
    private static long lastNanos;

    private FaWidgets() {
    }

    public static void button(GuiGraphics graphics, float x, float y, float w, float h,
                              String label, boolean hovered) {
        button(graphics, x, y, w, h, label, hovered, false, 1.0f, 1.0f);
    }

    public static void button(GuiGraphics graphics, float x, float y, float w, float h, String label,
                              boolean hovered, boolean pressed, float scale, float alpha) {
        if (graphics == null || w <= 0.0f || h <= 0.0f || alpha <= 0.01f || scale <= 0.01f) return;

        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 1.0f / 60.0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        float ease = 1.0f - (float) Math.exp(-dt * 14.0f);
        hoverAmount += ((hovered ? 1.0f : 0.0f) - hoverAmount) * ease;

        if (pressed && !wasPressed) popAmount = 1.0f;
        wasPressed = pressed;
        popAmount *= (float) Math.exp(-dt * 12.0f);
        if (popAmount < 0.01f) popAmount = 0.0f;

        FaTheme.refresh();

        graphics.flush();

        PoseStack pose = graphics.pose();
        float centerX = x + w * 0.5f;
        float centerY = y + h * 0.5f;
        float total = scale * (1.0f - 0.05f * hoverAmount - 0.12f * popAmount);
        pose.pushPose();
        pose.translate(centerX, centerY, 0.0f);
        pose.scale(total, total, 1.0f);
        pose.translate(-centerX, -centerY, 0.0f);

        float radius = 3.0f;
        RenderUtils.drawRoundedRect(pose, x, y, w, h, radius, FaTheme.alpha(FaTheme.FIELD, alpha));

        FaFont font = FaTheme.small();

        String shown = label.length() <= 4 ? label : FaTheme.fit(label, font, w - 6.0f);
        float textX = x + (w - font.width(shown)) * 0.5f;

        float textY = y + (h - font.height()) * 0.5f + 0.5f;
        int color = FaTheme.alpha(FaTheme.LABEL, alpha);
        if (font instanceof FaFont.Game game) {
            game.font().draw(graphics, shown, textX, textY, color);
        } else if (font instanceof FaFont.Vanilla vanilla) {
            vanilla.draw(graphics, shown, textX, textY, color);
        }
        pose.popPose();
    }
}
