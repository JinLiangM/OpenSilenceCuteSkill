package lol.silencecute.client.render.naven;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL30C;
import org.slf4j.Logger;

public final class StencilUtils {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean lastSetupSucceeded;

    private static int stencilRbo = -1;
    private static int stencilWidth;
    private static int stencilHeight;
    private static int stencilSamples;

    private static int originalDepthType;
    private static int originalDepthName;

    private static boolean broken;
    private static boolean warned;

    private StencilUtils() {
    }

    public static boolean isAvailable() {
        return !broken && lastSetupSucceeded;
    }

    public static void write(boolean renderClipLayer) {
        lastSetupSucceeded = setup();
        if (!lastSetupSucceeded) {
            RenderSystem.colorMask(true, true, true, true);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            return;
        }
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFFFF);
        try {
            GL11.glClearStencil(0);
        } catch (Throwable ignored) {

        }
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFFFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        if (!renderClipLayer) {
            RenderSystem.colorMask(false, false, false, false);
        }
    }

    public static void erase(boolean invert) {
        if (!lastSetupSucceeded) {
            RenderSystem.colorMask(true, true, true, true);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            return;
        }
        RenderSystem.colorMask(true, true, true, true);
        GL11.glStencilMask(0x0);
        GL11.glStencilFunc(invert ? GL11.GL_EQUAL : GL11.GL_NOTEQUAL, 1, 0xFFFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    public static void dispose() {
        RenderSystem.colorMask(true, true, true, true);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void invalidate() {
        stencilRbo = -1;
        stencilWidth = 0;
        stencilHeight = 0;
        stencilSamples = 0;
        lastSetupSucceeded = false;
    }

    public static void setupFBO() {
        setup();
    }

    public static void setupFBO(RenderTarget target) {
        if (target == null) return;
        target.bindWrite(false);
        setup();
    }

    private static boolean setup() {
        if (broken) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return false;
        RenderTarget main = minecraft.getMainRenderTarget();
        if (main == null) return false;

        int drawFbo = GL11.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        if (drawFbo != main.frameBufferId) {
            GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, main.frameBufferId);
        }

        int width = Math.max(1, main.width);
        int height = Math.max(1, main.height);
        int samples = GL11.glGetInteger(GL13C.GL_SAMPLES);

        if (stencilRbo < 0 || stencilWidth != width || stencilHeight != height
                || stencilSamples != samples) {
            rebuild(width, height, samples);
        }
        if (stencilRbo < 0) return false;

        if (isOurRboAttached()) return true;

        if (originalDepthName == 0) {
            try {
                originalDepthType = GL30C.glGetFramebufferAttachmentParameteri(
                        GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                        GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
                originalDepthName = GL30C.glGetFramebufferAttachmentParameteri(
                        GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                        GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
            } catch (Throwable ignored) {
                originalDepthType = 0;
                originalDepthName = 0;
            }
        }

        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_STENCIL_ATTACHMENT,
                GL30C.GL_RENDERBUFFER, stencilRbo);
        int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
        if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
            restoreDepth();
            broken = true;
            if (!warned) {
                warned = true;
                LOGGER.warn("[silencecute] 深度/模板附件替换失败（fbo={}, {}x{}, samples={}, status=0x{}），"
                                + "模板遮罩模糊已停用",
                        main.frameBufferId, width, height, samples, Integer.toHexString(status));
            }
            return false;
        }
        return true;
    }

    private static boolean isOurRboAttached() {
        if (stencilRbo < 0) return false;
        try {
            int type = GL30C.glGetFramebufferAttachmentParameteri(GL30C.GL_FRAMEBUFFER,
                    GL30C.GL_DEPTH_ATTACHMENT, GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
            if (type != GL30C.GL_RENDERBUFFER) return false;
            int name = GL30C.glGetFramebufferAttachmentParameteri(GL30C.GL_FRAMEBUFFER,
                    GL30C.GL_DEPTH_ATTACHMENT, GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
            return name == stencilRbo;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void rebuild(int width, int height, int samples) {
        if (stencilRbo >= 0) {
            try {
                GL30C.glDeleteRenderbuffers(stencilRbo);
            } catch (Throwable ignored) {

            }
        }
        stencilRbo = GL30C.glGenRenderbuffers();
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, stencilRbo);
        if (samples > 0) {
            GL30C.glRenderbufferStorageMultisample(GL30C.GL_RENDERBUFFER, samples,
                    GL30C.GL_DEPTH24_STENCIL8, width, height);
        } else {
            GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL30C.GL_DEPTH24_STENCIL8, width, height);
        }
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);
        stencilWidth = width;
        stencilHeight = height;
        stencilSamples = samples;
    }

    private static void restoreDepth() {
        try {
            GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_STENCIL_ATTACHMENT,
                    GL30C.GL_RENDERBUFFER, 0);
            if (originalDepthName != 0 && originalDepthType == GL30C.GL_RENDERBUFFER) {
                GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                        GL30C.GL_RENDERBUFFER, originalDepthName);
            } else if (originalDepthName != 0 && originalDepthType == GL11.GL_TEXTURE) {
                GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                        GL11.GL_TEXTURE_2D, originalDepthName, 0);
            }
        } catch (Throwable ignored) {

        }
    }
}
