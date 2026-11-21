package lol.silencecute.client.screen.fatality;

import java.util.ArrayDeque;
import java.util.Deque;

import lol.silencecute.client.render.ui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

final class McFaDraw extends FaDraw {
    private GuiGraphics graphics;
    private float alpha = 1.0f;

    private int[] clip;
    private int clipDepth;
    private final Deque<Float> savedAlpha = new ArrayDeque<>();

    @Override
    void begin(GuiGraphics graphics) {
        this.graphics = graphics;
        this.alpha = 1.0f;
        this.clip = null;
        this.clipDepth = 0;
        this.savedAlpha.clear();
    }

    @Override
    int openClips() {
        return clipDepth;
    }

    @Override
    void rect(float x, float y, float w, float h, float radius, int argb) {
        if (graphics == null || w <= 0.0f || h <= 0.0f) return;
        int color = mul(argb);
        if ((color >>> 24) == 0) return;
        float r = Math.max(0.0f, Math.min(radius, Math.min(w, h) * 0.5f));
        UiDraw.roundRect(graphics, x, y, w, h, r, color);
    }

    @Override
    void outline(float x, float y, float w, float h, float radius, float width, int argb) {
        if (graphics == null || w <= 0.0f || h <= 0.0f || width <= 0.0f) return;
        int color = mul(argb);
        if ((color >>> 24) == 0) return;
        float r = Math.max(0.0f, Math.min(radius, Math.min(w, h) * 0.5f));
        UiDraw.roundRectOutline(graphics, x, y, w, h, r, width, color);
    }

    @Override
    void quad(float x, float y, float w, float h, int topLeft, int topRight, int bottomRight, int bottomLeft) {
        if (graphics == null || w <= 0.0f || h <= 0.0f) return;
        if (topLeft == topRight && bottomLeft == bottomRight) {
            UiDraw.gradientVertical(graphics, x, y, w, h, mul(topLeft), mul(bottomLeft));
            return;
        }
        if (topLeft == bottomLeft && topRight == bottomRight) {
            UiDraw.gradientHorizontal(graphics, x, y, w, h, mul(topLeft), mul(topRight));
            return;
        }
        UiDraw.gradientVertical(graphics, x, y, w, h, mul(topLeft), mul(bottomLeft));
    }

    @Override
    void shadow(float x, float y, float w, float h, float radius, float sigma, int argb) {
        if (graphics == null || w <= 0.0f || h <= 0.0f) return;
        float spread = Math.max(2.0f, sigma * 0.6f);
        int layers = 6;
        for (int i = layers - 1; i >= 0; i--) {
            float k = spread * (i + 1) / layers;
            int color = mul(FaTheme.alpha(argb, 0.10f));
            if ((color >>> 24) == 0) continue;
            UiDraw.roundRect(graphics, x - k, y - k * 0.6f + 2.0f, w + k * 2.0f, h + k * 1.2f, radius + k, color);
        }
    }

    @Override
    void text(String text, float x, float y, FaFont font, int argb) {
        if (graphics == null || text == null || text.isEmpty()) return;
        int color = mul(argb);
        if ((color >>> 24) == 0) return;
        if (font instanceof FaFont.Game game) {
            game.font().draw(graphics, text, x, y, color);
        } else if (font instanceof FaFont.Vanilla vanilla) {
            vanilla.draw(graphics, text, x, y, color);
        }
    }

    @Override
    void skinFace(AbstractClientPlayer player, float x, float y, float size) {
        if (graphics == null || player == null || size <= 0.0f) return;
        ResourceLocation skin = player.getSkinTextureLocation();
        if (skin == null) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        float k = size / 8.0f;
        graphics.pose().scale(k, k, 1.0f);

        graphics.blit(skin, 0, 0, 8, 8, 8.0f, 8.0f, 8, 8, 64, 64);
        graphics.pose().popPose();
    }

    @Override
    void item(ItemStack stack, float x, float y, float size) {
        if (graphics == null || stack == null || stack.isEmpty() || size <= 0.0f) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        float k = size / 16.0f;
        graphics.pose().scale(k, k, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    @Override
    Scope push() {
        GuiGraphics target = graphics;
        if (target != null) target.pose().pushPose();
        savedAlpha.push(alpha);
        return () -> {
            if (target != null) target.pose().popPose();
            if (!savedAlpha.isEmpty()) alpha = savedAlpha.pop();
        };
    }

    @Override
    void translate(float x, float y) {
        if (graphics != null) graphics.pose().translate(x, y, 0.0f);
    }

    @Override
    void scale(float scale) {
        if (graphics != null) graphics.pose().scale(scale, scale, 1.0f);
    }

    @Override
    void opacity(float value) {
        alpha *= Math.max(0.0f, Math.min(1.0f, value));
    }

    @Override
    Scope clip(float x, float y, float w, float h) {
        final int[] previous = clip;
        int[] next = screenRect(x, y, Math.max(0.0f, w), Math.max(0.0f, h));
        if (previous != null) next = intersect(previous, next);
        clip = next;
        clipDepth++;
        pushScissor(next);
        return () -> {
            clip = previous;
            clipDepth--;
            restoreScissor(previous);
        };
    }

    void closeAllClips() {
        if (graphics == null) {
            clipDepth = 0;
            clip = null;
            return;
        }        while (clipDepth > 0) {
            graphics.flush();
            graphics.disableScissor();
            clipDepth--;
        }
        clip = null;
    }

    private void pushScissor(int[] rect) {
        if (graphics == null || rect == null) return;
        graphics.flush();
        int x1 = Math.max(0, rect[0]);
        int y1 = Math.max(0, rect[1]);
        int x2 = Math.max(x1, rect[2]);
        int y2 = Math.max(y1, rect[3]);
        graphics.enableScissor(x1, y1, x2, y2);
    }

    private void restoreScissor(int[] previous) {
        if (graphics == null) return;
        graphics.flush();
        graphics.disableScissor();
        if (previous != null) pushScissor(previous);
    }

    private int[] screenRect(float x, float y, float w, float h) {
        Matrix4f matrix = graphics.pose().last().pose();
        float ax = matrix.m00() * x + matrix.m01() * y + matrix.m03();
        float ay = matrix.m10() * x + matrix.m11() * y + matrix.m13();
        float bx = matrix.m00() * (x + w) + matrix.m01() * (y + h) + matrix.m03();
        float by = matrix.m10() * (x + w) + matrix.m11() * (y + h) + matrix.m13();
        return new int[]{
                (int) Math.floor(Math.min(ax, bx)), (int) Math.floor(Math.min(ay, by)),
                (int) Math.ceil(Math.max(ax, bx)), (int) Math.ceil(Math.max(ay, by))
        };
    }

    private static int[] intersect(int[] a, int[] b) {
        int x1 = Math.max(a[0], b[0]);
        int y1 = Math.max(a[1], b[1]);
        int x2 = Math.min(a[2], b[2]);
        int y2 = Math.min(a[3], b[3]);
        return new int[]{x1, y1, Math.max(x1, x2), Math.max(y1, y2)};
    }

    private int mul(int argb) {
        return alpha >= 0.999f ? argb : FaTheme.alpha(argb, alpha);
    }
}
