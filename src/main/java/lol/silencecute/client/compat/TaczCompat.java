package lol.silencecute.client.compat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

@OnlyIn(Dist.CLIENT)
public final class TaczCompat {
    private static final String TACZ_MOD_ID = "tacz";
    private static final String OPERATOR_CLASS_NAME =
            "com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator";
    private static final String AIM_KEY_CLASS_NAME = "com.tacz.guns.client.input.AimKey";

    private static boolean initialized;
    private static boolean available;
    private static Class<?> operatorClass;
    private static Method isAimMethod;
    private static Method aimMethod;
    private static KeyMapping aimKey;
    private static KeyMapping shootKey;
    private static Method controllerShootMethod;

    private TaczCompat() {
    }

    
    public static boolean suspendAim(LocalPlayer player) {
        Object operator = operator(player);
        if (operator == null) {
            return false;
        }
        try {
            if (!Boolean.TRUE.equals(isAimMethod.invoke(operator))) {
                return false;
            }
            aimMethod.invoke(operator, false);
            return true;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            disableAfterFailure();
            return false;
        }
    }

    
    public static void keepAimSuspended(LocalPlayer player) {
        Object operator = operator(player);
        if (operator == null) {
            return;
        }
        try {
            if (Boolean.TRUE.equals(isAimMethod.invoke(operator))) {
                aimMethod.invoke(operator, false);
            }
            if (aimKey != null) {
                aimKey.setDown(false);
            }
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            disableAfterFailure();
        }
    }

    
    public static void restoreAim(Minecraft minecraft, LocalPlayer player) {
        Object operator = operator(player);
        if (operator == null) {
            return;
        }
        try {
            if (aimKey != null) {
                aimKey.setDown(isPhysicallyDown(minecraft, aimKey));
            }
            if (!Boolean.TRUE.equals(isAimMethod.invoke(operator))) {
                aimMethod.invoke(operator, true);
            }
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            disableAfterFailure();
        }
    }

    
    public static void suppressGrenadeGunInput(Minecraft minecraft) {
        initialize();
        keepAimSuspended(minecraft.player);
        if (shootKey != null) {
            shootKey.setDown(false);
            while (shootKey.consumeClick()) {

            }
        }
        if (controllerShootMethod != null) {
            try {
                controllerShootMethod.invoke(null, false);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                controllerShootMethod = null;
            }
        }
    }

    public static boolean isShootPhysicallyDown(Minecraft minecraft) {
        initialize();
        return shootKey != null && isPhysicallyDown(minecraft, shootKey);
    }

    private static boolean isPhysicallyDown(Minecraft minecraft, KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return false;
        }
        InputConstants.Key key = mapping.getKey();
        long window = minecraft.getWindow().getWindow();
        return key.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : key.getType() == InputConstants.Type.KEYSYM
                        && InputConstants.isKeyDown(window, key.getValue());
    }

    private static Object operator(LocalPlayer player) {
        initialize();
        return available && player != null && operatorClass.isInstance(player) ? player : null;
    }

    private static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!ModList.get().isLoaded(TACZ_MOD_ID)) {
            return;
        }

        try {
            Class<?> shootClass = Class.forName("com.tacz.guns.client.input.ShootKey");
            shootKey = (KeyMapping) shootClass.getField("SHOOT_KEY").get(null);
            try {
                controllerShootMethod = shootClass.getMethod("shootControllerTick", boolean.class);
            } catch (NoSuchMethodException ignored) {

            }
        } catch (ReflectiveOperationException | LinkageError | ClassCastException ignored) {

        }
        try {
            operatorClass = Class.forName(OPERATOR_CLASS_NAME);
            isAimMethod = operatorClass.getMethod("isAim");
            aimMethod = operatorClass.getMethod("aim", boolean.class);
            Class<?> aimKeyClass = Class.forName(AIM_KEY_CLASS_NAME);
            Field aimKeyField = aimKeyClass.getField("AIM_KEY");
            aimKey = (KeyMapping) aimKeyField.get(null);
            available = true;
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException
                | IllegalAccessException | ClassCastException | LinkageError exception) {
            disableAfterFailure();
        }
    }

    private static void disableAfterFailure() {
        available = false;
        operatorClass = null;
        isAimMethod = null;
        aimMethod = null;
        aimKey = null;
    }
}
