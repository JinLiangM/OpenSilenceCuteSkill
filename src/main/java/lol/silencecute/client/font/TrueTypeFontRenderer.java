package lol.silencecute.client.font;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.slf4j.Logger;

public final class TrueTypeFontRenderer {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int GRID = 16;
    private static final int BLOCK_SIZE = GRID * GRID;

    private static final int PADDING = 2;

    private final int em;

    private static final int FULL_BRIGHT = 0x00F000F0;

    private static final int[] COLOR_CODES = new int[32];

    static {
        for (int i = 0; i < 32; i++) {
            int offset = (i >> 3 & 1) * 85;
            int red = (i >> 2 & 1) * 170 + offset;
            int green = (i >> 1 & 1) * 170 + offset;
            int blue = (i & 1) * 170 + offset;
            if (i == 6) red += 85;
            if (i >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            COLOR_CODES[i] = (red & 255) << 16 | (green & 255) << 8 | blue & 255;
        }
    }

    private final String file;
    private final Font font;
    private final FontRenderContext context = new FontRenderContext(null, true, false);
    private final float size;

    private final float scale;
    private final int ascent;
    private final int descent;

    private final Map<Integer, Block> blocks = new HashMap<>();

    private TrueTypeFontRenderer fallback;
    private boolean fallbackResolved;

    public TrueTypeFontRenderer(String id, byte[] ttf, float size, int em) {
        this.file = id;
        this.size = size;
        this.em = Math.max(8, Math.min(128, em));
        this.scale = size / this.em;
        Font created;
        try {
            created = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(ttf)).deriveFont((float) this.em);
        } catch (Exception e) {
            throw new IllegalStateException("字体解析失败: " + id, e);
        }
        this.font = created;

        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = probe.createGraphics();
        graphics.setFont(this.font);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics metrics = graphics.getFontMetrics();
        this.ascent = metrics.getAscent();
        this.descent = metrics.getDescent();
        graphics.dispose();
    }

    public int getLineHeight() {
        return Math.max(1, Math.round((ascent + descent) * scale));
    }

    public float size() {
        return size;
    }

    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        float width = 0.0F;
        float longest = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                longest = Math.max(longest, width);
                width = 0.0F;
                continue;
            }
            if (c == '\u00a7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            Glyph glyph = glyphOf(c);
            if (glyph == null) {
                TrueTypeFontRenderer fb = fallback();
                if (fb == null) continue;
                Glyph other = fb.glyphOf(c);
                if (other == null) continue;
                width += other.advance() * fb.scale;
                continue;
            }
            width += glyph.advance() * scale;
        }
        return (int) Math.ceil(Math.max(longest, width));
    }

    public void drawString(GuiGraphics graphics, String text, float x, float y, int color, boolean shadow) {
        if (graphics == null || text == null || text.isEmpty()) return;
        if ((color & 0xFC000000) == 0) color |= 0xFF000000;
        if (shadow) drawString(graphics, text, x + 1.0F, y + 1.0F, darken(color), false);

        Matrix4f matrix = graphics.pose().last().pose();
        float lineHeight = getLineHeight();
        float cursorX = x;
        float baseline = y + ascent * scale;
        int current = color;
        Block activeBlock = null;
        VertexConsumer consumer = null;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                cursorX = x;
                baseline += lineHeight;
                continue;
            }

            if (c == '\u00a7' && i + 1 < text.length()) {
                int index = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(++i)));
                if (index < 0) continue;
                if (index < 16) {
                    current = 0xFF000000 | COLOR_CODES[index];
                } else if (index == 21) {

                    current = color;
                }
                continue;
            }

            Block block = blockFor(c >>> 8);
            Glyph glyph = block.glyphs[c & 0xFF];
            if (glyph == null) {

                TrueTypeFontRenderer fb = fallback();
                if (fb != null) {
                    String single = String.valueOf(c);
                    fb.drawString(graphics, single, cursorX, y, current, false);
                    cursorX += fb.getStringWidth(single);
                }
                continue;
            }

            if (block != activeBlock || consumer == null) {
                activeBlock = block;
                consumer = graphics.bufferSource().getBuffer(block.renderType);
            }

            float x0 = cursorX + glyph.x0() * scale;
            float y0 = baseline + glyph.y0() * scale;
            float x1 = x0 + glyph.width() * scale;
            float y1 = y0 + glyph.height() * scale;
            quad(consumer, matrix, x0, y0, x1, y1,
                    glyph.u0(), glyph.v0(), glyph.u1(), glyph.v1(), current);

            cursorX += glyph.advance() * scale;
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                             float x0, float y0, float x1, float y1,
                             float u0, float v0, float u1, float v1, int color) {
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        consumer.vertex(matrix, x0, y0, 0.0F).color(red, green, blue, alpha).uv(u0, v0).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x0, y1, 0.0F).color(red, green, blue, alpha).uv(u0, v1).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x1, y1, 0.0F).color(red, green, blue, alpha).uv(u1, v1).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x1, y0, 0.0F).color(red, green, blue, alpha).uv(u1, v0).uv2(FULL_BRIGHT).endVertex();
    }

    private static int darken(int color) {
        return (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
    }

    public static final int SUPERSAMPLE = 2;

    private record Glyph(float x0, float y0, float width, float height,
                         float u0, float v0, float u1, float v1, float advance) {
    }

    private record Block(ResourceLocation location, RenderType renderType, Glyph[] glyphs) {
    }

    private static RenderType createRenderType(ResourceLocation location) {
        return RenderType.create("silencecute_text/" + location.getPath(),
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextShader))

                        .setTextureState(new RenderStateShard.TextureStateShard(location, true, false))

                        .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                        .setTransparencyState(TRANSLUCENT)
                        .setWriteMaskState(COLOR_ONLY)
                        .createCompositeState(false));
    }

    private static final RenderStateShard.TransparencyStateShard TRANSLUCENT =
            new RenderStateShard.TransparencyStateShard("silencecute_font_translucent", () -> {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            }, () -> RenderSystem.disableBlend());

    private static final RenderStateShard.WriteMaskStateShard COLOR_ONLY =
            new RenderStateShard.WriteMaskStateShard(true, false);

    private Glyph glyphOf(char c) {
        return blockFor(c >>> 8).glyphs[c & 0xFF];
    }

    private TrueTypeFontRenderer fallback() {
        if (!fallbackResolved) {
            fallbackResolved = true;
            if (!ModFonts.DEFAULT_FONT.equals(file)) {
                try {
                    fallback = ModFonts.ttf(ModFonts.DEFAULT_FONT, size);
                } catch (Throwable throwable) {
                    LOGGER.warn("[silencecute] 回退字体不可用: {}", throwable.toString());
                }
            }
        }
        return fallback;
    }

    private Block blockFor(int blockIndex) {
        Block existing = blocks.get(blockIndex);
        if (existing != null) return existing;

        int base = blockIndex * BLOCK_SIZE;

        GlyphVector[] vectors = new GlyphVector[BLOCK_SIZE];
        Rectangle2D[] bounds = new Rectangle2D[BLOCK_SIZE];
        int cellWidth = 0;
        int cellHeight = 0;
        for (int i = 0; i < BLOCK_SIZE; i++) {
            char c = (char) (base + i);
            if (!font.canDisplay(c)) continue;
            GlyphVector vector = font.createGlyphVector(context, new char[]{c});
            Rectangle2D box = vector.getVisualBounds();
            if (box == null || box.isEmpty()) {

                vectors[i] = vector;
                bounds[i] = box;
                continue;
            }
            vectors[i] = vector;
            bounds[i] = box;
            cellWidth = Math.max(cellWidth, (int) Math.ceil(box.getWidth()));
            cellHeight = Math.max(cellHeight, (int) Math.ceil(box.getHeight()));
        }

        int textureWidth = Math.max(1, (cellWidth + PADDING * 2) * GRID);
        int textureHeight = Math.max(1, (cellHeight + PADDING * 2) * GRID);

        BufferedImage image = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Glyph[] glyphs = new Glyph[BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++) {
            GlyphVector vector = vectors[i];
            if (vector == null) continue;
            if (vector.getNumGlyphs() == 0) continue;

            Rectangle2D box = bounds[i];
            float advance = vector.getGlyphMetrics(0).getAdvance();
            if (box == null || box.isEmpty()) {

                glyphs[i] = new Glyph(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, advance);
                continue;
            }

            int cellX = (i % GRID) * (cellWidth + PADDING * 2);
            int cellY = (i / GRID) * (cellHeight + PADDING * 2);
            float inkX = (float) box.getX();
            float inkY = (float) box.getY();
            float inkWidth = (float) box.getWidth();
            float inkHeight = (float) box.getHeight();

            graphics.drawGlyphVector(vector, cellX + PADDING - inkX, cellY + PADDING - inkY);

            float u0 = (cellX + PADDING) / (float) textureWidth;
            float v0 = (cellY + PADDING) / (float) textureHeight;
            float u1 = (cellX + PADDING + inkWidth) / (float) textureWidth;
            float v1 = (cellY + PADDING + inkHeight) / (float) textureHeight;
            glyphs[i] = new Glyph(inkX, inkY, inkWidth, inkHeight, u0, v0, u1, v1, advance);
        }
        graphics.dispose();

        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, textureWidth, textureHeight, true);

        int[] argb = image.getRGB(0, 0, textureWidth, textureHeight, null, 0, textureWidth);
        for (int y = 0; y < textureHeight; y++) {
            int row = y * textureWidth;
            for (int x = 0; x < textureWidth; x++) {
                int pixel = argb[row + x];
                int alpha = (pixel >>> 24) & 0xFF;
                if (alpha == 0) continue;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                nativeImage.setPixelRGBA(x, y, (alpha << 24) | (blue << 16) | (green << 8) | red);
            }
        }

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("silencecute_skill",
                "font/glyphs_" + sanitize(file) + "_" + (int) size + "_" + em + "_" + blockIndex);
        DynamicTexture texture = new DynamicTexture(nativeImage);
        texture.setFilter(true, false);
        Minecraft.getInstance().getTextureManager().register(location, texture);

        texture.upload();

        Block block = new Block(location, createRenderType(location), glyphs);
        blocks.put(blockIndex, block);
        return block;
    }

    private static String sanitize(String name) {
        String lowered = name.toLowerCase(java.util.Locale.ROOT);
        StringBuilder builder = new StringBuilder(lowered.length());
        for (int i = 0; i < lowered.length(); i++) {
            char c = lowered.charAt(i);
            builder.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ? c : '_');
        }
        return builder.toString();
    }

    public void close() {
        for (Block block : blocks.values()) {
            try {
                Minecraft.getInstance().getTextureManager().release(block.location);
            } catch (Throwable ignored) {

            }
        }
        blocks.clear();
    }
}
