package lol.silencecute.client;

public final class RollPose {

    private static int appliedId = -1;

    private RollPose() {
    }

    public static void clear() {
        appliedId = -1;
    }

    public static void markApplied(int entityId) {
        appliedId = entityId;
    }

    public static boolean appliedFor(int entityId) {
        return appliedId == entityId;
    }
}
