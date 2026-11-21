package lol.silencecute.client.screen;

import lol.silencecute.client.render.hud.DragManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudEditScreen extends Screen {

    public HudEditScreen() {
        super(Component.literal("HUD"));
    }

    @Override
    protected void init() {

        addRenderableWidget(Button.builder(Component.literal("重置"), button ->
                        DragManager.resetAll(width, height))
                .bounds(6, 6, 56, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
