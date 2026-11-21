package lol.silencecute.client.render.naven;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import lol.silencecute.Client;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30C;
import org.slf4j.Logger;

import java.util.function.Consumer;

public final class NvPostFx {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int LEVELS = 6;

    private static final float[][] STRENGTHS = {
            {1f, 1.25f}, {1f, 2.25f},
            {2f, 2.0f}, {2f, 3.0f}, {2f, 4.25f},
            {3f, 2.5f}, {3f, 3.25f}, {3f, 4.25f}, {3f, 5.5f},
            {4f, 3.25f}, {4f, 4.0f}, {4f, 5.0f}, {4f, 6.0f}, {4f, 7.25f}, {4f, 8.25f},
            {5f, 4.5f}, {5f, 5.25f}, {5f, 6.25f}, {5f, 7.25f}, {5f, 8.5f}
    };

    private static final int[] fboIds = new int[LEVELS];
    private static final int[] texIds = new int[LEVELS];
    private static final int[] fbWidths = new int[LEVELS];
    private static final int[] fbHeights = new int[LEVELS];

    private static ShaderInstance shaderDown;
    private static ShaderInstance shaderUp;
    private static boolean allocated;

    private static boolean broken;
    private static long lastChainNanos;

    private static boolean chainDecidedThisFrame;

    private NvPostFx() {
    }

    public static boolean available() {
        return !broken;
    }

    public static void beginFrame() {
        chainDecidedThisFrame = false;
    }

    public static int strengthCount() {
        return STRENGTHS.length;
    }

    public static void blur(PoseStack pose, float x, float y, float w, float h, float strength, float radius) {
        blur(pose, strength, shapes -> shapes.add(x, y, w, h, radius));
    }

    public static void blur(PoseStack pose, float strength, Consumer<ShapePainter> shapes) {
        if (broken) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        RenderTarget main = minecraft.getMainRenderTarget();
        if (main == null) return;
        if (!ensureResources(minecraft, main)) return;

        int index = Mth.clamp(Math.round(strength), 0, STRENGTHS.length - 1);
        int iterations = Mth.clamp((int) STRENGTHS[index][0], 1, LEVELS - 1);
        float offset = STRENGTHS[index][1];

        boolean scissorWasOn = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissorWasOn) GlStateManager._disableScissorTest();

        StencilUtils.write(false);
        if (!StencilUtils.isAvailable()) {
            StencilUtils.dispose();
            RenderSystem.colorMask(true, true, true, true);
            if (scissorWasOn) GlStateManager._enableScissorTest();
            return;
        }
        try {
            paintShapes(pose, shapes);
            StencilUtils.erase(true);

            if (!chainDecidedThisFrame) {
                chainDecidedThisFrame = true;
                long now = System.nanoTime();
                int fps = Math.max(15, Math.min(240, Math.round(ModConfigData.navenBlurFps())));
                long interval = 1_000_000_000L / fps;
                if (lastChainNanos == 0L || now - lastChainNanos >= interval) {
                    computeChain(minecraft, main, iterations, offset);
                    lastChainNanos = now;
                }
            }

            if (scissorWasOn) GlStateManager._enableScissorTest();
            composite(main, offset);
        } finally {
            if (scissorWasOn) GlStateManager._enableScissorTest();
            StencilUtils.dispose();
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    private static void paintShapes(PoseStack pose, Consumer<ShapePainter> shapes) {
        RenderSystem.assertOnRenderThread();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        shapes.accept(new ShapePainter(pose.last().pose()));
    }

    public static final class ShapePainter {

        private final Matrix4f matrix;

        private ShapePainter(Matrix4f matrix) {
            this.matrix = matrix;
        }

        public void add(float x, float y, float width, float height, float radius) {
            if (width <= 0.5f || height <= 0.5f) return;
            float r = Mth.clamp(radius, 0.0f, Math.min(width, height) * 0.5f);

            quad(x + r, y + r, width - r * 2.0f, height - r * 2.0f);
            quad(x + r, y, width - r * 2.0f, r);
            quad(x + r, y + height - r, width - r * 2.0f, r);
            quad(x, y + r, r, height - r * 2.0f);
            quad(x + width - r, y + r, r, height - r * 2.0f);
            if (r > 0.01f) {
                corner(x + r, y + r, r, 180.0);
                corner(x + width - r, y + r, r, 90.0);
                corner(x + r, y + height - r, r, 270.0);
                corner(x + width - r, y + height - r, r, 0.0);
            }
        }

        private void quad(float x, float y, float w, float h) {
            if (w <= 0.0f || h <= 0.0f) return;
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            put(buffer, x, y + h);
            put(buffer, x + w, y + h);
            put(buffer, x + w, y);
            put(buffer, x, y);
            Tesselator.getInstance().end();
        }

        private void corner(float centerX, float centerY, float r, double startDegrees) {
            int vertices = (int) Mth.clamp(r, 10.0f, 90.0f);
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            put(buffer, centerX, centerY);
            for (int i = 0; i <= vertices; i++) {
                double angle = (Math.PI * 2.0) * (i + startDegrees) / (vertices * 4.0);
                put(buffer, (float) (centerX + Math.sin(angle) * r),
                        (float) (centerY + Math.cos(angle) * r));
            }
            Tesselator.getInstance().end();
        }

        private void put(BufferBuilder buffer, float x, float y) {
            buffer.vertex(matrix, x, y, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).endVertex();
        }
    }

    private static void computeChain(Minecraft minecraft, RenderTarget main, int iterations, float offset) {
        RenderSystem.assertOnRenderThread();

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        GL11.glDisable(GL11.GL_STENCIL_TEST);

        renderToFbo(0, main.getColorTextureId(), shaderDown, offset);
        for (int i = 0; i < iterations; i++) {
            renderToFbo(i + 1, texIds[i], shaderDown, offset);
        }
        for (int i = iterations; i >= 1; i--) {
            renderToFbo(i - 1, texIds[i], shaderUp, offset);
        }

        main.bindWrite(false);
        RenderSystem.viewport(0, 0, main.width, main.height);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void renderToFbo(int level, int sourceTexture, ShaderInstance shader, float offset) {
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fboIds[level]);
        RenderSystem.viewport(0, 0, fbWidths[level], fbHeights[level]);
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, sourceTexture);

        shader.apply();
        shader.safeGetUniform("uHalfTexelX").set(0.5f / Math.max(1, fbWidths[level]));
        shader.safeGetUniform("uHalfTexelY").set(0.5f / Math.max(1, fbHeights[level]));
        shader.safeGetUniform("uOffset").set(offset);
        drawFullscreenQuad();
    }

    private static void composite(RenderTarget main, float offset) {
        main.bindWrite(false);
        RenderSystem.viewport(0, 0, main.width, main.height);

        GL11.glEnable(GL11.GL_STENCIL_TEST);

        RenderSystem.setShader(() -> shaderUp);
        RenderSystem.setShaderTexture(0, texIds[1]);
        shaderUp.apply();
        shaderUp.safeGetUniform("uHalfTexelX").set(0.5f / Math.max(1, fbWidths[1]));
        shaderUp.safeGetUniform("uHalfTexelY").set(0.5f / Math.max(1, fbHeights[1]));
        shaderUp.safeGetUniform("uOffset").set(offset);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawFullscreenQuad();
    }

    private static void drawFullscreenQuad() {
        Matrix4f identity = new Matrix4f();
        BufferBuilder buffer = new BufferBuilder(4 * DefaultVertexFormat.POSITION_TEX.getVertexSize());
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(identity, -1.0f, -1.0f, 0.0f).uv(0.0f, 0.0f).endVertex();
        buffer.vertex(identity, 1.0f, -1.0f, 0.0f).uv(1.0f, 0.0f).endVertex();
        buffer.vertex(identity, 1.0f, 1.0f, 0.0f).uv(1.0f, 1.0f).endVertex();
        buffer.vertex(identity, -1.0f, 1.0f, 0.0f).uv(0.0f, 1.0f).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    private static boolean ensureResources(Minecraft minecraft, RenderTarget main) {
        if (shaderDown == null || shaderUp == null) {
            try {
                shaderDown = new ShaderInstance(minecraft.getResourceManager(),
                        ResourceLocation.fromNamespaceAndPath(Client.MOD_ID, "blur_down"),
                        DefaultVertexFormat.POSITION_TEX);
                shaderUp = new ShaderInstance(minecraft.getResourceManager(),
                        ResourceLocation.fromNamespaceAndPath(Client.MOD_ID, "blur_up"),
                        DefaultVertexFormat.POSITION_TEX);
            } catch (Throwable throwable) {
                broken = true;
                LOGGER.warn("[silencecute] 模糊 shader 编译失败，背景模糊已停用: {}", throwable.toString());
                return false;
            }
        }

        if (!allocated) {
            for (int i = 0; i < LEVELS; i++) {
                texIds[i] = GlStateManager._genTexture();
                fboIds[i] = GL30C.glGenFramebuffers();
            }
            allocated = true;
        }

        boolean resized = false;
        for (int i = 0; i < LEVELS; i++) {
            int width = Math.max(1, main.width >> i);
            int height = Math.max(1, main.height >> i);
            if (width != fbWidths[i] || height != fbHeights[i]) {
                fbWidths[i] = width;
                fbHeights[i] = height;
                resized = true;
            }
        }
        if (resized) {
            allocateStorage();
        }
        return !broken;
    }

    private static void allocateStorage() {
        RenderSystem.assertOnRenderThread();

        int previousFbo = GL11.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        for (int i = 0; i < LEVELS; i++) {
            GlStateManager._bindTexture(texIds[i]);
            GlStateManager._texImage2D(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_RGBA8,
                    fbWidths[i], fbHeights[i], 0, org.lwjgl.opengl.GL11.GL_RGBA,
                    org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, null);
            GlStateManager._texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
            GlStateManager._texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
            GlStateManager._texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S, org.lwjgl.opengl.GL11.GL_CLAMP);
            GlStateManager._texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T, org.lwjgl.opengl.GL11.GL_CLAMP);

            GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fboIds[i]);
            GlStateManager._glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                    org.lwjgl.opengl.GL11.GL_TEXTURE_2D, texIds[i], 0);
            int status = GlStateManager.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
            if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
                broken = true;
                LOGGER.warn("[silencecute] 模糊 FBO 不完整 (status={})，背景模糊已停用", status);
                break;
            }
        }
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, previousFbo);
        RenderSystem.setShaderTexture(0, 0);
    }
}
