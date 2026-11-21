package lol.silencecute.client.screen.fatality;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import lol.silencecute.api.FontMode;
import lol.silencecute.client.font.UiFont;
import lol.silencecute.client.render.naven.text.CustomTextRenderer;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

interface FaFont {
    float width(String text);

    float height();

    float LOGO = 16.0f;
    float TAB = 10.6f;
    float TITLE = 11.6f;
    float LABEL = 11.2f;
    float FIELD = 10.6f;
    float SMALL = 9.8f;

    String TEXT_FILE = "font.ttf";

    String LOGO_FILE = "manrope-bold.ttf";

    String ICON_FILE = "lucide.ttf";

    record Game(UiFont font) implements FaFont {
        @Override
        public float width(String text) {
            return font.width(text);
        }

        @Override
        public float height() {
            return font.height();
        }
    }

    record Vanilla(float size) implements FaFont {
        private static final float BASE = 9.0f;

        @Override
        public float width(String text) {
            if (text == null || text.isEmpty()) return 0.0f;
            return Minecraft.getInstance().font.width(text) * size / BASE;
        }

        @Override
        public float height() {
            return size;
        }

        void draw(GuiGraphics graphics, String text, float x, float y, int color) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(x, y, 0.0f);
            float k = size / BASE;
            pose.scale(k, k, 1.0f);
            graphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, false);
            pose.popPose();
        }
    }

    boolean USE_NAVEN_RENDERER = false;

    record Naven(CustomTextRenderer renderer, float size) implements FaFont {
        private double navenScale() {
            return size / 32.0;
        }

        @Override
        public float width(String text) {
            return text == null || text.isEmpty() ? 0.0f : (float) renderer.getWidth(text, navenScale());
        }

        @Override
        public float height() {
            return (float) renderer.getHeight(false, navenScale());
        }
    }

    interface Provider {
        FaFont logo();

        FaFont tab();

        FaFont title();

        FaFont label();

        FaFont field();

        FaFont small();

        FaFont icon(float size);
    }

    Provider GAME = new Provider() {
        private final Map<String, FaFont> cache = new HashMap<>();

        private FaFont get(String file, float size) {
            return ttf(file, size);
        }

        private FaFont ttf(String file, float size) {
            return cache.computeIfAbsent(file + "@" + size + "@" + USE_NAVEN_RENDERER, ignored -> {
                if (USE_NAVEN_RENDERER) {
                    try {
                        return new Naven(navenRenderer(file), size);
                    } catch (Throwable throwable) {

                        System.err.println("[silencecute] Naven 字体渲染器不可用，回退内置: " + throwable);
                    }
                }
                return new Game(new UiFont(file, size));
            });
        }

        private CustomTextRenderer navenRenderer(String file) {
            String name = file.endsWith(".ttf") ? file.substring(0, file.length() - 4) : file;
            if (TEXT_FILE.equals(file)) return new CustomTextRenderer(name, 32, 0x20, 0x9FFF, 16384);
            if (ICON_FILE.equals(file)) return new CustomTextRenderer(name, 32, 0xE000, 0xF8FF, 4096);
            return new CustomTextRenderer(name, 32, 0x00, 0xFF, 512);
        }

        @Override public FaFont logo() { return get(LOGO_FILE, LOGO); }
        @Override public FaFont tab() { return get(TEXT_FILE, TAB); }
        @Override public FaFont title() { return get(TEXT_FILE, TITLE); }
        @Override public FaFont label() { return get(TEXT_FILE, LABEL); }
        @Override public FaFont field() { return get(TEXT_FILE, FIELD); }
        @Override public FaFont small() { return get(TEXT_FILE, SMALL); }

        @Override public FaFont icon(float size) { return ttf(ICON_FILE, size); }
    };
}
