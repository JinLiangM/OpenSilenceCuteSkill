package lol.silencecute.client;

import lol.silencecute.Client;
import lol.silencecute.client.anim.LunaBowAnimation;
import lol.silencecute.client.anim.LunaBowSway;
import lol.silencecute.client.compat.TaczCompat;
import lol.silencecute.client.render.entity.LunaShockArrowRenderer;
import lol.silencecute.client.render.item.LunaCompoundBowModel;
import lol.silencecute.manager.LunaShockArrowService;
import com.mojang.blaze3d.platform.InputConstants;

import lol.silencecute.api.Operator;

import lol.silencecute.config.ModConfigData;

import lol.silencecute.network.LunaShockArrowActionPacket;

import lol.silencecute.registry.ModNetwork;

import lol.silencecute.registry.ModSounds;

import net.minecraft.client.KeyMapping;

import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.util.Mth;

import net.minecraft.world.InteractionHand;

import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import net.minecraftforge.client.event.InputEvent;

import net.minecraftforge.client.event.RenderGuiEvent;

import net.minecraftforge.client.event.RenderHandEvent;

import net.minecraftforge.event.TickEvent;

import net.minecraftforge.eventbus.api.EventPriority;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

import net.minecraft.sounds.SoundEvent;
import lol.silencecute.client.font.ModFonts;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)

public final class LunaShockArrowClient {

    private static final float FULL_CHARGE_SECONDS = 0.5F;

    private static final float MIN_POWER = 0.06F;

    private static final float AIM_START_SECONDS = FULL_CHARGE_SECONDS / 3.0F;

    private static final int ARC_SEGMENTS = 3;

    private static final float ARC_START_DEG = 190.0F;

    private static final float ARC_SEGMENT_SPAN_DEG = 48.0F;

    private static final float ARC_GAP_DEG = 8.0F;

    private static final float ARC_RADIUS_INNER = 10.0F;

    private static final float ARC_RADIUS_OUTER = 12.5F;

    private static final int ARC_STEPS_PER_SEGMENT = 8;

    private static final int ARC_GREY = 0xFF6E7A82;

    private static final int ARC_BLUE = 0xFF35D7FF;

    private static final long BOUNCE_NOTICE_NANOS = 1_200_000_000L;

    private static final long BOUNCE_FADE_NANOS = 300_000_000L;

    private static final int BOUNCE_NOTICE_COLOR = 0xFF9AA6AD;

    private static final int BAR_GAP = 2;

    private static final int BAR_OFFSET_Y = 35;

    private static final int BAR_IDLE = 0xFF6E7A82;

    private static final int BAR_ACTIVE = 0xFF35D7FF;

    private static final int BAR_BACKDROP = 0x60000000;

    private static boolean drawn;

    private static boolean bounce;

    private static boolean charging;

    private static long chargeStartNano;

    private static long cooldownEndNano;

    private static boolean attackWasDown;

    private static boolean useWasDown;

    private static int lastSelectedSlot = -1;

    private static long equipStartNano;

    private static long releaseStartNano;

    private static float releasedPower;

    private static long bounceNoticeUntilNano;

    private static float releasedAimSeconds;

    private static float releasedEquipSeconds;

    private static boolean requireInputRelease;

    private static float swayMovement;

    private static float swayMovementOld;

    private static float swayAim;

    private static float swayAimOld;

    private static float swayTurnYaw;

    private static final int BAR_COUNT = 3;

    private static final int BAR_WIDTH = 16;

    private static final int BAR_HEIGHT = 2;

    private static float swayTurnYawOld;

    private static float swayTurnPitch;

    private static float swayTurnPitchOld;

    private static int actionSequence;

    private static int charges = LunaShockArrowService.MAX_CHARGES;

    private static long rechargeEndNano;

    private static void sendAction(LunaShockArrowActionPacket.Action action, float power) {

        ModNetwork.CHANNEL.sendToServer(new LunaShockArrowActionPacket(action, power, ++actionSequence));

    }

    public static boolean isReleasing() {

        return releaseStartNano != 0L && secondsSince(releaseStartNano) < LunaBowAnimation.RELEASE_SECONDS;

    }

    public static boolean blocksWeaponInput() {

        return drawn || isReleasing() || requireInputRelease;

    }

    private static float secondsSince(long start) {

        return start == 0L ? 0.0F : Math.max(0.0F, (System.nanoTime() - start) / 1.0E9F);

    }

    private LunaShockArrowClient() {

    }

    public static void toggleDrawn() {
        if (ModConfigData.operator() != Operator.LUNA) return;

        if (!drawn && LunaGrenadeClient.isHeld()) {
            LunaGrenadeClient.stowNow();
        }

        if (!drawn && (charges <= 0 || LunaReconArrowClient.isDrawn() || isReleasing())) {
            ClientEvents.playSkillUnavailableSound();
            return;
        }

        if (drawn) {
            stowNow();
            return;
        }
        sendAction(LunaShockArrowActionPacket.Action.TOGGLE, 0.0F);
        drawn = true;
        charging = false;
        equipStartNano = System.nanoTime();
        releaseStartNano = 0L;
        Minecraft minecraft = Minecraft.getInstance();
        attackWasDown = isPhysicallyDown(minecraft.options.keyAttack);
        useWasDown = isPhysicallyDown(minecraft.options.keyUse);
        lastSelectedSlot = minecraft.player == null ? -1
                : minecraft.player.getInventory().selected;
    }

    public static void stowNow() {

        releaseStartNano = 0L;

        if (!drawn) {

            return;

        }

        drawn = false;

        charging = false;

        releaseStartNano = 0L;

        requireInputRelease = true;

        sendAction(LunaShockArrowActionPacket.Action.STOW, 0.0F);

    }

    public static boolean isDrawn() {

        return drawn;

    }

    public static boolean bounceEnabled() {

        return bounce;

    }

    public static boolean isCharging() {

        return charging;

    }

    private static void playLocal(SoundEvent event, float volume, float pitch) {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {

            minecraft.player.playSound(event, volume, pitch);

        }

    }

    public static float chargePower() {

        if (!charging) {

            return 0.0F;

        }

        float seconds = (System.nanoTime() - chargeStartNano) / 1.0E9F;

        return Math.max(MIN_POWER, Math.min(1.0F, seconds / FULL_CHARGE_SECONDS));

    }

    public static int chargeStage() {

        if (!charging) return 0;

        return Math.min(3, 1 + (int) (secondsSince(chargeStartNano)

                / (FULL_CHARGE_SECONDS / 3.0F)));

    }

    private static float currentAimSeconds() {

        if (!charging) {

            return 0.0F;

        }

        return Math.max(0.0F, secondsSince(chargeStartNano) - AIM_START_SECONDS);

    }

    public static long cooldownMillis() {

        return Math.max(0L, (cooldownEndNano - System.nanoTime()) / 1_000_000L);

    }

    public static int arrowCharges() {

        return charges;

    }

    public static long rechargeMillis() {

        return Math.max(0L, (rechargeEndNano - System.nanoTime()) / 1_000_000L);

    }

    public static void applyServerSync(boolean serverDrawn, boolean serverBounce,

                                       int cooldownMillis, int serverCharges,

                                       int rechargeMillis, int sequence) {

        if (sequence < actionSequence) {

            return;

        }

        actionSequence = sequence;

        if (serverDrawn && !drawn) {

            equipStartNano = System.nanoTime();

        }

        drawn = serverDrawn;

        bounce = serverBounce;

        cooldownEndNano = System.nanoTime() + Math.max(0, cooldownMillis) * 1_000_000L;

        charges = Math.max(0, Math.min(LunaShockArrowService.MAX_CHARGES, serverCharges));

        rechargeEndNano = System.nanoTime() + Math.max(0, rechargeMillis) * 1_000_000L;

        if (!drawn) {

            charging = false;

        }

    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)

    public static void onClientTick(TickEvent.ClientTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {

            return;

        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || !minecraft.player.isAlive()

                || minecraft.player.isSpectator()

                || ModConfigData.operator() != Operator.LUNA) {

            clearState();

            return;

        }

        boolean attackDown = isPhysicallyDown(minecraft.options.keyAttack);

        boolean useDown = isPhysicallyDown(minecraft.options.keyUse);

        updateSway(minecraft);

        int selected = minecraft.player.getInventory().selected;

        if (selected != lastSelectedSlot) {

            boolean switched = lastSelectedSlot >= 0;

            lastSelectedSlot = selected;

            if (switched && drawn) {

                stowNow();

            }

        }

        if (drawn && minecraft.screen == null && minecraft.isWindowActive()) {

            if (attackDown && !attackWasDown) {

                beginCharge();

            }

            if (!attackDown && attackWasDown && charging) {

                release();

            }

            if (useDown && !useWasDown) {

                toggleBounce();

            }

        } else if (charging) {

            charging = false;

        }

        if (!drawn && !isReleasing() && !attackDown && !useDown

                && !TaczCompat.isShootPhysicallyDown(minecraft)) {

            requireInputRelease = false;

        }

        attackWasDown = attackDown;

        useWasDown = useDown;

    }

    private static void beginCharge() {

        if (drawn && !charging && cooldownMillis() <= 0L) {

            charging = true;

            chargeStartNano = System.nanoTime();

            playLocal(ModSounds.LUNA_BOW_DRAW.get(), 1.0F, 1.0F);

        }

    }

    private static void toggleBounce() {

        sendAction(LunaShockArrowActionPacket.Action.TOGGLE_BOUNCE, 0.0F);

        bounce = !bounce;

        bounceNoticeUntilNano = System.nanoTime() + BOUNCE_NOTICE_NANOS;

        playLocal(ModSounds.LUNA_BOW_BOUNCE.get(), 1.0F, bounce ? 1.15F : 0.9F);

    }

    private static void release() {

        if (!drawn || !charging) return;

        releasedPower = chargePower();

        releasedAimSeconds = currentAimSeconds();

        releasedEquipSeconds = secondsSince(equipStartNano);

        releaseStartNano = System.nanoTime();

        drawn = false;

        charging = false;

        requireInputRelease = true;

        sendAction(LunaShockArrowActionPacket.Action.FIRE, releasedPower);

        playLocal(ModSounds.LUNA_BOW_SHOOT.get(), 1.0F, 1.0F);

        cooldownEndNano = System.nanoTime()

                + LunaShockArrowService.FIRE_COOLDOWN_TICKS * 50_000_000L;

        charges = Math.max(0, charges - 1);

        if (charges == 0 && rechargeMillis() <= 0L) {

            rechargeEndNano = System.nanoTime()

                    + LunaShockArrowService.RECHARGE_TICKS * 50_000_000L;

        }

    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)

    public static void onMouseButton(InputEvent.MouseButton.Pre event) {

        Minecraft minecraft = Minecraft.getInstance();

        if (!drawn || minecraft.player == null || minecraft.screen != null || !minecraft.isWindowActive()) return;

        InputConstants.Key attack = minecraft.options.keyAttack.getKey();

        InputConstants.Key use = minecraft.options.keyUse.getKey();

        if (attack.getType() == InputConstants.Type.MOUSE && attack.getValue() == event.getButton()) {

            if (event.getAction() == GLFW.GLFW_PRESS) {

                beginCharge();

                attackWasDown = true;

            } else if (event.getAction() == GLFW.GLFW_RELEASE) {

                release();

                attackWasDown = false;

            }

            event.setCanceled(true);

        } else if (use.getType() == InputConstants.Type.MOUSE && use.getValue() == event.getButton()) {

            if (event.getAction() == GLFW.GLFW_PRESS) {

                toggleBounce();

                useWasDown = true;

            } else if (event.getAction() == GLFW.GLFW_RELEASE) {

                useWasDown = false;

            }

            event.setCanceled(true);

        }

    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)

    public static void onKeyInput(InputEvent.Key event) {

        if (event.getAction() != GLFW.GLFW_PRESS || !drawn) {

            return;

        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.screen != null) {

            return;

        }

        for (KeyMapping hotbarKey : minecraft.options.keyHotbarSlots) {

            InputConstants.Key key = hotbarKey.getKey();

            if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() == event.getKey()) {

                stowNow();

                return;

            }

        }

    }

    private static boolean isPhysicallyDown(KeyMapping mapping) {

        InputConstants.Key key = mapping.getKey();

        long window = Minecraft.getInstance().getWindow().getWindow();

        if (key.getType() == InputConstants.Type.MOUSE) {

            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;

        }

        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() >= 0

                && InputConstants.isKeyDown(window, key.getValue());

    }

    private static void updateSway(Minecraft minecraft) {

        swayMovementOld = swayMovement;

        swayAimOld = swayAim;

        swayTurnYawOld = swayTurnYaw;

        swayTurnPitchOld = swayTurnPitch;

        if (!drawn && !isReleasing()) {

            resetSway();

            return;

        }

        if (minecraft.isPaused()) return;

        var player = minecraft.player;

        float speed = player.onGround() && !player.isPassenger()

                ? Mth.clamp((player.walkDist - player.walkDistO) * 5.0F, 0.0F, 1.0F) : 0.0F;

        swayMovement += (speed - swayMovement) * 0.35F;

        float targetAim = chargeStage() >= 2 || isReleasing() ? 1.0F : 0.0F;

        swayAim = Mth.clamp(swayAim + Mth.clamp(targetAim - swayAim, -0.5F, 0.5F), 0.0F, 1.0F);

        swayTurnYaw += (Mth.clamp(Mth.wrapDegrees(player.getYRot() - player.yRotO), -12, 12)

                - swayTurnYaw) * 0.35F;

        swayTurnPitch += (Mth.clamp(player.getXRot() - player.xRotO, -10, 10)

                - swayTurnPitch) * 0.35F;

    }

    private static void resetSway() {

        swayMovement = swayMovementOld = swayAim = swayAimOld = 0.0F;

        swayTurnYaw = swayTurnYawOld = swayTurnPitch = swayTurnPitchOld = 0.0F;

    }

    private static LunaBowSway.Motion currentSway(Minecraft minecraft, float partialTick) {

        if (minecraft.player == null || minecraft.screen != null || !minecraft.isWindowActive()

                || minecraft.isPaused() || minecraft.player.isSpectator()

                || !minecraft.player.isAlive() || minecraft.getCameraEntity() != minecraft.player

                || !minecraft.options.getCameraType().isFirstPerson() || (!drawn && !isReleasing())) {

            return LunaBowSway.Motion.STILL;

        }

        float envelope = LunaBowAnimation.smooth(secondsSince(equipStartNano) / 0.35F);

        if (isReleasing()) envelope *= 1.0F - LunaBowAnimation.smooth(

                secondsSince(releaseStartNano) / LunaBowAnimation.RELEASE_SECONDS);

        float aim = Mth.lerp(partialTick, swayAimOld, swayAim);

        if (charging) aim = Math.max(aim, LunaBowAnimation.smooth(

                currentAimSeconds() / LunaBowAnimation.AIM_SECONDS));

        if (isReleasing()) aim = 1.0F;

        return LunaBowSway.sample(minecraft.player.tickCount + partialTick,

                Mth.lerp(partialTick, minecraft.player.walkDistO, minecraft.player.walkDist),

                Mth.lerp(partialTick, swayMovementOld, swayMovement),

                Mth.lerp(partialTick, swayTurnYawOld, swayTurnYaw),

                Mth.lerp(partialTick, swayTurnPitchOld, swayTurnPitch), aim, envelope);

    }

    @SubscribeEvent

    public static void onCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {

        LunaBowSway.Motion sway = currentSway(Minecraft.getInstance(), (float) event.getPartialTick());

        event.setPitch(event.getPitch() + sway.cameraPitch());

        event.setYaw(event.getYaw() + sway.cameraYaw());

        event.setRoll(event.getRoll() + sway.cameraRoll());

    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)

    public static void onRenderHand(RenderHandEvent event) {

        boolean releasing = isReleasing();

        if (!drawn && !releasing) return;

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null

                || !minecraft.options.getCameraType().isFirstPerson()) return;

        event.setCanceled(true);

        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        LunaBowAnimation.Pose pose = LunaBowAnimation.sample(

                releasing ? releasedPower : chargePower(),

                releasing ? releasedEquipSeconds : secondsSince(equipStartNano),

                releasing ? secondsSince(releaseStartNano) : -1.0F,

                releasing ? releasedAimSeconds : currentAimSeconds());

        pose = currentSway(minecraft, event.getPartialTick()).apply(pose);

        LunaCompoundBowModel.render(event.getPoseStack(), event.getMultiBufferSource(),

                event.getPackedLight(), minecraft.player, pose, !releasing);

    }

    @SubscribeEvent

    public static void onRenderGui(RenderGuiEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();

        if (!drawn || minecraft.options.hideGui || minecraft.player == null) {

            return;

        }

        GuiGraphics graphics = event.getGuiGraphics();

        int centerX = event.getWindow().getGuiScaledWidth() / 2;

        int centerY = event.getWindow().getGuiScaledHeight() / 2;

        drawChargeBars(graphics, centerX, centerY, chargeStage());

        long remaining = bounceNoticeUntilNano - System.nanoTime();

        if (remaining <= 0L) {

            return;

        }

        String label = bounce ? "反弹开启" : "反弹关闭";

        Font font = minecraft.font;

        float alpha = Math.min(1.0F, remaining / (float) BOUNCE_FADE_NANOS);

        int color = ((int) (alpha * 255.0F) << 24) | (BOUNCE_NOTICE_COLOR & 0x00FFFFFF);

        drawScaledString(graphics, font, label,

                centerX - ModFonts.width(label) * 0.7F / 2.0F,

                centerY + BAR_OFFSET_Y + BAR_HEIGHT + 8.0F, 0.7F, color);

    }

    private static void drawChargeBars(GuiGraphics graphics, int centerX, int centerY, int stage) {

        int totalWidth = BAR_COUNT * BAR_WIDTH + (BAR_COUNT - 1) * BAR_GAP;

        int left = centerX - totalWidth / 2;

        int top = centerY + BAR_OFFSET_Y;

        for (int i = 0; i < BAR_COUNT; i++) {

            int x = left + i * (BAR_WIDTH + BAR_GAP);

            int color = stage > i ? BAR_ACTIVE : BAR_IDLE;

            graphics.fill(x, top, x + BAR_WIDTH, top + BAR_HEIGHT, color);

        }

    }

    private static void drawScaledString(GuiGraphics graphics, Font font, String text,

                                         float x, float y, float scale, int color) {

        graphics.pose().pushPose();

        graphics.pose().translate(x, y, 0);

        graphics.pose().scale(scale, scale, 1.0F);

        ModFonts.drawString(graphics, text, 0, 0, color, true);

        graphics.pose().popPose();

    }

    @SubscribeEvent

    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {

        clearState();

        actionSequence = 0;

        LunaShockArrowRenderer.clearVisual();

    }

    private static void clearState() {

        resetSway();

        drawn = false;

        bounce = false;

        charging = false;

        chargeStartNano = 0L;

        cooldownEndNano = 0L;

        attackWasDown = false;

        useWasDown = false;

        lastSelectedSlot = -1;

        equipStartNano = 0L;

        releaseStartNano = 0L;

        releasedPower = 0.0F;

        releasedAimSeconds = 0.0F;

        releasedEquipSeconds = 0.0F;

        requireInputRelease = false;

        charges = LunaShockArrowService.MAX_CHARGES;

        rechargeEndNano = 0L;

        bounceNoticeUntilNano = 0L;

    }

}

