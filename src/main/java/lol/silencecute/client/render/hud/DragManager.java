package lol.silencecute.client.render.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lol.silencecute.Client;
import lol.silencecute.client.anim.UiAnimation.DecelerateAnimation;
import lol.silencecute.client.anim.UiAnimation.Direction;
import lol.silencecute.client.screen.fatality.FaWidgets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import lol.silencecute.client.screen.HudEditScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import lol.silencecute.client.font.ModFonts;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DragManager {

    private enum Handle {
        NONE, NW, N, NE, E, SE, S, SW, W
    }

    private static final int HANDLE_HIT_RADIUS = 7;

    private static final int HANDLE_DOT = 4;

    private static final int RESET_W = 56;
    private static final int RESET_H = 13;
    private static final int RESET_Y = 4;
    private static final String TEXT_RESET = "重置";

    private static final float LABEL_TEXT_SCALE = 0.75F;

    private static final int LABEL_OFFSET_X = 0;
    private static final int LABEL_OFFSET_Y = 1;

    private static final DecelerateAnimation labelAnim =
            new DecelerateAnimation(150, 1.0, Direction.BACKWARDS);

    private static final DecelerateAnimation resetAnim =
            new DecelerateAnimation(180, 1.0, Direction.BACKWARDS);

    private static final Map<HudWidget, Float> handleHover = new HashMap<>();

    private static final Map<HudWidget, Long> handleLastNanos = new HashMap<>();

    private static boolean selecting;
    private static float selStartX, selStartY, selEndX, selEndY;

    private static boolean resetPressed;

    private static boolean editingScreen;

    private static final List<HudWidget> WIDGETS = new ArrayList<>(4);

    public static void resetAll(int screenW, int screenH) {
        for (HudWidget widget : WIDGETS) widget.reset(screenW, screenH);
        HudLayout.markDirty();
        HudLayout.saveIfDirty();
    }

    private static HudWidget active;

    private static float snapLineY = -1.0F;

    private static float snapLineX = -1.0F;

    private static final float SNAP_Y = 6.0F;
    private static final float SNAP_X = 6.0F;

    private static final float SNAP_RELEASE = 2.0F;
    private static boolean snapYActive;
    private static boolean snapXActive;
    private static Handle activeHandle = Handle.NONE;

    private static float startMouseX;
    private static float startMouseY;
    private static float startX;
    private static float startY;
    private static float startW;
    private static float startH;

    private DragManager() {}

    public static void register(HudWidget widget) {
        if (widget == null) return;
        if (!WIDGETS.contains(widget)) WIDGETS.add(widget);
    }

    public static boolean isInteracting() {
        return active != null;
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isEditing(event.getScreen())) return;

        int screenW = screenWidth();
        int screenH = screenHeight();
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        if (event.getButton() == 1) {
            selecting = true;
            selStartX = selEndX = (float) mouseX;
            selStartY = selEndY = (float) mouseY;
            event.setCanceled(true);
            return;
        }
        if (event.getButton() != 0) return;

        for (int i = WIDGETS.size() - 1; i >= 0; i--) {
            HudWidget widget = WIDGETS.get(i);
            if (!widget.isVisible()) continue;
            Handle handle = handleAt(widget, mouseX, mouseY);

            if (handle != Handle.SE) handle = Handle.NONE;
            if (handle != Handle.NONE) {
                begin(widget, handle, mouseX, mouseY);
                event.setCanceled(true);
                return;
            }
        }

        for (int i = WIDGETS.size() - 1; i >= 0; i--) {
            HudWidget widget = WIDGETS.get(i);
            if (!widget.isVisible()) continue;
            if (widget.contains(mouseX, mouseY)) {
                begin(widget, Handle.NONE, mouseX, mouseY);
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (selecting) {
            selEndX = (float) event.getMouseX();
            selEndY = (float) event.getMouseY();
            event.setCanceled(true);
            return;
        }
        if (active == null || event.getMouseButton() != 0) return;
        if (!isEditing(event.getScreen())) {
            end();
            return;
        }
        update((float) event.getMouseX(), (float) event.getMouseY(), screenWidth(), screenHeight());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (selecting) {
            selecting = false;
            event.setCanceled(true);
            return;
        }
        resetPressed = false;
        if (event.getButton() != 0 || active == null) return;
        end();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (isEditing(event.getScreen())) end();
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        end();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        HudLayout.tickAutosave();
    }

    private static void begin(HudWidget widget, Handle handle, double mouseX, double mouseY) {
        active = widget;
        snapYActive = false;
        snapXActive = false;
        activeHandle = handle;
        startMouseX = (float) mouseX;
        startMouseY = (float) mouseY;
        startX = widget.x();
        startY = widget.y();
        startW = widget.width();
        startH = widget.height();
    }

    public static void end() {
        if (active != null) active.persist();
        active = null;
        activeHandle = Handle.NONE;
        snapLineY = -1.0F;
        snapLineX = -1.0F;
        HudLayout.saveIfDirty();
    }

    private static void update(float mouseX, float mouseY, int screenW, int screenH) {
        if (active == null) return;

        if (activeHandle == Handle.NONE) {

            float rawX = startX + (mouseX - startMouseX);
            float rawY = startY + (mouseY - startMouseY);

            float screenCenterY = screenH * 0.5F;
            float centerY = rawY + active.height() * 0.5F;
            float deltaY = screenCenterY - centerY;

            if (Math.abs(deltaY) <= SNAP_Y) {
                rawY += deltaY;
                snapLineY = screenCenterY;
            } else {
                snapLineY = -1.0F;
            }

            float screenCenterX = screenW * 0.5F;
            float centerX = rawX + active.width() * 0.5F;
            float deltaX = screenCenterX - centerX;

            if (Math.abs(deltaX) <= SNAP_X) {
                rawX += deltaX;
                snapLineX = screenCenterX;
            } else {
                snapLineX = -1.0F;
            }

                        active.setPosition(rawX, rawY, screenW, screenH);
            return;
        }

        float dx = mouseX - startMouseX;
        float dy = mouseY - startMouseY;
        float min = active.minSize();
        float max = active.maxSize();

        float newX = startX;
        float newY = startY;
        float newW = startW;
        float newH = startH;

        switch (activeHandle) {
            case NW -> {
                newW = Mth.clamp(startW - dx, min, max);
                newH = Mth.clamp(startH - dy, min, max);
                newX = startX + (startW - newW);
                newY = startY + (startH - newH);
            }
            case NE -> {

                float baseW = Math.max(1.0f, startW);
                float baseH = Math.max(1.0f, startH);
                float kMin = Math.max(0.05f, Math.max(min / baseW, min / baseH));
                float kMax = Math.max(0.05f, Math.min(max / baseW, max / baseH));
                float k = Mth.clamp((startW + dx) / baseW, kMin, kMax);
                newW = baseW * k;
                newH = baseH * k;
                newX = startX;
                newY = startY;
            }
            case SW -> {
                newW = Mth.clamp(startW - dx, min, max);
                newH = Mth.clamp(startH + dy, min, max);
                newX = startX + (startW - newW);
            }
            case SE -> {

                float baseW = Math.max(1.0f, startW);
                float baseH = Math.max(1.0f, startH);
                float kMin = Math.max(0.05f, Math.max(min / baseW, min / baseH));
                float kMax = Math.max(0.05f, Math.min(max / baseW, max / baseH));
                float k = Mth.clamp((startW + dx) / baseW, kMin, kMax);
                newW = baseW * k;
                newH = baseH * k;
                newX = startX;
                newY = startY;
            }
            case N -> {
                newH = Mth.clamp(startH - dy, min, max);
                newY = startY + (startH - newH);
            }
            case S -> newH = Mth.clamp(startH + dy, min, max);
            case W -> {
                newW = Mth.clamp(startW - dx, min, max);
                newX = startX + (startW - newW);
            }
            case E -> newW = Mth.clamp(startW + dx, min, max);
            default -> {
            }
        }

        active.setBounds(newX, newY, newW, newH, screenW, screenH);
    }

    public static void renderWidgetDebug(GuiGraphics graphics, HudWidget widget,
                                         double mouseX, double mouseY) {
        if (widget == null || !widget.isVisible()) return;

        if (active == widget) {
            if (snapLineY >= 0.0F) {
                int lineY = (int) snapLineY;
                graphics.fill(0, lineY, graphics.guiWidth(), lineY + 1, 0x99FFFFFF);
            }
            if (snapLineX >= 0.0F) {
                int lineX = (int) snapLineX;
                graphics.fill(lineX, 0, lineX + 1, graphics.guiHeight(), 0x99FFFFFF);
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        updateLabelAnimation(minecraft);

        drawLabel(graphics, minecraft, widget);

        if (!editingScreen) return;

        int pad = Mth.ceil(widget.debugPadding());
        int boxX = widget.intX() - pad;
        int boxY = widget.intY() - pad;
        int boxW = widget.intWidth() + pad * 2;
        int boxH = widget.intHeight() + pad * 2;

        Handle hovered = active == widget ? activeHandle : handleAt(widget, mouseX, mouseY);
        if (hovered != Handle.SE) hovered = Handle.NONE;
        boolean busy = active == widget;

        boolean over = widget.contains(mouseX, mouseY);
        if (!over) {
            float hx = widget.x() + widget.width();
            float hy = widget.y() + widget.height();
            over = Math.abs(mouseX - hx) <= HANDLE_HIT_RADIUS && Math.abs(mouseY - hy) <= HANDLE_HIT_RADIUS;
        }

        if (busy || hovered != Handle.NONE || over) {
            graphics.renderOutline(boxX, boxY, boxW, boxH, busy ? 0xE0FFFFFF : 0x80FFFFFF);
        }

        float handleOut = smoothHandle(widget, busy || over ? 1.0F : 0.0F);
        if (handleOut > 0.002F) {
            int handleX = boxX + boxW;
            int handleY = boxY + boxH;
            float half = HANDLE_DOT * (0.55F + 0.45F * handleOut);
            int alpha = (int) Math.round((hovered == Handle.SE ? 200 : 165) * handleOut);
            int color = (alpha << 24) | 0x008080;

            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(handleX, handleY, 0.0F);
            pose.scale(half, half, 1.0F);
            graphics.fill(-1, -1, 1, 1, color);
            pose.popPose();
        }
    }

    private static float smoothHandle(HudWidget widget, float target) {
        long now = System.nanoTime();
        Long last = handleLastNanos.put(widget, now);
        float dt = last == null ? 0.016F : (now - last) / 1_000_000_000.0F;
        dt = Mth.clamp(dt, 0.0F, 0.1F);
        float current = handleHover.getOrDefault(widget, 0.0F);
        float k = 1.0F - (float) Math.exp(-dt * 14.0F);
        current += (target - current) * k;
        if (Math.abs(target - current) < 0.001F) current = target;
        handleHover.put(widget, current);
        return current;
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        updateLabelAnimation(minecraft);

        float anim = (float) resetAnim.getOutput();
        if (!editingScreen && anim <= 0.01f) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = event.getWindow().getGuiScaledWidth();

        if (selecting) {
            int x1 = (int) Math.min(selStartX, selEndX);
            int y1 = (int) Math.min(selStartY, selEndY);
            int x2 = (int) Math.max(selStartX, selEndX);
            int y2 = (int) Math.max(selStartY, selEndY);
            graphics.fill(x1, y1, x2, y2, 0x4000A0FF);
            graphics.renderOutline(x1, y1, x2 - x1, y2 - y1, 0xFF00A0FF);
        }
    }

    private static void updateLabelAnimation(Minecraft minecraft) {
        boolean editing = isEditing(minecraft.screen);
        if (editing == editingScreen) return;
        editingScreen = editing;
        labelAnim.setDirection(editing ? Direction.FORWARDS : Direction.BACKWARDS);
        labelAnim.timerUtil.setTime(0L);

        resetAnim.setDirection(editing ? Direction.FORWARDS : Direction.BACKWARDS);
        resetAnim.timerUtil.setTime(0L);
        if (!editing) resetPressed = false;
    }

    private static void drawLabel(GuiGraphics graphics, Minecraft minecraft, HudWidget widget) {
        String text = widget.label();
        if (text == null || text.isEmpty()) return;

        float progress = (float) labelAnim.getOutput();
        if (progress <= 0.005F) return;

        Font font = minecraft.font;
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();

        int textW = (int) (Mth.ceil( ModFonts.width(text) * LABEL_TEXT_SCALE));
        int textH = Mth.ceil(ModFonts.height() * LABEL_TEXT_SCALE);

        int textX = widget.intX() + LABEL_OFFSET_X;
        int textY = widget.intY() - textH - LABEL_OFFSET_Y;
        if (textY < 0) {
            textY = widget.intY() + LABEL_OFFSET_Y;
        }
        textX = Mth.clamp(textX, 0, Math.max(0, screenW - textW));
        textY = Mth.clamp(textY, 0, Math.max(0, screenH - textH));

        int alpha = (int) (progress * 255.0F);
        float scale = 0.2F + progress * 0.8F;

        float centerX = textX + textW / 2.0F;
        float centerY = textY + textH / 2.0F;

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0F);

        HudDraw.drawScaledString(graphics, font, text, textX, textY,
                LABEL_TEXT_SCALE, (alpha << 24) | 0xFFFFFF, true);

        graphics.pose().popPose();
    }

    private static boolean isEditing(Screen screen) {

        return screen instanceof HudEditScreen;
    }

    public static boolean isEditingNow() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && isEditing(minecraft.screen);
    }

    private static boolean isResetButton(double mouseX, double mouseY, int screenW) {
        int boxX = screenW / 2 - RESET_W / 2;
        int boxY = RESET_Y;
        return mouseX >= boxX && mouseX <= boxX + RESET_W
                && mouseY >= boxY && mouseY <= boxY + RESET_H;
    }

    private static Handle handleAt(HudWidget widget, double mouseX, double mouseY) {
        float x = widget.x();
        float y = widget.y();
        float w = widget.width();
        float h = widget.height();
        int radius = HANDLE_HIT_RADIUS;

        boolean insideX = mouseX >= x - radius && mouseX <= x + w + radius;
        boolean insideY = mouseY >= y - radius && mouseY <= y + h + radius;
        if (!insideX || !insideY) return Handle.NONE;

        boolean left = Math.abs(mouseX - x) <= radius;
        boolean right = Math.abs(mouseX - (x + w)) <= radius;
        boolean top = Math.abs(mouseY - y) <= radius;
        boolean bottom = Math.abs(mouseY - (y + h)) <= radius;

        if (left && top) return Handle.NW;
        if (right && top) return Handle.NE;
        if (left && bottom) return Handle.SW;
        if (right && bottom) return Handle.SE;
        if (top) return Handle.N;
        if (bottom) return Handle.S;
        if (left) return Handle.W;
        if (right) return Handle.E;
        return Handle.NONE;
    }

    private static int screenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    private static int screenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }
}
