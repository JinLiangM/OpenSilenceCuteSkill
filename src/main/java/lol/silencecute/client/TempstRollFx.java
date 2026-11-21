package lol.silencecute.client;

import java.util.HashMap;
import java.util.Map;

public final class TempstRollFx {

    public record Roll(long startNanos, long durationNanos, float dirX, float dirZ) {

        public float progress() {
            long elapsed = System.nanoTime() - startNanos;
            if (durationNanos <= 0L) return 1.0F;
            return Math.max(0.0F, Math.min(1.0F, elapsed / (float) durationNanos));
        }

        public boolean finished() {
            return System.nanoTime() - startNanos >= durationNanos;
        }
    }

    private static final Map<Integer, Roll> ROLLS = new HashMap<>();

    private TempstRollFx() {
    }

    public static void track(int entityId, int ticks, float dirX, float dirZ) {
        ROLLS.put(entityId, new Roll(System.nanoTime(),
                Math.max(1, ticks) * 50_000_000L, dirX, dirZ));
    }

    public static Roll roll(int entityId) {
        Roll roll = ROLLS.get(entityId);
        if (roll == null) return null;
        if (roll.finished()) {
            ROLLS.remove(entityId);
            return null;
        }
        return roll;
    }

    public static void clear() {
        ROLLS.clear();
    }
}
