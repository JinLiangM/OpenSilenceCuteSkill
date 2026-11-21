package lol.silencecute.client.render.hud.notification;

import com.mojang.blaze3d.systems.RenderSystem;
import lol.silencecute.client.lang.Lang;
import lol.silencecute.client.font.ModFonts;
import lol.silencecute.client.font.TrueTypeFontRenderer;
import lol.silencecute.client.render.naven.RenderUtils;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationRea1 {

    public enum Level {
        INFO(0xFF171626),
        SUCCESS(0xFF179626),
        WARNING(0xFF8A5A5C),
        ERROR(0xFF942A2B);

        public final int color;

        Level(int color) {
            this.color = color;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static final String FONT_FILE = "harmony.ttf";
    private static final float FONT_SIZE = 10.0F;

    private static final float ENTRY_HEIGHT = 24.0F;

    private static final float PANEL_HEIGHT = 20.0F;

    private static final float BOTTOM_MARGIN = 5.0F;
    private static final float RIGHT_MARGIN = 2.0F;

    private static final long STAY_MS = 2400L;

    private static long lastFrameNanos;

    private NotificationRea1() {
    }

    public static void post(Level level, String text) {
        ENTRIES.add(new Entry(level, text == null ? "" : text));
    }

    public static void info(String text) {
        post(Level.INFO, text);
    }

    public static void success(String text) {
        post(Level.SUCCESS, text);
    }

    public static void warning(String text) {
        post(Level.WARNING, text);
    }

    public static void error(String text) {
        post(Level.ERROR, text);
    }

    public static String moduleLabel(String name) {
        return Lang.byZh(name);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ModConfigData.notificationRea1()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        long now = System.nanoTime();
        float deltaMs = lastFrameNanos == 0L
                ? 16.0F
                : Math.min(100.0F, (now - lastFrameNanos) / 1_000_000.0F);
        lastFrameNanos = now;

        if (ENTRIES.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();

        float height = BOTTOM_MARGIN;

        for (Iterator<Entry> it = ENTRIES.iterator(); it.hasNext(); ) {
            Entry entry = it.next();

            height += ENTRY_HEIGHT;

            if (entry.expired()) {
                entry.widthTarget = 0.0F;
                entry.heightTarget = 0.0F;
                if (entry.widthTimer <= 0.5F) {
                    it.remove();
                    continue;
                }
            } else {
                entry.widthTarget = entry.width;

                entry.heightTarget = height;
            }

            entry.update(deltaMs);

            float x = screenW - entry.widthTimer + RIGHT_MARGIN;
            float y = screenH - entry.heightTimer;

            entry.render(graphics, x, y);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static final class Entry {

        private final Level level;
        private final String text;
        private final long createTime = System.currentTimeMillis();
        private final float width;
        private final int textWidth;

        private float widthTimer;
        private float heightTimer;
        private float widthTarget;
        private float heightTarget;

        private Entry(Level level, String text) {
            this.level = level;
            this.text = text;

            TrueTypeFontRenderer font = ModFonts.ttf(FONT_FILE, FONT_SIZE);
            this.textWidth = font == null ? text.length() * 5 : font.getStringWidth(text);

            this.width = Math.max(40.0F, this.textWidth + 12.0F);
        }

        private boolean expired() {
            return System.currentTimeMillis() - createTime > STAY_MS;
        }

        private void update(float deltaMs) {
            widthTimer = animState(widthTimer, widthTarget, deltaMs);
            heightTimer = animState(heightTimer, heightTarget, deltaMs);
        }

        private void render(GuiGraphics graphics, float x, float y) {
            int color = level.color;

            RenderUtils.drawRoundedRect(graphics.pose(), x + 2.0F, y + 4.0F,
                    Math.max(1.0F, width), PANEL_HEIGHT, 5.0F, color);

            TrueTypeFontRenderer font = ModFonts.ttf(FONT_FILE, FONT_SIZE);
            if (font != null) {

                float textY = y + 4.0F + (PANEL_HEIGHT - font.getLineHeight()) * 0.5F;
                font.drawString(graphics, text, x + 6.0F, textY, 0xFFFFFFFF, false);
            }
        }

        private static float animState(float current, float target, float deltaMs) {
            float speed = Math.max(10.0F, Math.abs(current - target) * 40.0F) * 0.4F;
            float add = deltaMs * speed / 1000.0F;
            if (current < target) return Math.min(current + add, target);
            if (current > target) return Math.max(current - add, target);
            return current;
        }
    }
}
