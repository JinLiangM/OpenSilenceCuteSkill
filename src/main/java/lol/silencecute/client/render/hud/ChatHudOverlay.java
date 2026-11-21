package lol.silencecute.client.render.hud;

import java.util.ArrayList;
import java.util.List;

import lol.silencecute.Client;
import lol.silencecute.client.font.ModFonts;
import lol.silencecute.client.screen.HudEditScreen;
import lol.silencecute.client.screen.SilenceChatScreen;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChatHudOverlay {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final int BASE_W = 150;
    private static final int BASE_H = 80;

    private static final int LINE_H = 16;

    private static final int PAD_X = 3;

    private static final float MIN_SIZE = 40.0F;
    private static final float MAX_SIZE = 800.0F;

    private static final int MAX_KEEP = 200;

    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final int TEXT_OLD_ALPHA = 0xB0;

    private static final List<String> MESSAGES = new ArrayList<>();

    private static long lastMessageAt;

    private static final List<String> TEST_LINES =
            List.of("测试1", "测试2", "测试3", "测试4", "测试5");

    private static final HudWidget WIDGET = new HudWidget("chat", "聊天框", BASE_W, BASE_H,
            MIN_SIZE, MAX_SIZE,
            (screenW, screenH) -> new float[]{8.0F, screenH - BASE_H - 40.0F});

    static {
        DragManager.register(WIDGET);
    }

    private ChatHudOverlay() {
    }

    public static HudWidget widget() {
        return WIDGET;
    }

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        try {
            if (event.getMessage() == null) return;
            MESSAGES.add(event.getMessage().getString());
            lastMessageAt = System.currentTimeMillis();
            while (MESSAGES.size() > MAX_KEEP) {
                MESSAGES.remove(0);
            }
        } catch (Throwable ignored) {

        }
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (MC.player == null || MC.options.hideGui) return;

        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();
        WIDGET.ensureInitialized(screenW, screenH);

        migrateChatVisibilityOnce();
        migrateChatSizeOnce();

        if (!ModConfigData.hudChat()) {
            restoreVanillaChatHeight();
            WIDGET.setVisible(false);
            return;
        }

        hideVanillaChatHeight();

        boolean editing = MC.screen instanceof HudEditScreen;
        boolean chatOpen = MC.screen instanceof SilenceChatScreen;

        if (!(editing || chatOpen || !isExpired())) {
            WIDGET.setVisible(false);
            return;
        }

        WIDGET.setVisible(true);

        GuiGraphics graphics = event.getGuiGraphics();
        draw(graphics, editing);

        DragManager.renderWidgetDebug(graphics, WIDGET,
                HudDraw.mouseX(MC), HudDraw.mouseY(MC));
    }

    private static boolean isExpired() {
        if (lastMessageAt == 0L) return true;
        return System.currentTimeMillis() - lastMessageAt > ModConfigData.hudChatFadeTime() * 1000L;
    }

    private static void draw(GuiGraphics graphics, boolean editing) {
        List<String> visible = editing ? TEST_LINES : collect();
        if (visible.isEmpty()) return;

        WIDGET.setBaseHeightKeepScale(visible.size() * (float) LINE_H);

        HudDraw.pushWidget(graphics, WIDGET);

        int total = visible.size();
        for (int i = 0; i < total; i++) {
            boolean oldest = i == 0 && total > 1;
            int a = oldest ? TEXT_OLD_ALPHA : 0xFF;
            int color = (a << 24) | (TEXT_COLOR & 0x00FFFFFF);

            ModFonts.drawStringSmooth(graphics, visible.get(i), PAD_X, LINE_H * i + 1.0F, color, false);
        }

        HudDraw.popWidget(graphics);
    }

    private static List<String> collect() {
        int maxMessages = ModConfigData.hudChatMaxMessages();
        int width = BASE_W - PAD_X * 2;
        List<String> result = new ArrayList<>();

        int taken = 0;
        for (int i = MESSAGES.size() - 1; i >= 0 && taken < maxMessages; i--) {
            taken++;
            List<String> wrapped = wrap(MESSAGES.get(i), width);
            for (int j = wrapped.size() - 1; j >= 0; j--) {
                result.add(0, wrapped.get(j));
            }
        }
        return result;
    }

    private static List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                lines.add(line.toString());
                line.setLength(0);
                continue;
            }
            line.append(c);
            if (ModFonts.smoothWidth(line.toString()) > maxWidth) {
                line.setLength(line.length() - 1);
                lines.add(line.toString());
                line.setLength(0);
                line.append(c);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private static Double savedChatHeightFocused;
    private static Double savedChatHeightUnfocused;
    private static Double savedChatScale;
    private static Double savedChatOpacity;

    private static void hideVanillaChatHeight() {
        try {
            if (savedChatHeightFocused == null) {
                savedChatHeightFocused = MC.options.chatHeightFocused().get();
                savedChatHeightUnfocused = MC.options.chatHeightUnfocused().get();
                savedChatScale = MC.options.chatScale().get();
                savedChatOpacity = MC.options.chatOpacity().get();
            }
            setIfNot(MC.options.chatHeightFocused(), 0.0D);
            setIfNot(MC.options.chatHeightUnfocused(), 0.0D);
            setIfNot(MC.options.chatScale(), 0.0D);
            setIfNot(MC.options.chatOpacity(), 0.0D);
        } catch (Throwable ignored) {

        }
    }

    private static void restoreVanillaChatHeight() {
        try {
            if (savedChatHeightFocused == null) return;
            setIfNot(MC.options.chatHeightFocused(), savedChatHeightFocused);
            setIfNot(MC.options.chatHeightUnfocused(), savedChatHeightUnfocused);
            setIfNot(MC.options.chatScale(), savedChatScale);
            setIfNot(MC.options.chatOpacity(), savedChatOpacity);
            savedChatHeightFocused = null;
        } catch (Throwable ignored) {

        }
    }

    private static void setIfNot(OptionInstance<Double> option, double value) {
        if (option.get() != value) {
            option.set(value);
        }
    }

    private static boolean migratedChatVisibility;

    private static void migrateChatVisibilityOnce() {
        if (migratedChatVisibility) return;
        migratedChatVisibility = true;
        try {
            OptionInstance<ChatVisiblity> option = MC.options.chatVisibility();
            if (option.get() == ChatVisiblity.HIDDEN) {
                option.set(ChatVisiblity.FULL);
            }
        } catch (Throwable ignored) {

        }
    }

    private static boolean migratedChatSize;

    private static void migrateChatSizeOnce() {
        if (migratedChatSize) return;
        migratedChatSize = true;
        try {
            if (MC.options.chatScale().get() <= 0.0D) MC.options.chatScale().set(1.0D);
            if (MC.options.chatHeightFocused().get() <= 0.0D) MC.options.chatHeightFocused().set(1.0D);
            if (MC.options.chatHeightUnfocused().get() <= 0.0D) MC.options.chatHeightUnfocused().set(0.4375D);
            if (MC.options.chatOpacity().get() <= 0.0D) MC.options.chatOpacity().set(1.0D);
        } catch (Throwable ignored) {

        }
    }
}
