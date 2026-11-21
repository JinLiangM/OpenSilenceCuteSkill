package lol.silencecute.client;

import java.util.HashSet;
import java.util.Set;

import lol.silencecute.Client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TempstRollRender {

    private static final double PIVOT_Y = 0.55D;

    private static final double SINK_EDGE = 0.45D;

    private static final double SINK_MID = 0.15D;

    private static final Set<Integer> PUSHED = new HashSet<>();

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("silencecute_skill");
    private static boolean probePre;
    private static boolean probeRoll;

    private TempstRollRender() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        try {
            Player player = event.getEntity();
            if (!probePre) {
                probePre = true;
                LOGGER.info("[TempstRoll] RenderPlayerEvent active");
            }
            float progress = TempstRollClient.progressFor(player.getId());
            if (lol.silencecute.client.compat.YsmCompat.useYsmAnimation()
                    && RollPose.appliedFor(player.getId())) {
                return;
            }
            if (progress > 0.0F && progress < 1.0F && !probeRoll) {
                probeRoll = true;
                LOGGER.info("[TempstRoll] progress=" + progress);
            }
            if (progress <= 0.0F || progress >= 1.0F) return;

            float dirX = TempstRollClient.dirX();
            float dirZ = TempstRollClient.dirZ();

            TempstRollFx.Roll roll = TempstRollFx.roll(player.getId());
            if (roll != null) {
                dirX = roll.dirX();
                dirZ = roll.dirZ();
            }

            double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (!Double.isFinite(length) || length <= 1.0E-4D) return;

            float axisX = (float) (dirZ / length);
            float axisZ = (float) (-dirX / length);
            float angle = (float) Math.toRadians(360.0D * progress);
            double phase = Math.abs(2.0D * progress - 1.0D);
            double sink = SINK_MID + SINK_EDGE * phase * phase;

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(0.0D, -sink, 0.0D);
            poseStack.translate(0.0D, PIVOT_Y, 0.0D);
            poseStack.mulPose(new org.joml.Quaternionf().rotateAxis(angle, axisX, 0.0F, axisZ));
            poseStack.translate(0.0D, -PIVOT_Y, 0.0D);

            PUSHED.add(player.getId());
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        try {
            Entity entity = event.getEntity();
            if (PUSHED.remove(entity.getId())) {
                event.getPoseStack().popPose();
            }
        } catch (Throwable ignored) {
        }
    }

    public static void reset() {
        PUSHED.clear();
    }
}