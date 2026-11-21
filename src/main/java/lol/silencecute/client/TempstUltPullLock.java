package lol.silencecute.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import lol.silencecute.Client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TempstUltPullLock {

    private static final float CROUCH_EYE_HEIGHT = 1.27F;

    private static boolean locked;
    private static boolean eyeLocked;
    private static float originalEyeHeight = -1.0F;
    private static Field eyeHeightField;

    private TempstUltPullLock() {
    }

    public static boolean isLocked() {
        return locked;
    }

    public static void begin(float yaw, float pitch) {
        locked = true;
        eyeLocked = true;
    }

    public static void end() {
        locked = false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            locked = false;
            eyeLocked = false;
            return;
        }

        if (locked) {
            if (player.input != null) {
                player.input.forwardImpulse = 0.0F;
                player.input.leftImpulse = 0.0F;
                player.input.jumping = false;
                player.input.shiftKeyDown = false;
            }
            player.setSprinting(false);
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
            player.setPose(Pose.SLEEPING);
        }

        if (eyeLocked) {
            if (locked) {
                restoreEyeHeight(player);
            } else if (player.getPose() != Pose.STANDING) {
                applyEyeHeight(player, CROUCH_EYE_HEIGHT);
            } else {
                restoreEyeHeight(player);
                eyeLocked = false;
            }
        }
    }

    @SubscribeEvent
    public static void onMovementInput(net.minecraftforge.client.event.MovementInputUpdateEvent event) {
        if (!locked) return;
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null || event.getEntity() != minecraft.player) return;

        net.minecraft.client.player.Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
    }

    private static void applyEyeHeight(LocalPlayer player, float height) {
        Field field = eyeField();
        if (field == null) return;
        try {
            if (originalEyeHeight < 0.0F) {
                originalEyeHeight = field.getFloat(player);
            }
            field.setFloat(player, height);
        } catch (Throwable ignored) {
        }
    }

    private static void restoreEyeHeight(LocalPlayer player) {
        Field field = eyeField();
        if (field == null || originalEyeHeight < 0.0F) return;
        try {
            field.setFloat(player, originalEyeHeight);
        } catch (Throwable ignored) {
        }
        originalEyeHeight = -1.0F;
    }

    private static Field eyeField() {
        if (eyeHeightField != null) return eyeHeightField;
        for (String name : new String[]{"eyeHeight", "f_19815_"}) {
            try {
                Field field = Entity.class.getDeclaredField(name);
                field.setAccessible(true);
                eyeHeightField = field;
                return field;
            } catch (Throwable ignored) {
            }
        }
        for (Field field : Entity.class.getDeclaredFields()) {
            if (field.getType() == float.class && Modifier.isPublic(field.getModifiers())
                    && field.getName().toLowerCase().contains("eye")) {
                field.setAccessible(true);
                eyeHeightField = field;
                return field;
            }
        }
        return null;
    }
}
