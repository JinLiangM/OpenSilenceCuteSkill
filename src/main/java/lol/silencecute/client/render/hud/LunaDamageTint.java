package lol.silencecute.client.render.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import lol.silencecute.Client;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class LunaDamageTint {

    private static final long TOTAL_TIME = 250L;

    private static final long PEAK_TIME = 80L;

    private static long startTime = 0L;

    private LunaDamageTint() {}

    
    public static void trigger() {
        startTime = System.currentTimeMillis();
    }

    
    private static float getTintStrength() {
        if (startTime == 0L) return 0.0F;
        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed >= TOTAL_TIME) {
            startTime = 0L;
            return 0.0F;
        }

        if (elapsed < PEAK_TIME) {

            return elapsed / (float) PEAK_TIME;
        } else {

            return 1.0F - ((elapsed - PEAK_TIME) / (float) (TOTAL_TIME - PEAK_TIME));
        }
    }

    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        float tint = getTintStrength();
        if (tint > 0.01F) {

            float greenBlue = 1.0F - (tint * 0.85F);
            RenderSystem.setShaderColor(1.0F, greenBlue, greenBlue, 1.0F);
        }
    }

    
    @SubscribeEvent
    public static void onRenderTickStart(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
