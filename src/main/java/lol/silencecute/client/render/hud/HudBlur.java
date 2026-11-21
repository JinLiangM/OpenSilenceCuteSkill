package lol.silencecute.client.render.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import lol.silencecute.Client;
import lol.silencecute.client.render.naven.NvPostFx;
import lol.silencecute.config.ModConfigData;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HudBlur {

    private HudBlur() {}

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        NvPostFx.beginFrame();
    }

    public static boolean enabled() {
        try {
            if (Minecraft.getInstance().screen != null) return false;
            return ModConfigData.rendererEnabled()
                    && ModConfigData.navenBlur()
                    && NvPostFx.available();
        } catch (Exception e) {
            return false;
        }
    }

    public static void apply(GuiGraphics graphics, float x, float y, float w, float h, float radius) {
        if (graphics == null || w <= 1.0F || h <= 1.0F) return;
        if (!enabled()) return;
        try {
            graphics.flush();
            NvPostFx.blur(graphics.pose(), x, y, w, h, ModConfigData.navenBlurStrength(), radius);
        } catch (Exception ignored) {

        }
    }
}
