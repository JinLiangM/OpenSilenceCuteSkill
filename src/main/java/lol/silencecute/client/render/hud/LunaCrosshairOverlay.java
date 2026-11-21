package lol.silencecute.client.render.hud;

import lol.silencecute.Client;
import lol.silencecute.client.LunaShockArrowClient;
import lol.silencecute.config.ModConfigData;

import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.client.event.RenderGuiOverlayEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)

public final class LunaCrosshairOverlay {

    

    private static final ResourceLocation VANILLA_CROSSHAIR =

             new ResourceLocation("minecraft", "crosshair");

    

    private static final int[] LINE_LENGTHS = {22, 13, 7};

    

    private static final int ROW_SPACING = 6;

    

    private static final int CENTER_GAP = 4;

    

    private static final int DOT_SIZE = 2;

    

    private static final int COLOR = 0xE6FFFFFF;

    private LunaCrosshairOverlay() {

    }

    @SubscribeEvent

    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {

        if (!VANILLA_CROSSHAIR.equals(event.getOverlay().id())) {

            return;

        }

        if (!shouldReplace()) {

            return;

        }

        event.setCanceled(true);

        drawFocusCrosshair(event.getGuiGraphics(),

                event.getWindow().getGuiScaledWidth() / 2,

                event.getWindow().getGuiScaledHeight() / 2);

    }

    private static boolean shouldReplace() {

        Minecraft minecraft = Minecraft.getInstance();

        if (!ModConfigData.lunaCrosshair()) {

            return false;

        }

        if (minecraft.player == null || minecraft.options.hideGui) {

            return false;

        }

        if (!minecraft.options.getCameraType().isFirstPerson()) {

            return false;

        }

        return LunaShockArrowClient.isDrawn() || LunaShockArrowClient.isReleasing();

    }

    

    private static void drawFocusCrosshair(GuiGraphics graphics, int centerX, int centerY) {

        for (int row = 0; row < LINE_LENGTHS.length; row++) {

            int length = LINE_LENGTHS[row];

            int y = centerY + (row - 1) * ROW_SPACING;

            int dotLeft = centerX - DOT_SIZE / 2;

            int dotTop = y - DOT_SIZE / 2;

            graphics.fill(dotLeft, dotTop, dotLeft + DOT_SIZE, dotTop + DOT_SIZE, COLOR);

            graphics.fill(centerX - CENTER_GAP - length, y, centerX - CENTER_GAP, y + 1, COLOR);

            graphics.fill(centerX + CENTER_GAP, y, centerX + CENTER_GAP + length, y + 1, COLOR);

        }

    }

}

