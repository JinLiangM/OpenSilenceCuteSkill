package lol.silencecute.client.compat;

import lol.silencecute.config.ModConfigData;

import net.minecraftforge.fml.ModList;

public final class YsmCompat {

    public static final String MOD_ID = "yes_steve_model";

    public static final String MODE_VANILLA = "原版翻滚";

    public static final String MODE_YSM = "YSM蹲姿";

    private static Boolean loaded;

    private YsmCompat() {
    }

    public static boolean isYsmLoaded() {
        if (loaded == null) {
            try {
                loaded = ModList.get() != null && ModList.get().isLoaded(MOD_ID);
            } catch (Throwable t) {
                loaded = Boolean.FALSE;
            }
        }
        return loaded;
    }

    public static boolean isYsmMode() {
        try {
            return MODE_YSM.equals(ModConfigData.yssmCompatMode());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean useYsmAnimation() {
        return isYsmMode() && isYsmLoaded();
    }

    public static boolean isApiMode() {
        return isYsmMode();
    }
}
