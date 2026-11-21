package lol.silencecute.client.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class UiFont {

    private static final float VANILLA_LINE_HEIGHT = 9.0F;

    private final String file;
    private final float size;

    public UiFont(String file, float size) {
        this.file = file;
        this.size = size;
    }

    public float size() {
        return size;
    }

    private TrueTypeFontRenderer renderer() {
        return ModFonts.ttf(file, size);
    }

    private TrueTypeFontRenderer renderer(GuiGraphics graphics) {
        return ModFonts.ttf(file, size, ModFonts.poseScale(graphics));
    }

    public void draw(GuiGraphics graphics, String text, float x, float y, int color) {
        draw(graphics, text, x, y, color, false);
    }

    public void draw(GuiGraphics graphics, String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        TrueTypeFontRenderer font = renderer(graphics);
        if (font != null) {
            font.drawString(graphics, text, x, y, color, shadow);
            return;
        }
        drawVanilla(graphics, text, x, y, color, shadow);
    }

    public void drawCentered(GuiGraphics graphics, String text, float centerX, float y, int color) {
        if (text == null || text.isEmpty()) return;
        draw(graphics, text, centerX - width(text) / 2.0F, y, color, false);
    }

    public void drawRight(GuiGraphics graphics, String text, float rightX, float y, int color) {
        if (text == null || text.isEmpty()) return;
        draw(graphics, text, rightX - width(text), y, color, false);
    }

    public float width(String text) {
        if (text == null || text.isEmpty()) return 0.0F;
        TrueTypeFontRenderer font = renderer();
        if (font != null) return font.getStringWidth(text);
        return Minecraft.getInstance().font.width(text) * (size / VANILLA_LINE_HEIGHT);
    }

    public float height() {
        TrueTypeFontRenderer font = renderer();
        return font != null ? font.getLineHeight() : size;
    }

    private void drawVanilla(GuiGraphics graphics, String text, float x, float y, int color, boolean shadow) {
        float scale = size / VANILLA_LINE_HEIGHT;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, shadow);
        graphics.pose().popPose();
    }
}
