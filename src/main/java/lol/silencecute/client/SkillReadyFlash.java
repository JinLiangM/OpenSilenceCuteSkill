package lol.silencecute.client;

import lol.silencecute.Client;
import lol.silencecute.api.Operator;
import lol.silencecute.client.render.hud.SkillHudOverlay;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SkillReadyFlash {

    private static final long FLASH_NANOS = 260_000_000L;

    private static final float PEAK_ALPHA = 0.85F;

    private static final int SLOTS = 3;

    private static final long[] START_NANOS = new long[SLOTS];
    private static final boolean[] CHARGING = new boolean[SLOTS];

    private static Operator lastOperator;
    private static boolean operatorKnown;

    private SkillReadyFlash() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (!ModConfigData.operatorEnabled()) return;

        Operator operator = ModConfigData.operator();
        if (!operatorKnown || operator != lastOperator) {
            operatorKnown = true;
            lastOperator = operator;
            for (int slot = 0; slot < SLOTS; slot++) {
                CHARGING[slot] = SkillHudOverlay.isSlotRecharging(operator, slot);
            }
            return;
        }

        for (int slot = 0; slot < SLOTS; slot++) {
            boolean charging = SkillHudOverlay.isSlotRecharging(operator, slot);
            if (CHARGING[slot] && !charging) {
                trigger(slot, minecraft.player);
            }
            CHARGING[slot] = charging;
        }
    }

    public static float flashAlpha(int slot) {
        if (slot < 0 || slot >= SLOTS) return 0.0F;

        long start = START_NANOS[slot];
        if (start == 0L) return 0.0F;

        long elapsed = System.nanoTime() - start;
        if (elapsed < 0L || elapsed >= FLASH_NANOS) return 0.0F;

        float t = elapsed / (float) FLASH_NANOS;
        float fade;
        if (t < 0.45F) {
            fade = t / 0.45F;
        } else {
            float k = (t - 0.45F) / 0.55F;
            fade = 1.0F - k * k;
        }
        return Math.max(0.0F, fade) * PEAK_ALPHA;
    }

    private static void trigger(int slot, LocalPlayer player) {
        START_NANOS[slot] = System.nanoTime();
        player.playSound(ModSounds.SKILL_READY.get(), 1.0F, 1.0F);
    }

    public static void reset() {
        operatorKnown = false;
        for (int slot = 0; slot < SLOTS; slot++) {
            START_NANOS[slot] = 0L;
            CHARGING[slot] = false;
        }
    }
}
