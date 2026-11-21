package lol.silencecute.client.font;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.mojang.logging.LogUtils;

import lol.silencecute.Client;
import lol.silencecute.api.FontMode;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.slf4j.Logger;

public final class ModFonts {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int BASE_FONT_SIZE = 12;

    public static final String DEFAULT_FONT = "font.ttf";

    private static final float VANILLA_LINE_HEIGHT = 9.0F;

    private static final Map<String, TrueTypeFontRenderer> CACHE = new HashMap<>();

    private static int cachedGuiScale = Integer.MIN_VALUE;

    private ModFonts() {}

    public static FontMode mode() {
        return FontMode.MINECRAFT;
    }

    public static void setMode(FontMode mode) {
        if (mode == null) return;
        try {
            ModConfigData.GUI_FONT.set(mode);
            ModConfigData.SPEC.save();
        } catch (Exception e) {
            LOGGER.warn("[silencecute] 保存字体设置失败: {}", e.toString());
        }
    }

    public static float poseScale(GuiGraphics graphics) {
        if (graphics == null) return 1.0F;
        try {
            Matrix4f matrix = graphics.pose().last().pose();
            float scale = Math.max(Math.abs(matrix.m00()), Math.abs(matrix.m11()));
            return Float.isFinite(scale) && scale > 0.0F ? scale : 1.0F;
        } catch (Exception e) {
            return 1.0F;
        }
    }

    public static TrueTypeFontRenderer ttf(String file, float size) {
        return ttf(file, size, 1.0F);
    }

    public static TrueTypeFontRenderer ttf(String file, float size, float upsample) {
        int guiScale = guiScale();
        if (guiScale != cachedGuiScale) {

            CACHE.clear();
            cachedGuiScale = guiScale;
        }
        String key = file + "@" + size;
        if (CACHE.containsKey(key)) return CACHE.get(key);
        TrueTypeFontRenderer renderer = load(file, size, guiScale);
        CACHE.put(key, renderer);
        return renderer;
    }

    public static void warmup() {
        for (String file : new String[]{DEFAULT_FONT, "xylitol_bold.ttf", "xylitol_museo.ttf", "manrope-bold.ttf", "lucide.ttf"}) {
            try {
                ttf(file, BASE_FONT_SIZE);
            } catch (Throwable throwable) {
                LOGGER.warn("[silencecute] 预热字体 {} 失败: {}", file, throwable.toString());
            }
        }
    }

    private static void closeCache() {
        for (TrueTypeFontRenderer renderer : CACHE.values()) {
            if (renderer == null) continue;
            try {
                renderer.close();
            } catch (Exception e) {
                LOGGER.warn("[silencecute] 释放旧字体图集失败: {}", e.toString());
            }
        }
        CACHE.clear();
    }

    public static int guiScale() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getWindow() == null) return 2;
            int scale = (int) Math.round(minecraft.getWindow().getGuiScale());
            return Mth.clamp(scale, 1, 4);
        } catch (Exception e) {
            return 2;
        }
    }

    private static TrueTypeFontRenderer load(String file, float size, int guiScale) {
        try {
            byte[] bytes = readFontBytes(file);
            if (bytes == null || bytes.length == 0) return null;

            int em = Mth.clamp(Math.round(size * guiScale * TrueTypeFontRenderer.SUPERSAMPLE), 16, 192);
            return new TrueTypeFontRenderer(file, bytes, size, em);
        } catch (Throwable e) {
            LOGGER.warn("[silencecute] 加载字体 {} 失败，回退原版字体: {}", file, e.toString());
            return null;
        }
    }

    static byte[] readFontBytes(String file) {
        try {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Client.MOD_ID, "font/" + file);
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            if (manager == null) return null;
            try (InputStream stream = manager.getResource(location)
                    .orElseThrow(() -> new IOException("缺少字体资源 " + location)).open()) {
                return stream.readAllBytes();
            }
        } catch (Throwable throwable) {
            LOGGER.warn("[silencecute] 读不到字体资源 {}: {}", file, throwable.toString());
            return null;
        }
    }

    private static TrueTypeFontRenderer renderer() {
        if (mode() == FontMode.MINECRAFT) return null;
        return ttf(DEFAULT_FONT, BASE_FONT_SIZE);
    }

    private static TrueTypeFontRenderer renderer(GuiGraphics graphics) {
        if (mode() == FontMode.MINECRAFT) return null;
        return ttf(DEFAULT_FONT, BASE_FONT_SIZE, poseScale(graphics));
    }

    public static void drawStringSmooth(GuiGraphics graphics, String text, float x, float y,
                                        int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        ttf(DEFAULT_FONT, BASE_FONT_SIZE, poseScale(graphics)).drawString(graphics, text, x, y, color, shadow);
    }

    public static int smoothWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        return ttf(DEFAULT_FONT, BASE_FONT_SIZE).getStringWidth(text);
    }

    public static int smoothHeight() {
        return ttf(DEFAULT_FONT, BASE_FONT_SIZE).getLineHeight();
    }

    public static void drawString(GuiGraphics graphics, String text, float x, float y, int color) {
        drawString(graphics, text, x, y, color, false);
    }

    public static void drawString(GuiGraphics graphics, String text, float x, float y,
                                  int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        TrueTypeFontRenderer renderer = renderer(graphics);
        if (renderer == null) {
            graphics.drawString(Minecraft.getInstance().font, text, (int) x, (int) y, color, shadow);
            return;
        }
        renderer.drawString(graphics, text, x, y, color, shadow);
    }

    public static void drawCenteredString(GuiGraphics graphics, String text, float centerX, float y,
                                          int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        drawString(graphics, text, centerX - width(text) / 2.0F, y, color, shadow);
    }

    public static float width(String text) {
        if (text == null || text.isEmpty()) return 0.0F;
        TrueTypeFontRenderer renderer = renderer();
        if (renderer == null) {
            return Minecraft.getInstance().font.width(text);
        }
        return renderer.getStringWidth(text);
    }

    public static float height() {
        TrueTypeFontRenderer renderer = renderer();
        return renderer == null ? VANILLA_LINE_HEIGHT : renderer.getLineHeight();
    }
}
