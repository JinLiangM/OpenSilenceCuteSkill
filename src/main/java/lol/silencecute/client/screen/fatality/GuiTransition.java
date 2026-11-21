package lol.silencecute.client.screen.fatality;

import net.minecraft.client.Minecraft;

public final class GuiTransition {

    private static final float OUT_SECONDS = 0.15f;

    private static final float WAIT_SECONDS = 0.05f;
    private static final float IN_SECONDS = 0.15f;

    private static final float STALL_SECONDS = 0.15f;

    private static final float MAX_STEP_SECONDS = 0.1f;

    private enum Phase {
        IDLE, OUT, WAIT, IN
    }

    private static Phase phase = Phase.IDLE;

    private static float phaseElapsed;

    private static long lastNanos;
    private static String target = "NAVEN";

    private static Runnable pendingAction;

    private GuiTransition() {
    }

    public static void request(String style) {
        request(style, null);
    }

    public static void request(String style, Runnable onSwitch) {
        target = style == null ? "NAVEN" : style;
        pendingAction = onSwitch;
        phase = Phase.OUT;
        phaseElapsed = 0.0f;
        lastNanos = 0L;
    }

    public static float scale() {
        return switch (phase) {
            case OUT -> 1.0f - ease(progress(OUT_SECONDS));
            case WAIT -> 0.0f;
            case IN -> ease(progress(IN_SECONDS));
            case IDLE -> 1.0f;
        };
    }

    public static boolean isSwitching() {
        return phase != Phase.IDLE;
    }

    public static void tick() {
        long now = System.nanoTime();
        float rawDt = lastNanos == 0L ? 1.0f / 60.0f : (now - lastNanos) / 1.0e9f;
        lastNanos = now;

        if (phase == Phase.IDLE) {
            phaseElapsed = 0.0f;
            return;
        }

        if (rawDt <= STALL_SECONDS) phaseElapsed += Math.min(MAX_STEP_SECONDS, rawDt);

        switch (phase) {
            case OUT -> {
                if (phaseElapsed >= OUT_SECONDS) {
                    phase = Phase.WAIT;
                    phaseElapsed = 0.0f;
                }
            }
            case WAIT -> {
                if (phaseElapsed >= WAIT_SECONDS) {
                    phase = Phase.IN;
                    phaseElapsed = 0.0f;

                    runPending();
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft != null) {
                        boolean naven = "NAVEN".equalsIgnoreCase(target);
                        minecraft.setScreen(naven ? new NvClickGui(null) : new FaClickGui(null));
                    } else {
                        phase = Phase.IDLE;
                    }
                }
            }
            case IN -> {
                if (phaseElapsed >= IN_SECONDS) {
                    phase = Phase.IDLE;
                    phaseElapsed = 0.0f;
                }
            }
            default -> {
            }
        }
    }

    public static void reset() {
        phase = Phase.IDLE;
        phaseElapsed = 0.0f;
        lastNanos = 0L;

        runPending();
    }

    private static float progress(float seconds) {
        if (seconds <= 0.0f) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, phaseElapsed / seconds));
    }

    private static float ease(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static void runPending() {
        Runnable action = pendingAction;
        pendingAction = null;
        if (action == null) return;
        try {
            action.run();
        } catch (Throwable ignored) {

        }
    }
}
