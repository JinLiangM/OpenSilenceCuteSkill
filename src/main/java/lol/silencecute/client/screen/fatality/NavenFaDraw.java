package lol.silencecute.client.screen.fatality;

import java.util.ArrayDeque;
import java.util.Deque;

import lol.silencecute.client.render.naven.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

final class NavenFaDraw extends FaDraw {
    private GuiGraphics graphics;
    private float alpha = 1.0f;
    private int[] clip;
    private int clipDepth;

    private boolean lowQuality;

    private boolean batchPending;
    private final Deque<Float> savedAlpha = new ArrayDeque<>();

    @Override
    void begin(GuiGraphics graphics) {
        this.graphics = graphics;
        this.alpha = 1.0f;
        this.clip = null;
        this.clipDepth = 0;
        this.batchPending = false;
        this.savedAlpha.clear();
    }

    @Override
    int openClips() {
        return clipDepth;
    }

    @Override
    void setLowQuality(boolean low) {
        this.lowQuality = low;
    }

    private void flushPending() {
        if (graphics != null && batchPending) {
            graphics.flush();
        }
        batchPending = false;
    }

    @Override
    void rect(float x, float y, float w, float h, float radius, int argb) {
        if (graphics == null || w <= 0.0f || h <= 0.0f) return;
        int color = mul(argb);
        if ((color >>> 24) == 0) return;
        flushPending();
        RenderUtils.drawRoundedRect(graphics.pose(), x, y, w, h, radius, color);
    }

    @Override
    void outline(float x, float y, float w, float h, float radius, float width, int argb) {
        if (graphics == null || w <= 0.0f || h <= 0.0f || width <= 0.0f) return;
        int color = mul(argb);
        if ((color >>> 24) == 0) return;
        flushPending();
        RenderUtils.drawRoundedOutline(graphics.pose(), x, y, w, h, radius, width, color);
    }

    @Override
    void quad(float x, float y, float w, float h, int topLeft, int topRight, int bottomRight, int bottomLeft) {
        if (graphics == null || w <= 0.0f || h <= 0.0f) return;
        int tl = mul(topLeft);
        int tr = mul(topRight);
        int br = mul(bottomRight);
        int bl = mul(bottomLeft);
        if (((tl | tr | br | bl) >>> 24) == 0) return;
        flushPending();
        RenderUtils.fillGradient(graphics.pose(), x, y, w, h, tl, tr, br, bl);
    }

    @Override
    void shadow(float x, float y, float w, float h, float radius, float sigma, int argb) {
        if (graphics == null || w <= 0.0f || h <= 0.0f) return;
        flushPending();
        float spread = Math.max(2.0f, sigma * 0.6f);
        int layers = lowQuality ? 2 : 4;
        float layerAlpha = lowQuality ? 0.14f : 0.10f;
        for (int i = layers - 1; i >= 0; i--) {
            float k = spread * (i + 1) / layers;
            int color = mul(FaTheme.alpha(argb, layerAlpha));
            if ((color >>> 24) == 0) continue;
            RenderUtils.drawRoundedRect(graphics.pose(), x - k, y - k * 0.6f + 2.0f,
                    w + k * 2.0f, h + k * 1.2f, radius + k, color);
        }
    }

    @Override
    void text(String text, float x, float y, FaFont font, int argb) {
        if (graphics == null || text == null || text.isEmpty()) return;
        int color = mul(argb);
        if ((color >>> 24) == 0) return;
        if (font instanceof FaFont.Game game) {
            game.font().draw(graphics, text, x, y, color);
        } else if (font instanceof FaFont.Naven naven) {
            flushPending();

            PoseStack pose = graphics.pose();
            pose.pushPose();
            naven.renderer().render(pose, text, x, y, new java.awt.Color(color, true), false, naven.size() / 32.0);
            pose.popPose();
        } else if (font instanceof FaFont.Vanilla vanilla) {
            vanilla.draw(graphics, text, x, y, color);
        } else {
            return;
        }
        batchPending = true;
    }

    @Override
    void skinFace(AbstractClientPlayer player, float x, float y, float size) {
        if (graphics == null || player == null || size <= 0.0f) return;
        ResourceLocation skin = player.getSkinTextureLocation();
        if (skin == null) return;
        flushPending();
        RenderUtils.drawPlayerHead(graphics.pose(), x, y, size, size, player, alpha);
    }

    @Override
    void item(ItemStack stack, float x, float y, float size) {
        if (graphics == null || stack == null || stack.isEmpty() || size <= 0.0f) return;
        flushPending();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0f);
        float k = size / 16.0f;
        pose.scale(k, k, 1.0f);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
        batchPending = true;
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
        applyScissor(next);
        return () -> {
            clip = previous;
            clipDepth--;

            if (graphics == null) return;
            flushPending();
            try {
                graphics.disableScissor();
            } catch (IllegalStateException ignored) {

            }
        };
    }

    @Override
    void closeAllClips() {
        if (graphics == null) {
            clipDepth = 0;
            clip = null;
            return;
        }
        while (clipDepth > 0) {
            graphics.flush();
            batchPending = false;
            try {
                graphics.disableScissor();
            } catch (IllegalStateException ignored) {

            }
            clipDepth--;
        }
        clip = null;
    }

    private void applyScissor(int[] rect) {
        if (graphics == null) return;

        flushPending();
        if (rect == null) {
            graphics.disableScissor();
            return;
        }
        int x1 = Math.max(0, rect[0]);
        int y1 = Math.max(0, rect[1]);
        int x2 = Math.max(x1, rect[2]);
        int y2 = Math.max(y1, rect[3]);
        graphics.enableScissor(x1, y1, x2, y2);
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
