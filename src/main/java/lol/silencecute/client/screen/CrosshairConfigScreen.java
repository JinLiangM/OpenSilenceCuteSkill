package lol.silencecute.client.screen;

import lol.silencecute.client.font.ModFonts;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CrosshairConfigScreen extends Screen {
    private final Screen parent;
    private AlphaSlider alphaSlider;

    public CrosshairConfigScreen(Screen parent) {
        super(Component.literal("露娜技能显示设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;

        this.addRenderableWidget(FontedButton.create(centerX - 120, centerY - 60, 240, 20, getTrajectoryText(), b -> {
            ModConfigData.SHOW_TRAJECTORY.set(!ModConfigData.SHOW_TRAJECTORY.get());
            ModConfigData.SPEC.save();
            b.setMessage(getTrajectoryText());
        }));

        this.addRenderableWidget(FontedButton.create(centerX - 120, centerY - 35, 240, 20, getHexagonText(), b -> {
            ModConfigData.SHOW_HEXAGON.set(!ModConfigData.SHOW_HEXAGON.get());
            ModConfigData.SPEC.save();
            b.setMessage(getHexagonText());
        }));

        this.addRenderableWidget(FontedButton.create(centerX - 120, centerY - 10, 240, 20, getLunaCrosshairText(), b -> {
            ModConfigData.LUNA_CROSSHAIR.set(!ModConfigData.LUNA_CROSSHAIR.get());
            ModConfigData.SPEC.save();
            b.setMessage(getLunaCrosshairText());
        }));

        this.addRenderableWidget(FontedButton.create(centerX - 120, centerY + 15, 240, 20, getShockRadiusText(), b -> {
            ModConfigData.SHOW_SHOCK_RADIUS.set(!ModConfigData.SHOW_SHOCK_RADIUS.get());
            ModConfigData.SPEC.save();
            b.setMessage(getShockRadiusText());
        }));

        this.addRenderableWidget(FontedButton.create(centerX - 120, centerY + 40, 240, 20, getChamsTeamColorText(), b -> {
            ModConfigData.CHAMS_TEAM_COLOR.set(!ModConfigData.CHAMS_TEAM_COLOR.get());
            ModConfigData.SPEC.save();
            b.setMessage(getChamsTeamColorText());
        }));

        this.alphaSlider = new AlphaSlider(centerX - 120, centerY + 65, 240, 20);
        this.addRenderableWidget(this.alphaSlider);

        this.addRenderableWidget(FontedButton.create(centerX - 120, centerY + 95, 240, 20,
                Component.literal("完成"), b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        }));
    }

    private Component getTrajectoryText() { return Component.literal("显示抛物线: " + (ModConfigData.SHOW_TRAJECTORY.get() ? "§a开启" : "§c关闭")); }
    private Component getHexagonText() { return Component.literal("显示终点六边形: " + (ModConfigData.SHOW_HEXAGON.get() ? "§a开启" : "§c关闭")); }
    private Component getLunaCrosshairText() { return Component.literal("电箭定位准心: " + (ModConfigData.LUNA_CROSSHAIR.get() ? "§a开启" : "§c关闭")); }
    private Component getShockRadiusText() { return Component.literal("电箭范围显示: " + (ModConfigData.SHOW_SHOCK_RADIUS.get() ? "§a开启" : "§c关闭")); }
    private Component getChamsTeamColorText() { return Component.literal("显形跟随队伍颜色: " + (ModConfigData.CHAMS_TEAM_COLOR.get() ? "§a开启" : "§c关闭")); }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        ModFonts.drawCenteredString(guiGraphics, this.title.getString(),
                this.width / 2.0F, this.height / 2.0F - 85, 0xFFFFFFFF, true);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.alphaSlider != null) {
            ModFonts.drawCenteredString(guiGraphics, this.alphaSlider.label(),
                    this.alphaSlider.getX() + this.alphaSlider.getWidth() / 2.0F,
                    this.alphaSlider.getY() + (this.alphaSlider.getHeight() - ModFonts.height()) / 2.0F,
                    0xFFFFFFFF, true);
        }
    }

    private static class AlphaSlider extends AbstractSliderButton {
        public AlphaSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), ModConfigData.CHAMS_ALPHA.get() / 255.0D);
            this.updateMessage();
        }

        String label() {
            return "显形透明度 (Alpha): §e" + (int) (this.value * 255);
        }

        @Override
        protected void updateMessage() {

            this.setMessage(Component.empty());
        }

        @Override
        protected void applyValue() {
            ModConfigData.CHAMS_ALPHA.set((int) (this.value * 255));
            ModConfigData.SPEC.save();
        }
    }
}
