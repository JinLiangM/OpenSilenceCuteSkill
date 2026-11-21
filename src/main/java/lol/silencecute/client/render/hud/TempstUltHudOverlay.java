package lol.silencecute.client.render.hud;

import lol.silencecute.Client;
import lol.silencecute.client.TempstUltAnchorClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TempstUltHudOverlay {

    private static final int BAR_WIDTH = 96;
    private static final int BAR_HEIGHT = 3;

    private TempstUltHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!TempstUltAnchorClient.isActive()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();

        int x = screenW / 2 - BAR_WIDTH / 2;
        int y = screenH / 2 + 18;

        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0x66000000);
        int filled = (int) (BAR_WIDTH * TempstUltAnchorClient.progress());
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + BAR_HEIGHT, 0xFFFFFFFF);
        }

        double distance = TempstUltAnchorClient.distanceToAnchor(minecraft.player.position());
        String text = String.format("%.1f m", distance);
        graphics.drawCenteredString(minecraft.font, text, screenW / 2, y + BAR_HEIGHT + 4, 0xFFFFFFFF);
    }
}
