package lol.silencecute.client.screen.fatality;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;

abstract class FaDraw {

    interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    abstract void begin(GuiGraphics graphics);

    abstract int openClips();

    abstract void closeAllClips();

    void setLowQuality(boolean low) {
    }

    abstract void rect(float x, float y, float w, float h, float radius, int argb);

    abstract void outline(float x, float y, float w, float h, float radius, float width, int argb);

    abstract void quad(float x, float y, float w, float h, int topLeft, int topRight, int bottomRight, int bottomLeft);

    abstract void shadow(float x, float y, float w, float h, float radius, float sigma, int argb);

    abstract void text(String text, float x, float y, FaFont font, int argb);

    abstract void skinFace(AbstractClientPlayer player, float x, float y, float size);

    abstract void item(ItemStack stack, float x, float y, float size);

    abstract Scope push();

    abstract void translate(float x, float y);

    abstract void scale(float scale);

    abstract void opacity(float alpha);

    abstract Scope clip(float x, float y, float w, float h);

    final void rect(float x, float y, float w, float h, int argb) {
        rect(x, y, w, h, 0.0f, argb);
    }

    final void vertical(float x, float y, float w, float h, int top, int bottom) {
        quad(x, y, w, h, top, top, bottom, bottom);
    }

    final void horizontal(float x, float y, float w, float h, int left, int right) {
        quad(x, y, w, h, left, right, right, left);
    }

    final void textMid(String text, float x, float centerY, FaFont font, int argb) {
        text(text, x, centerY - font.height() * 0.5f, font, argb);
    }

    final void textRightMid(String text, float right, float centerY, FaFont font, int argb) {
        textMid(text, right - font.width(text), centerY, font, argb);
    }

    final void textCenter(String text, float centerX, float centerY, FaFont font, int argb) {
        textMid(text, centerX - font.width(text) * 0.5f, centerY, font, argb);
    }

    final void icon(String glyph, float centerX, float centerY, float size, int argb) {
        FaFont font = FaTheme.icon(size);
        text(glyph, centerX - font.width(glyph) * 0.5f, centerY - font.height() * 0.5f, font, argb);
    }
}
