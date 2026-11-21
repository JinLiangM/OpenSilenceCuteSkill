package lol.silencecute.client;

import lol.silencecute.Client;
import lol.silencecute.client.audio.DashSound;
import lol.silencecute.client.compat.TaczCompat;
import lol.silencecute.client.compat.YsmCompat;
import lol.silencecute.client.command.Keybinds;
import lol.silencecute.client.screen.fatality.FaClickGui;
import lol.silencecute.client.screen.fatality.GuiTransition;
import lol.silencecute.client.screen.fatality.NvClickGui;
import lol.silencecute.modules.ModuleManager;
import lol.silencecute.manager.DashService;
import lol.silencecute.manager.MagneticBombService;
import java.util.ArrayList;

import java.util.List;

import lol.silencecute.api.DashMode;

import lol.silencecute.api.Operator;

import lol.silencecute.config.ModConfigData;

import lol.silencecute.network.DashPacket;

import lol.silencecute.network.BombThrowPacket;

import com.mojang.blaze3d.platform.InputConstants;


import lol.silencecute.registry.ModNetwork;

import net.minecraft.client.KeyMapping;

import net.minecraft.client.Minecraft;

import net.minecraft.client.player.LocalPlayer;

import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.network.chat.Component;

import net.minecraft.util.RandomSource;

import net.minecraft.world.entity.Pose;

import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import net.minecraftforge.client.event.InputEvent;

import net.minecraftforge.client.settings.KeyConflictContext;

import net.minecraftforge.event.TickEvent;

import net.minecraftforge.eventbus.api.EventPriority;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)

public final class ClientEvents {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("silencecute_skill");

    public static final KeyMapping ULTIMATE_KEY = new KeyMapping(

            "key." + Client.MOD_ID + ".ultimate",

            KeyConflictContext.IN_GAME,

            InputConstants.Type.KEYSYM,

            GLFW.GLFW_KEY_X,

            "key." + Client.MOD_ID + ".category");

    public static final KeyMapping DASH_KEY = new KeyMapping(

            "key." + Client.MOD_ID + ".dash",

            KeyConflictContext.IN_GAME,

            InputConstants.Type.KEYSYM,

            GLFW.GLFW_KEY_V,

            "key." + Client.MOD_ID + ".category");

    public static final KeyMapping BOMB_KEY = new KeyMapping(

            "key." + Client.MOD_ID + ".magnetic_bomb",

            KeyConflictContext.IN_GAME,

            InputConstants.Type.KEYSYM,

            GLFW.GLFW_KEY_G,

            "key." + Client.MOD_ID + ".category");

    private static final RandomSource DASH_RANDOM = RandomSource.create();

    private static boolean guiKeyDown;

    private static boolean listeningBind;

    private static String bindTarget;

    private static double dashDirX;

    private static double dashDirZ;

    private static double dashStartY;

    private static float dashOriginalStepHeight;

    private static int dashTicks;

    private static boolean dashAwaitingFinalMove;

    private static boolean dashForcedSprint;

    private static boolean dashWasSprinting;

    private static boolean dashControlsLocked;

    private static boolean rollControlsLocked;

    private static boolean dashTaczAimSuspended;

    private static long ultCdEndNano;

    private static long dashCdEndNano;

    private static long passiveCdEndNano;

    private static int bombCharges = MagneticBombService.MAX_CHARGES;

    private static long bombRechargeEndNano;

    private static int operatorSyncTicks;

    private static boolean serverConfirmed;

    private static int serverCheckTicks;

    private static boolean serverMissingWarned;

    private ClientEvents() {

    }

    @SubscribeEvent

    public static void onLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        AccessClient.reset();

        dashTicks = 0;

        dashAwaitingFinalMove = false;

        dashForcedSprint = false;

        dashWasSprinting = false;

        dashControlsLocked = false;

        dashTaczAimSuspended = false;

        operatorSyncTicks = 0;

        serverConfirmed = false;

        serverCheckTicks = 0;

        serverMissingWarned = false;

        AccessClient.query();

    }

    @SubscribeEvent

    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {

        AccessClient.reset();

        C4ThrowClient.reset();

    }

    private static int autoTestTicks = -1;

    @SubscribeEvent

    public static void onOperatorSyncTick(TickEvent.ClientTickEvent event) {

        ModuleManager.get().enforceAll();

        Minecraft clickGuiMc = Minecraft.getInstance();
        if (clickGuiMc != null && clickGuiMc.getWindow() != null && ModConfigData.guiBind() > 0) {
            boolean guiKeyNow = InputConstants.isKeyDown(clickGuiMc.getWindow().getWindow(), ModConfigData.guiBind());
            if (guiKeyNow && !guiKeyDown && clickGuiMc.screen == null) {
                openClickGui();
            }
            guiKeyDown = guiKeyNow;
        }

        if (event.phase != TickEvent.Phase.END) {

            return;

        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {
            TempstUltAnchorClient.tick(minecraft.player);
            TempstUltClient.tickPending(minecraft);

            int c4Slot = C4ThrowClient.lockedSlot();
            if (c4Slot >= 0 && minecraft.player.getInventory().selected != c4Slot) {
                minecraft.player.getInventory().selected = c4Slot;
            }

            if (TempstUltClient.isCasting() || TempstUltPullLock.isLocked()) {
                minecraft.player.stopUsingItem();
                if (minecraft.options != null) {
                    minecraft.options.keyAttack.setDown(false);
                    minecraft.options.keyUse.setDown(false);
                }
                if (lockedHotbarSlot < 0) {
                    lockedHotbarSlot = minecraft.player.getInventory().selected;
                } else {
                    minecraft.player.getInventory().selected = lockedHotbarSlot;
                }
            } else {
                lockedHotbarSlot = -1;
            }
        }

        if (minecraft.player == null || minecraft.getConnection() == null) {

            operatorSyncTicks = 0;

            return;

        }

        if (!serverConfirmed && !minecraft.isSingleplayer() && !serverMissingWarned) {

            if (serverCheckTicks++ > 200) {

                serverMissingWarned = true;

                minecraft.player.displayClientMessage(Component.literal(

                        "§c[三角洲干员] 未检测到服务器端模组：手雷、C4 等技能伤害由服务器结算，"

                                + "请让服主在服务端 mods 里放入与本客户端相同版本的模组 jar。"), false);

            }

        }

    }

    public static void applySkillSync(long dashMillis, long passiveMillis, int syncedBombCharges, long bombRechargeMillis, long ultMillis) {

        serverConfirmed = true;

        long now = System.nanoTime();

        dashCdEndNano = now + dashMillis * 1_000_000L;

        passiveCdEndNano = now + passiveMillis * 1_000_000L;

        bombCharges = Math.max(0, Math.min(MagneticBombService.MAX_CHARGES, syncedBombCharges));

        bombRechargeEndNano = now + bombRechargeMillis * 1_000_000L;

        ultCdEndNano = now + ultMillis * 1_000_000L;

    }

    public static long ultimateCooldownMillis() {

        return Math.max(0L, (ultCdEndNano - System.nanoTime()) / 1_000_000L);

    }

    public static long cooldownMillis() {

        return Math.max(0L, (dashCdEndNano - System.nanoTime()) / 1_000_000L);

    }

    public static long passiveCooldownMillis() {

        return Math.max(0L, (passiveCdEndNano - System.nanoTime()) / 1_000_000L);

    }

    public static int bombCharges() {

        return bombCharges;

    }

    public static long bombRechargeMillis() {

        return Math.max(0L, (bombRechargeEndNano - System.nanoTime()) / 1_000_000L);

    }

    public static boolean isDashSprintPhase() {

        return isDashControlLockActive();

    }

    @SubscribeEvent

    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {

        if ((isDashControlLockActive() || TempstRollClient.isRolling() || TempstUltClient.isCasting() || TempstUltPullLock.isLocked())
                && (event.isAttack() || event.isUseItem())) {

            event.setSwingHand(false);

            event.setCanceled(true);

        }

    }

    private static boolean matchesGuiBind(InputEvent.Key event) {

        int key = ModConfigData.guiBind();

        int scan = ModConfigData.guiBindScan();

        if (key > 0 && event.getKey() == key) return true;

        return scan > 0 && Keybinds.sameScan(event.getScanCode(), scan);

    }
    @SubscribeEvent(priority = EventPriority.LOWEST)

    public static void onKeyInput(InputEvent.Key event) {

        if (!listeningBind && event.getAction() == GLFW.GLFW_PRESS && matchesGuiBind(event)) {

            Minecraft openMc = Minecraft.getInstance();

            if (openMc != null && openMc.screen == null) {

                openClickGui();

            }

        }

        if (listeningBind && event.getAction() == GLFW.GLFW_PRESS) {

            listeningBind = false;

            String target = bindTarget == null ? "ClickGui" : bindTarget;
            bindTarget = null;

            int pressed = event.getKey();
            int pressedScan = event.getScanCode();

            bindModuleKey(target, pressed, pressedScan);

            String label = keyLabel(pressed, pressedScan);

            chat(BIND_PREFIX + "Bound " + target + " to " + (label != null ? label : "key code " + pressed) + ".");

            return;

        }

        if (event.getAction() == GLFW.GLFW_PRESS) {

            handleBoundSkillPress();

        }

        keepTaczAimSuspendedAfterInput();

    }

    @SubscribeEvent(priority = EventPriority.LOWEST)

    public static void onMouseInput(InputEvent.MouseButton.Post event) {

        if (event.getAction() == GLFW.GLFW_PRESS) {

            handleBoundSkillPress();

        }

        keepTaczAimSuspendedAfterInput();

    }

    private static void keepTaczAimSuspendedAfterInput() {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null && (isDashControlLockActive() || TempstUltClient.isCasting() || TempstUltPullLock.isLocked())) {

            TaczCompat.keepAimSuspended(minecraft.player);

        }

    }

    private static void handleBoundSkillPress() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        if (minecraft.screen != null) return;

        if (!ModConfigData.operatorEnabled()) return;

        if (ULTIMATE_KEY.consumeClick()) {
            handleUltimateKey();
            return;
        }
        if (DASH_KEY.consumeClick()) {
            handleSecondSkillKey(minecraft);
            return;
        }
        if (BOMB_KEY.consumeClick()) {
            handleThirdSkillKey(minecraft);
        }
    }

    private static void handleUltimateKey() {

        if (ModConfigData.operator() == Operator.LUNA) {

            LunaReconArrowClient.toggleDrawn();

        } else if (ModConfigData.operator() == Operator.JIFENG) {

            TempstUltClient.tryCast(Minecraft.getInstance());

        } else if (ModConfigData.operator() == Operator.WEILONG) {

        }

    }

    private static void handleSecondSkillKey(Minecraft minecraft) {

        Operator operator = ModConfigData.operator();

        if (operator == Operator.WEILONG) {

            tryDash(minecraft);

        } else if (operator == Operator.LUNA) {

            LunaShockArrowClient.toggleDrawn();

        } else if (operator == Operator.JIFENG) {

            TempstRollClient.tryRoll(minecraft);

        }

    }

    private static void handleThirdSkillKey(Minecraft minecraft) {

        LocalPlayer player = minecraft.player;

        if (player == null) return;

        Operator operator = ModConfigData.operator();

        if (operator == Operator.LUNA) {

            LunaShockArrowClient.stowNow();

            LunaReconArrowClient.stowNow();

            LunaGrenadeClient.toggleEquipped();

            return;

        }

        if (operator != Operator.WEILONG) return;
        if (C4ThrowClient.throwLocked()) return;
        if (bombCharges <= 0) {
            playSkillUnavailableSound();
            return;
        }

        throwBomb();

    }

    private static void throwBomb() {

        ModNetwork.CHANNEL.sendToServer(new BombThrowPacket());

        C4ThrowClient.begin();

        bombCharges--;

        if (bombCharges <= 0 && bombRechargeMillis() <= 0L) {

            bombRechargeEndNano = System.nanoTime()

                    + MagneticBombService.RECHARGE_TICKS * 50_000_000L;

        }

    }

    @SubscribeEvent
    public static void onScrollCasting(InputEvent.MouseScrollingEvent event) {
        if ((TempstUltClient.isCasting() || TempstUltPullLock.isLocked()
                || C4ThrowClient.isPlaying()) && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    public static int lockedHotbarSlot = -1;

    @SubscribeEvent
    public static void onHotbarKeyCasting(InputEvent.Key event) {
        if (!TempstUltClient.isCasting() && !TempstUltPullLock.isLocked()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options == null) return;
        for (KeyMapping mapping : minecraft.options.keyHotbarSlots) {
            if (mapping.matches(event.getKey(), event.getScanCode())) {
                return;
            }
        }
    }
    public static void playSkillUnavailableSound() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        player.playSound(lol.silencecute.registry.ModSounds.CANT_USE_SKILL.get(), 1.35F, 1.0F);
    }

    private static void tryDash(Minecraft minecraft) {

        LocalPlayer player = minecraft.player;

        if (player == null || player.isPassenger()) {
            LOGGER.info("[Dash] 拒绝：无玩家或在载具上");
            return;
        }

        if (ModConfigData.operator() != Operator.WEILONG) {
            LOGGER.info("[Dash] 拒绝：当前干员不是威龙（当前={}）", ModConfigData.operator());
            return;
        }

        if (player.isInWater() || player.isSwimming()) {

            playSkillUnavailableSound();
            ClientNotice.warn(lol.silencecute.client.lang.Lang.tr("notif.skill.no_water"));

            return;

        }

        if (player.getPose() == Pose.SWIMMING || player.isVisuallyCrawling()) {
            LOGGER.info("[Dash] 拒绝: swimming/crawling pose={}", player.getPose());
            return;
        }

        if (!conflicts(minecraft).isEmpty()) {
            LOGGER.info("[Dash] 拒绝：按键与其他绑定冲突 {}", conflicts(minecraft));
            return;
        }

        if (cooldownMillis() > 0L) {
            LOGGER.info("[Dash] 拒绝：冷却中 {}ms", cooldownMillis());
            playSkillUnavailableSound();
            return;
        }

        Vec3 direction = dashDirection(player, ModConfigData.dashMode());

        double length = Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        if (Double.isFinite(length) && length >= 0.1D) {

            dashDirX = direction.x / length;

            dashDirZ = direction.z / length;

            dashStartY = player.getY();

            dashOriginalStepHeight = player.maxUpStep();

            dashTicks = DashService.DASH_TICKS;

            dashAwaitingFinalMove = false;

            dashWasSprinting = player.isSprinting();

            dashControlsLocked = false;

            dashTaczAimSuspended = TaczCompat.suspendAim(player);

            player.setDeltaMovement(Vec3.ZERO);

            player.fallDistance = 0.0F;

            updateDashControlLock(minecraft, player);

            dashCdEndNano = System.nanoTime() + DashService.COOLDOWN_TICKS * 50_000_000L;

            DashSound.playForLocalPlayer(minecraft, player.getX(), player.getY() + 1.2D, player.getZ());

        }

        ModNetwork.CHANNEL.sendToServer(new DashPacket((float) direction.x, (float) direction.z));

    }

    @SubscribeEvent

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();

        if (event.player != minecraft.player || minecraft.level == null) return;

        LocalPlayer player = minecraft.player;

        TempstRollClient.tick(minecraft);

        updateDashControlLock(minecraft, player);

        if (dashTicks <= 0) {

            if (dashAwaitingFinalMove) {

                Vec3 movement = player.getDeltaMovement();

                player.setDeltaMovement(movement.x, 0.0D, movement.z);

                player.setMaxUpStep(dashOriginalStepHeight);

                dashAwaitingFinalMove = false;

                if (player.isAlive()) forceStandAfterDash(minecraft, player);

            }

            return;

        }

        int tickIndex = DashService.DASH_TICKS - dashTicks;

        double step = DashService.stepForTick(tickIndex);

        double stepX = dashDirX * step;

        double stepZ = dashDirZ * step;

        boolean stepAhead = DashService.hasDashStepAhead(player, stepX, stepZ);

        player.setMaxUpStep(stepAhead ? DashService.DASH_STEP_HEIGHT : dashOriginalStepHeight);

        if (stepAhead) player.setOnGround(true);

        if (!player.isAlive() || player.horizontalCollision && !stepAhead) {

            dashTicks = 0;

            dashAwaitingFinalMove = false;

            updateDashControlLock(minecraft, player);

            player.setMaxUpStep(dashOriginalStepHeight);

            if (player.isAlive()) forceStandAfterDash(minecraft, player);

            return;

        }

        double targetY = dashStartY + DashService.riseAfterTick(tickIndex);

        player.setDeltaMovement(stepX, Math.max(0.0D, targetY - player.getY()), stepZ);

        player.fallDistance = 0.0F;

        for (int i = 0; i < 4; i++) {

            minecraft.level.addParticle(ParticleTypes.CRIT,

                    player.getX() + (DASH_RANDOM.nextDouble() - 0.5D) * 0.56D,

                    player.getY() + 0.45D + DASH_RANDOM.nextDouble() * 0.7D,

                    player.getZ() + (DASH_RANDOM.nextDouble() - 0.5D) * 0.56D,

                    -dashDirX * 0.06D, 0.02D, -dashDirZ * 0.06D);

        }

        if (DASH_RANDOM.nextFloat() < 0.6F) {

            minecraft.level.addParticle(ParticleTypes.END_ROD,

                    player.getX() + (DASH_RANDOM.nextDouble() - 0.5D) * 0.36D,

                    player.getY() + 0.5D + DASH_RANDOM.nextDouble() * 0.8D,

                    player.getZ() + (DASH_RANDOM.nextDouble() - 0.5D) * 0.36D,

                    0.0D, 0.0D, 0.0D);

        }

        dashTicks--;

        if (dashTicks == 0) dashAwaitingFinalMove = true;

    }

    private static boolean isDashControlLockActive() {

        return dashTicks > DashService.DASH_TICKS - DashService.CONTROL_LOCK_TICKS;

    }

    public static boolean isDashing() {
        return dashTicks > 0;
    }

    private static void updateDashControlLock(Minecraft minecraft, LocalPlayer player) {

        if (TempstRollClient.isRolling()) {

            player.setSprinting(true);

            TaczCompat.keepAimSuspended(player);

            minecraft.options.keyAttack.setDown(false);

            minecraft.options.keyUse.setDown(false);

            if (!rollControlsLocked) {

                if (minecraft.gameMode != null) {

                    minecraft.gameMode.stopDestroyBlock();

                    if (player.isUsingItem()) minecraft.gameMode.releaseUsingItem(player);

                }

                player.stopUsingItem();

                rollControlsLocked = true;

            }

            return;

        }

        if (rollControlsLocked) {

            rollControlsLocked = false;

            restorePhysicalKeyState(minecraft, minecraft.options.keyAttack);

            restorePhysicalKeyState(minecraft, minecraft.options.keyUse);

        }



        if (isDashControlLockActive()) {

            player.setSprinting(true);

            dashForcedSprint = true;

            TaczCompat.keepAimSuspended(player);

            minecraft.options.keyAttack.setDown(false);

            minecraft.options.keyUse.setDown(false);

            if (!dashControlsLocked) {

                if (minecraft.gameMode != null) {

                    minecraft.gameMode.stopDestroyBlock();

                    if (player.isUsingItem()) minecraft.gameMode.releaseUsingItem(player);

                }

                player.stopUsingItem();

                dashControlsLocked = true;

            }

            return;

        }

        if (dashControlsLocked) {

            dashControlsLocked = false;

            restorePhysicalKeyState(minecraft, minecraft.options.keyAttack);

            restorePhysicalKeyState(minecraft, minecraft.options.keyUse);

        }

        if (dashTaczAimSuspended) {

            dashTaczAimSuspended = false;

            if (player.isAlive()) TaczCompat.restoreAim(minecraft, player);

        }

        if (dashForcedSprint) {

            dashForcedSprint = false;

            player.setSprinting(dashWasSprinting || minecraft.options.keySprint.isDown());

        }

    }

    private static void restorePhysicalKeyState(Minecraft minecraft, KeyMapping mapping) {

        InputConstants.Key key = mapping.getKey();

        long window = minecraft.getWindow().getWindow();

        boolean down = key.getType() == InputConstants.Type.MOUSE

                ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS

                : key.getType() == InputConstants.Type.KEYSYM && InputConstants.isKeyDown(window, key.getValue());

        mapping.setDown(down);

        if (down && mapping == minecraft.options.keyUse) {

            try {

                KeyMapping.click(key);

            } catch (Throwable ignored) {

            }

        }

    }

    private static void forceStandAfterDash(Minecraft minecraft, LocalPlayer player) {

        minecraft.options.keyShift.setDown(false);

        player.input.shiftKeyDown = false;

        player.setShiftKeyDown(false);

        player.setPose(Pose.STANDING);

        player.refreshDimensions();

    }

    static Vec3 dashDirection(LocalPlayer player, DashMode mode) {

        Vec3 look = player.getLookAngle();

        double forwardX = look.x;

        double forwardZ = look.z;

        double forwardLength = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);

        if (forwardLength < 1.0E-4D) {

            forwardX = 0.0D;

            forwardZ = 1.0D;

        } else {

            forwardX /= forwardLength;

            forwardZ /= forwardLength;

        }

        double rightX = -forwardZ;

        double rightZ = forwardX;

        if (mode == DashMode.FOUR_WAY) {

            if (player.input.up) return new Vec3(forwardX, 0.0D, forwardZ);

            if (player.input.down) return new Vec3(-forwardX, 0.0D, -forwardZ);

            if (player.input.left) return new Vec3(-rightX, 0.0D, -rightZ);

            if (player.input.right) return new Vec3(rightX, 0.0D, rightZ);

            return new Vec3(forwardX, 0.0D, forwardZ);

        }

        double forwardImpulse = player.input.forwardImpulse;

        double strafeImpulse = player.input.leftImpulse;

        double x = forwardX * forwardImpulse + rightX * strafeImpulse;

        double z = forwardZ * forwardImpulse + rightZ * strafeImpulse;

        if (Math.abs(x) + Math.abs(z) < 0.1D) return new Vec3(forwardX, 0.0D, forwardZ);

        return new Vec3(x, 0.0D, z);

    }

    static List<KeyMapping> conflicts(Minecraft minecraft) {

        List<KeyMapping> result = new ArrayList<>();

        for (KeyMapping other : minecraft.options.keyMappings) {

            if (other != DASH_KEY && !other.isUnbound() && DASH_KEY.same(other)) {

                result.add(other);

            }

        }

        return result;

    }

    public static void openClickGui() {
        openClickGui(null);
    }

    public static void openClickGui(lol.silencecute.modules.Category target) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.screen != null) return;

        GuiTransition.reset();
        boolean naven = "NAVEN".equalsIgnoreCase(ModConfigData.guiStyle());
        if (naven) {
            NvClickGui.requestCategory(target);
            minecraft.setScreen(new NvClickGui(null));
        } else {
            FaClickGui.requestCategory(target);
            minecraft.setScreen(new FaClickGui(null));
        }
    }

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {

        String message = event.getMessage().trim();

        if (!message.startsWith(".")) return;

        event.setCanceled(true);

        String[] parts = message.substring(1).trim().split("\\s+");

        if (parts.length == 0 || parts[0].isEmpty()) return;

        if (!parts[0].equalsIgnoreCase("bind") && !parts[0].equalsIgnoreCase("b")) {

            chat("§c未知指令: ." + parts[0]);

            return;

        }

        if (parts.length == 2) {

            String moduleName = parts[1];

            if (!isBindableModule(moduleName)) {

                chat(BIND_PREFIX + "Invalid module.");

                return;

            }

            bindTarget = moduleName;
            listeningBind = true;

            chat(BIND_PREFIX + "Press a key to bind " + moduleName + " to.");

            return;

        }

        if (parts.length >= 3) {

            String moduleName = parts[1];
            String keyName = parts[2];

            if (!isBindableModule(moduleName)) {

                chat(BIND_PREFIX + "Invalid module.");

                return;

            }

            if (keyName.equalsIgnoreCase("none")) {

                bindModuleKey(moduleName, 0, -1);

                chat(BIND_PREFIX + "Unbound " + moduleName + ".");

                return;

            }

            int code = resolveKey(keyName);

            if (code <= 0) {

                chat(BIND_PREFIX + "Invalid key.");

                return;

            }

            bindModuleKey(moduleName, code, -1);

            chat(BIND_PREFIX + "Bound " + moduleName + " to " + keyName.toUpperCase(java.util.Locale.ROOT) + ".");

            return;

        }

        chat(BIND_PREFIX + "Usage: .bind <module> [key]");

    }

    private static final String BIND_PREFIX = "§7[§bS§7] ";

    private static boolean isBindableModule(String name) {

        return name != null
                && (name.equalsIgnoreCase("ClickGui")
                || name.equalsIgnoreCase("gui")
                || name.equalsIgnoreCase("click_gui"));

    }

    private static void bindModuleKey(String module, int key, int scan) {

        ModConfigData.setGuiBind(key);

        ModConfigData.setGuiBindScan(scan > 0 ? scan : Keybinds.scanFromKey(key));

    }

    private static int resolveKey(String name) {

        try {

            InputConstants.Key key = InputConstants.getKey("key.keyboard." + name.toLowerCase(java.util.Locale.ROOT));

            if (key != null && key != InputConstants.UNKNOWN && key.getValue() > 0) return key.getValue();

        } catch (Exception ignored) {

        }

        return Keybinds.parse(name);

    }

    private static String keyLabel(int key, int scan) {

        int code = key > 0 ? key : Keybinds.keyFromScan(scan);

        if (code <= 0) return null;

        try {

            InputConstants.Key mapped = InputConstants.getKey(code, 0);

            if (mapped != null && mapped != InputConstants.UNKNOWN) {

                String display = mapped.getDisplayName().getString().toUpperCase(java.util.Locale.ROOT);

                if (!display.isEmpty() && !display.contains("UNKNOWN")) return display;

            }

        } catch (Exception ignored) {

        }

        String fallback = Keybinds.name(code);

        return "未绑定".equals(fallback) ? null : fallback;

    }

    private static void chat(String message) {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft != null && minecraft.player != null) {

            minecraft.player.displayClientMessage(Component.literal(message), false);

        }

    }
}