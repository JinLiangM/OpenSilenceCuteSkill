package lol.silencecute.client.screen;

import lol.silencecute.client.font.ModFonts;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class FontedButton extends Button {

    private FontedButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public static FontedButton create(int x, int y, int width, int height,
                                      Component message, OnPress onPress) {
        return new FontedButton(x, y, width, height, message, onPress);
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        ModFonts.drawCenteredString(graphics, this.getMessage().getString(),
                this.getX() + this.getWidth() / 2.0F,
                this.getY() + (this.getHeight() - ModFonts.height()) / 2.0F,
                color, true);
    }
}
