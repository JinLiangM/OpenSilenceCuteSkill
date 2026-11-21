package lol.silencecute.client.screen;

import org.lwjgl.glfw.GLFW;

import lol.silencecute.client.render.hud.ChatHudOverlay;
import lol.silencecute.client.render.hud.HudWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SilenceChatScreen extends Screen {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final int BOX_H = 12;

    private static final int MIN_BOX_W = 140;

    private static final int BG_COLOR = 0x99606060;

    private final String initial;

    private int boxX;
    private int boxY;
    private int boxW;

    private EditBox input;

    public SilenceChatScreen(String initial) {
        super(Component.literal("Chat"));
        this.initial = initial == null ? "" : initial;
    }

    @Override
    protected void init() {
        HudWidget widget = ChatHudOverlay.widget();

        this.boxW = (int) Math.max((float) MIN_BOX_W, widget.width());
        this.boxX = (int) widget.x();
        this.boxY = (int) (widget.y() + widget.height()) + 2;

        if (this.boxX <= 0 && this.boxY <= 0) {
            this.boxX = this.width / 2 - this.boxW / 2;
            this.boxY = this.height - BOX_H - 4;
        }

        if (this.boxX < 2) this.boxX = 2;
        if (this.boxX + this.boxW > this.width - 2) this.boxX = this.width - this.boxW - 2;
        if (this.boxY + BOX_H + 2 > this.height) this.boxY = this.height - BOX_H - 2;

        this.input = new EditBox(this.font, this.boxX, this.boxY, this.boxW, BOX_H, Component.literal(""));
        this.input.setMaxLength(256);
        this.input.setValue(this.initial);
        this.input.setCanLoseFocus(false);
        this.addRenderableWidget(this.input);

        this.setFocused(this.input);
        this.setInitialFocus(this.input);
        this.input.setFocused(true);
        this.input.moveCursorToEnd();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        graphics.fill(this.boxX - 1, this.boxY - 1,
                this.boxX + this.boxW + 1, this.boxY + BOX_H + 1, BG_COLOR);

        this.input.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            String text = this.input.getValue();
            this.onClose();
            send(text);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void send(String text) {
        try {
            if (text == null || text.isBlank()) return;
            if (MC.player == null || MC.player.connection == null) return;
            if (text.startsWith("/")) {
                MC.player.connection.sendCommand(text.substring(1));
            } else {
                MC.player.connection.sendChat(text);
            }
        } catch (Throwable ignored) {

        }
    }

    @Override
    public void onClose() {
        MC.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
