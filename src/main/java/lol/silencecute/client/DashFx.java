package lol.silencecute.client;

import lol.silencecute.Client;
import lol.silencecute.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class DashFx {

    private static final float BASE_FOV_BOOST = 1.0F;

    private static final float DASH_FOV_BOOST = 1.25F;

    private static final float RISE_SECONDS = 0.16F;

    private static final float FALL_SECONDS = 0.42F;

    private static final float MAX_STEP_SECONDS = 0.1F;

    private static float currentDashFovBoost = BASE_FOV_BOOST;

    private static float rampStartBoost = BASE_FOV_BOOST;
    private static float rampTargetBoost = BASE_FOV_BOOST;
    private static float rampElapsedSeconds;
    private static float rampDurationSeconds;

    private static long lastUpdateNanos;

    private DashFx() {
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        long now = System.nanoTime();
        float deltaSeconds = lastUpdateNanos == 0L ? 0.0F : (now - lastUpdateNanos) / 1.0E9F;
        lastUpdateNanos = now;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }
        if (deltaSeconds <= 0.0F) return;

        update(Math.min(deltaSeconds, MAX_STEP_SECONDS));
    }

    private static void update(float deltaSeconds) {
        boolean rushing = ClientEvents.isDashing() || TempstRollClient.isRolling();
        float target = rushing ? DASH_FOV_BOOST : BASE_FOV_BOOST;

        if (target != rampTargetBoost) {
            rampTargetBoost = target;
            rampStartBoost = currentDashFovBoost;
            rampElapsedSeconds = 0.0F;
            rampDurationSeconds = target > rampStartBoost ? RISE_SECONDS : FALL_SECONDS;
        }

        if (rampElapsedSeconds >= rampDurationSeconds) {
            currentDashFovBoost = rampTargetBoost;
            return;
        }

        rampElapsedSeconds = Math.min(rampDurationSeconds, rampElapsedSeconds + deltaSeconds);
        float t = rampElapsedSeconds / rampDurationSeconds;

        float eased = t * t * (3.0F - 2.0F * t);
        currentDashFovBoost = Mth.lerp(eased, rampStartBoost, rampTargetBoost);
    }

    private static void reset() {
        currentDashFovBoost = BASE_FOV_BOOST;
        rampStartBoost = BASE_FOV_BOOST;
        rampTargetBoost = BASE_FOV_BOOST;
        rampElapsedSeconds = 0.0F;
        rampDurationSeconds = 0.0F;
        lastUpdateNanos = 0L;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float fov = (float) event.getFOV();

        if (mc.player.hasEffect(ModEffects.KINETIC_BOOST.get())) {
            float currentSpeed = (float) mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            float baseSpeed = currentSpeed / (1.0F + (float) ModEffects.SPEED_BONUS);

            float sprint = mc.player.isSprinting() ? 1.3F : 1.0F;
            float f1Current = (currentSpeed * sprint / 0.1F + 1.0F) / 2.0F;
            float f1Base = (baseSpeed * sprint / 0.1F + 1.0F) / 2.0F;

            double scale = mc.options.fovEffectScale().get();
            float multCurrent = (float) Mth.lerp(scale, 1.0D, f1Current);
            float multBase = (float) Mth.lerp(scale, 1.0D, f1Base);

            if (multCurrent > 0.01F) {
                fov = fov / multCurrent * multBase;
            }
        }

        if (fov < 120.0F) {
            fov = Math.min(120.0F, fov * currentDashFovBoost);
        }

        event.setFOV(fov);
    }
}
