package lol.silencecute.client;

import java.util.Locale;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
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
import org.lwjgl.opengl.GL11;

import lol.silencecute.Client;
import lol.silencecute.api.Operator;
import lol.silencecute.client.anim.LunaGrenadeAnimation;
import lol.silencecute.client.anim.LunaGrenadeAnimation.Transform;
import lol.silencecute.client.anim.LunaGrenadeAnimation.Vector;
import lol.silencecute.client.compat.TaczCompat;
import lol.silencecute.client.render.item.LunaGrenadeModel;
import lol.silencecute.config.ModConfigData;
import lol.silencecute.manager.LunaGrenadeService;
import lol.silencecute.network.LunaGrenadeActionPacket;
import lol.silencecute.registry.ModNetwork;
import lol.silencecute.client.font.ModFonts;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class LunaGrenadeClient {
    private static final ResourceLocation COUNTDOWN_ICON = ResourceLocation.fromNamespaceAndPath(
            Client.MOD_ID, "textures/gui/luna_grenade_countdown.png");

    private static LunaGrenadeService.HeldState state = LunaGrenadeService.HeldState.NONE;
    private static long fuseEndNano;
    private static int charges = LunaGrenadeService.MAX_CHARGES;
    private static long rechargeEndNano;
    private static int lastSelectedSlot = -1;
    private static boolean throwSent;
    private static boolean primeSent;
    private static boolean attackWasDown;
    private static boolean requireAttackRelease;
    private static String animation = "static_idle";
    private static long animationStartedNano;
    private static float transitionSeconds;
    private static Map<String, Transform> transitionFrom;
    private static double savedFovEffectScale = -1.0D;

    private LunaGrenadeClient() {}

    public static void toggleEquipped() {
        if (ModConfigData.operator() != Operator.LUNA || isPrimed()) return;

        if (state == LunaGrenadeService.HeldState.NONE) {
            if (LunaShockArrowClient.isDrawn()) LunaShockArrowClient.stowNow();
            if (LunaReconArrowClient.isDrawn()) LunaReconArrowClient.stowNow();
        }

        if (state == LunaGrenadeService.HeldState.NONE
                && (charges <= 0 || isAnimatingExit())) {
            ClientEvents.playSkillUnavailableSound();
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new LunaGrenadeActionPacket(LunaGrenadeActionPacket.Action.EQUIP));
        if (state == LunaGrenadeService.HeldState.NONE) {
            state = LunaGrenadeService.HeldState.EQUIPPED;
            startAnimation("draw", 0);
        } else {
            startAnimation("put_away", 0.08F);
            state = LunaGrenadeService.HeldState.NONE;
        }
        throwSent = false;
        primeSent = false;
        Minecraft minecraft = Minecraft.getInstance();
        attackWasDown = isAttackPhysicallyDown(minecraft);
        requireAttackRelease = true;
        suppressWeaponInput(minecraft);
    }
    public static void applyServerSync(byte rawState, int fuseTicks, int syncedCharges, int rechargeMillis) {
        LunaGrenadeService.HeldState[] values = LunaGrenadeService.HeldState.values();
        int index = Math.max(0, Math.min(values.length - 1, rawState));
        LunaGrenadeService.HeldState previousState = state;
        LunaGrenadeService.HeldState nextState = values[index];
        long now = System.nanoTime();
        charges = Math.max(0, Math.min(LunaGrenadeService.MAX_CHARGES, syncedCharges));
        rechargeEndNano = now + Math.max(0, rechargeMillis) * 1_000_000L;
        if (throwSent && nextState != LunaGrenadeService.HeldState.NONE) return;
        if (primeSent && nextState == LunaGrenadeService.HeldState.EQUIPPED) return;
        primeSent = false;
        if (nextState != previousState) {
            if (nextState == LunaGrenadeService.HeldState.EQUIPPED) startAnimation("draw", 0);
            else if (nextState == LunaGrenadeService.HeldState.PRIMED) startAnimation("unlock_safe", 0.08F);
            else if (previousState == LunaGrenadeService.HeldState.PRIMED) startAnimation("throw", 0.06F);
            else if (previousState == LunaGrenadeService.HeldState.EQUIPPED) startAnimation("put_away", 0.08F);
        }
        state = nextState;
        if (isPrimed()) fuseEndNano = now + Math.max(0, fuseTicks) * 50_000_000L;
        else {
            fuseEndNano = 0L;
            throwSent = false;
        }
    }

    public static long rechargeMillis() {
        return Math.max(0L, (rechargeEndNano - System.nanoTime()) / 1_000_000L);
    }

    public static int grenadeCharges() {
        return charges;
    }

    public static boolean isHeld() {
        return state != LunaGrenadeService.HeldState.NONE;
    }

    public static boolean isPrimed() {
        return state == LunaGrenadeService.HeldState.PRIMED;
    }

    public static double fuseSeconds() {
        return isPrimed() ? Math.max(0.0D, (fuseEndNano - System.nanoTime()) / 1_000_000_000.0D) : 0;
    }

    public static boolean blocksWeaponInput() {
        return isHeld() || isAnimatingExit() || requireAttackRelease
                || LunaShockArrowClient.blocksWeaponInput()
                || LunaReconArrowClient.blocksWeaponInput();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (blocksWeaponInput() && (event.isAttack() || event.isUseItem())) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!blocksWeaponInput() || minecraft.screen != null || minecraft.player == null) return;
        InputConstants.Key attackKey = minecraft.options.keyAttack.getKey();
        boolean attack = attackKey.getType() == InputConstants.Type.MOUSE && attackKey.getValue() == event.getButton();
        if (attack) {
            if (event.getAction() == GLFW.GLFW_PRESS && !attackWasDown) {
                requestPrime();
                attackWasDown = true;
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                attackWasDown = false;
                requestThrow();
            }
        }
        if (attack || event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            event.setCanceled(true);
            suppressWeaponInput(minecraft);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHotbarKeyPressed(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (state != LunaGrenadeService.HeldState.EQUIPPED || minecraft.player == null
                || minecraft.screen != null || !minecraft.isWindowActive()
                || event.getAction() != GLFW.GLFW_PRESS) return;
        for (KeyMapping hotbarKey : minecraft.options.keyHotbarSlots) {
            InputConstants.Key key = hotbarKey.getKey();
            if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() == event.getKey()) {
                stowForWeaponSwitch();
                return;
            }
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (ModConfigData.grenadeFovEffect()) {

            if (minecraft.options.fovEffectScale().get() != 0.0D) {
                if (savedFovEffectScale == -1.0D) {
                    savedFovEffectScale = minecraft.options.fovEffectScale().get();
                }
                minecraft.options.fovEffectScale().set(0.0D);
            }
        } else {

            restoreFovEffect();
        }

        if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                || ModConfigData.operator() != Operator.LUNA) {
            clearLocalState();
            return;
        }

        boolean attackDown = isAttackPhysicallyDown(minecraft);
        if (event.phase == TickEvent.Phase.START) {
            int selected = minecraft.player.getInventory().selected;
            if (selected != lastSelectedSlot) {
                boolean switchedSlot = lastSelectedSlot >= 0;
                lastSelectedSlot = selected;
                if (switchedSlot && state == LunaGrenadeService.HeldState.EQUIPPED) stowForWeaponSwitch();
            }
            if (minecraft.screen == null && minecraft.isWindowActive()) {
                if (isHeld() && attackDown && !attackWasDown) requestPrime();
                else if (isPrimed() && !attackDown) requestThrow();
            } else if (isPrimed()) requestThrow();
            attackWasDown = attackDown;
        }
        if (isPrimed() && fuseSeconds() <= 0.6D) requestThrow();
        if (blocksWeaponInput()) {
            suppressWeaponInput(minecraft);
            if (!isHeld() && !isAnimatingExit() && !attackDown
                    && !TaczCompat.isShootPhysicallyDown(minecraft)) requireAttackRelease = false;
        }
    }

    private static boolean isAttackPhysicallyDown(Minecraft minecraft) {
        InputConstants.Key key = minecraft.options.keyAttack.getKey();
        long window = minecraft.getWindow().getWindow();
        return key.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : key.getType() == InputConstants.Type.KEYSYM && key.getValue() >= 0
                && InputConstants.isKeyDown(window, key.getValue());
    }

    private static void suppressWeaponInput(Minecraft minecraft) {
        minecraft.options.keyAttack.setDown(false);
        minecraft.options.keyUse.setDown(false);
        while (minecraft.options.keyAttack.consumeClick()) {}
        while (minecraft.options.keyUse.consumeClick()) {}
        if (minecraft.player != null) {
            if (minecraft.gameMode != null) {
                minecraft.gameMode.stopDestroyBlock();
                if (minecraft.player.isUsingItem()) minecraft.gameMode.releaseUsingItem(minecraft.player);
            }
            minecraft.player.stopUsingItem();
            TaczCompat.suppressGrenadeGunInput(minecraft);
        }
    }

    private static void requestPrime() {
        if (state != LunaGrenadeService.HeldState.EQUIPPED) return;
        startAnimation("unlock_safe", 0.08F);
        state = LunaGrenadeService.HeldState.PRIMED;
        fuseEndNano = System.nanoTime() + LunaGrenadeService.FULL_FUSE_TICKS * 50_000_000L;
        throwSent = false;
        primeSent = true;
        requireAttackRelease = true;
        ModNetwork.CHANNEL.sendToServer(new LunaGrenadeActionPacket(LunaGrenadeActionPacket.Action.PRIME));
    }

    private static void stowForWeaponSwitch() {
        ModNetwork.CHANNEL.sendToServer(new LunaGrenadeActionPacket(LunaGrenadeActionPacket.Action.CANCEL));
        startAnimation("put_away", 0.08F);
        state = LunaGrenadeService.HeldState.NONE;
        throwSent = false;
        primeSent = false;
    }

    private static void requestThrow() {
        if (throwSent || !isPrimed()) return;
        startAnimation("throw", 0.06F);
        throwSent = true;
        ModNetwork.CHANNEL.sendToServer(new LunaGrenadeActionPacket(LunaGrenadeActionPacket.Action.THROW));
        state = LunaGrenadeService.HeldState.NONE;
        fuseEndNano = 0L;
        charges = Math.max(0, charges - 1);
        if (charges == 0 && rechargeMillis() <= 0L) {
            rechargeEndNano = System.nanoTime() + LunaGrenadeService.RECHARGE_TICKS * 50_000_000L;
        }
    }

    private static final float[][] THROW_ARM_KEYS = {
            {0.00F, -9.0F, -4.0F, 3.0F, -20.0F},
            {0.05F, -10.0F, -6.0F, 7.0F, -35.0F},
            {0.13F, -1.0F, -3.0F, -11.0F, 55.0F},
            {0.20F, -3.0F, -8.0F, -8.0F, 40.0F},
            {0.30F, -5.0F, -14.0F, -4.0F, 20.0F},
            {0.50F, -7.0F, -22.0F, 0.0F, 0.0F},
    };

    private static float throwArmValue(float time, int component) {
        float[][] keys = THROW_ARM_KEYS;
        int last = keys.length - 1;
        if (time <= keys[0][0]) return keys[0][component];
        if (time >= keys[last][0]) return keys[last][component];
        int next = 1;
        while (next < keys.length && time > keys[next][0]) next++;
        float[] before = keys[Math.max(0, next - 2)];
        float[] from = keys[next - 1];
        float[] to = keys[next];
        float[] after = keys[Math.min(last, next + 1)];
        float progress = (time - from[0]) / (to[0] - from[0]);
        return catmull(before[component], from[component], to[component], after[component], progress);
    }

    private static float catmull(float a, float b, float c, float d, float t) {
        return 0.5F * ((2 * b) + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t * t
                + (-a + 3 * b - 3 * c + d) * t * t * t);
    }

    private static void startAnimation(String name, float blendSeconds) {
        long now = System.nanoTime();
        transitionFrom = blendSeconds > 0 ? currentPose(now) : null;
        animation = name;
        animationStartedNano = now;
        transitionSeconds = blendSeconds;
    }

    private static boolean isAnimatingExit() {
        return (animation.equals("throw") || animation.equals("put_away"))
                && animationSeconds(System.nanoTime()) < LunaGrenadeAnimation.duration(animation);
    }

    private static float animationSeconds(long now) {
        return animationStartedNano == 0 ? 0 : Math.max(0, (now - animationStartedNano) / 1_000_000_000F);
    }

    private static Map<String, Transform> currentPose(long now) {
        float time = animationSeconds(now);
        Map<String, Transform> pose;
        float fakeUnlockDuration = 0.2F;

        if (animation.equals("unlock_safe") && time >= fakeUnlockDuration) {
            float loopTime = time - fakeUnlockDuration;
            pose = LunaGrenadeAnimation.sample("unlock_safe_loop", loopTime, true);
            float blendDuration = 0.25F;
            if (loopTime < blendDuration) {
                float blend = loopTime / blendDuration;
                blend = blend * blend * (3 - 2 * blend);
                pose = LunaGrenadeAnimation.blend(
                        LunaGrenadeAnimation.sample("unlock_safe", fakeUnlockDuration, false), pose, blend);
            }
            float offsetRatio = Math.min(1.0F, loopTime / blendDuration);
            float smoothRatio = offsetRatio * offsetRatio * (3 - 2 * offsetRatio);
            float arcUp = (float) Math.sin(offsetRatio * Math.PI) * 1.2F;
            float yOffset = -2.0F * smoothRatio + arcUp;
            Transform rightHand = pose.getOrDefault("righthand_pos", LunaGrenadeAnimation.IDENTITY);
            pose.put("righthand_pos", new Transform(
                    new Vector(rightHand.position().x(), rightHand.position().y() + yOffset, rightHand.position().z()),
                    rightHand.rotation(), rightHand.scale()));
        } else if (animation.equals("unlock_safe_loop")) {
            pose = LunaGrenadeAnimation.sample("unlock_safe_loop", time, true);
            Transform rightHand = pose.getOrDefault("righthand_pos", LunaGrenadeAnimation.IDENTITY);
            pose.put("righthand_pos", new Transform(
                    new Vector(rightHand.position().x(), rightHand.position().y() - 2.0F, rightHand.position().z()),
                    rightHand.rotation(), rightHand.scale()));
        } else if (animation.equals("throw")) {
            pose = LunaGrenadeAnimation.sample("throw", Math.min(time * 2.0F, 0.25F), false);
            Transform leftHand = pose.getOrDefault("lefthand_pos", LunaGrenadeAnimation.IDENTITY);
            pose.put("lefthand_pos", new Transform(
                    new Vector(leftHand.position().x(), leftHand.position().y() - 3.5F, leftHand.position().z()),
                    leftHand.rotation(), leftHand.scale()));

            float armX = throwArmValue(time, 1);
            float armY = throwArmValue(time, 2);
            float armZ = throwArmValue(time, 3);
            float armPitch = throwArmValue(time, 4);
            pose.put("r", new Transform(new Vector(armX, armY, armZ),
                    new Vector(armPitch, 0.0F, 0.0F), new Vector(1.0F, 1.0F, 1.0F)));
        } else {
            pose = LunaGrenadeAnimation.sample(animation, time, false);
        }

        if (transitionFrom != null && transitionSeconds > 0 && time < transitionSeconds) {
            pose = LunaGrenadeAnimation.blend(transitionFrom, pose, time / transitionSeconds);
        }
        return pose;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRenderHand(RenderHandEvent event) {
        if (!isHeld() && !isAnimatingExit()) return;
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive()) return;
        LunaGrenadeModel.renderFirstPerson(event.getPoseStack(), event.getMultiBufferSource(),
                event.getPackedLight(), player, currentPose(System.nanoTime()));
    }

    @SubscribeEvent
    public static void onCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if ((!isHeld() && !isAnimatingExit()) || !minecraft.options.getCameraType().isFirstPerson()) return;
        var camera = currentPose(System.nanoTime()).getOrDefault("camera", LunaGrenadeAnimation.IDENTITY).rotation();
        event.setPitch(event.getPitch() + camera.x());
        event.setYaw(event.getYaw() + camera.y());
        event.setRoll(event.getRoll() + camera.z());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;
        if (!isPrimed()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int centerX = event.getWindow().getGuiScaledWidth() / 2;
        int centerY = event.getWindow().getGuiScaledHeight() / 2;
        double remaining = fuseSeconds();
        String timerStr = String.format(Locale.ROOT, "%.2fs", remaining);
        int color = remaining <= 1.0D ? 0xFFFF5757 : 0xFFFFFFFF;
        int iconWidth = 18, iconHeight = 19;
        int textWidth = (int) ( ModFonts.width(timerStr));
        int contentWidth = iconWidth + 4 + textWidth;
        float scale = 0.75F;
        float startX = centerX - contentWidth * scale / 2F;
        float startY = centerY + 18F;

        drawSafeIcon(graphics, COUNTDOWN_ICON, startX + 1, startY + 4f, 0.55f,
                16, 16, 0.0F, 0.0F, 16, 16, 16, 16);
        drawScaledString(graphics, font, timerStr,
                startX + (iconWidth + 4) * scale - 5,
                startY + (iconHeight - ModFonts.height()) / 2.0F * scale + 2f, scale, color);
    }

    private static void restoreFovEffect() {
        if (savedFovEffectScale != -1.0D) {
            Minecraft.getInstance().options.fovEffectScale().set(savedFovEffectScale);
            savedFovEffectScale = -1.0D;
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clearLocalState();
        restoreFovEffect();
    }
    private static void clearLocalState() {
        state = LunaGrenadeService.HeldState.NONE;
        fuseEndNano = 0L;
        charges = LunaGrenadeService.MAX_CHARGES;
        rechargeEndNano = 0L;
        lastSelectedSlot = -1;
        throwSent = false;
        primeSent = false;
        attackWasDown = false;
        requireAttackRelease = false;
        animation = "static_idle";
        animationStartedNano = 0L;
        transitionFrom = null;
        transitionSeconds = 0;
    }

    private static void drawSafeIcon(GuiGraphics g, ResourceLocation tex, float x, float y,
                                     float scale, int w, int h, float u, float v,
                                     int uWidth, int vHeight, int texWidth, int texHeight) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1F);
        g.blit(tex, 0, 0, w, h, u, v, uWidth, vHeight, texWidth, texHeight);
        g.pose().popPose();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawScaledString(GuiGraphics g, Font font, String text,
                                         float x, float y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        ModFonts.drawString(g, text, 0, 0, color, true);
        g.pose().popPose();
    }
    public static void stowNow() {
        if (state == LunaGrenadeService.HeldState.NONE || isPrimed()) return;
        startAnimation("put_away", 0.08F);
        state = LunaGrenadeService.HeldState.NONE;
        ModNetwork.CHANNEL.sendToServer(new LunaGrenadeActionPacket(LunaGrenadeActionPacket.Action.CANCEL));
        throwSent = false;
        primeSent = false;
        requireAttackRelease = true;
    }
}