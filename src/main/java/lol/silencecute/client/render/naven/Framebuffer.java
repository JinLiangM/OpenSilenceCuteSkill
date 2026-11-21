package lol.silencecute.client.render.naven;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

public class Framebuffer implements Wrapper {
   private int id;
   public int texture;
   public double sizeMulti = 1.0;
   public int width;
   public int height;
   private boolean fixedSize;
   private int fixedWidth;
   private int fixedHeight;

   public Framebuffer(double sizeMulti) {
      this.sizeMulti = sizeMulti;
      this.init();
   }

   public Framebuffer() {
      this(1.0);
   }

   public Framebuffer(int width, int height) {
      this.fixedSize = true;
      this.fixedWidth = width;
      this.fixedHeight = height;
      this.init();
   }

   private void init() {

      int prevDrawFbo = 0;
      int prevReadFbo = 0;
      int[] prevVp = new int[]{0, 0, 0, 0};
      int prevActiveTex = GL13.GL_TEXTURE0;
      int prevTex2D0 = 0;

      try {
         prevDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
      } catch (Throwable ignored) {
      }
      try {
         prevReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
      } catch (Throwable ignored) {
      }
      if (prevDrawFbo == 0 && prevReadFbo == 0) {
         try {
            int fbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            prevDrawFbo = fbo;
            prevReadFbo = fbo;
         } catch (Throwable ignored) {
         }
      } else if (prevDrawFbo == 0) {
         prevDrawFbo = prevReadFbo;
      } else if (prevReadFbo == 0) {
         prevReadFbo = prevDrawFbo;
      }
      try {
         GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevVp);
      } catch (Throwable ignored) {
      }
      try {
         prevActiveTex = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
      } catch (Throwable ignored) {
         prevActiveTex = GL13.GL_TEXTURE0;
      }
      try {
         GlStateManager._activeTexture(GL13.GL_TEXTURE0);
         prevTex2D0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
      } catch (Throwable ignored) {
      }

      this.id = GL.genFramebuffer();
      int baseW = mc.getWindow().getWidth();
      int baseH = mc.getWindow().getHeight();
      if (prevVp[2] > 0 && prevVp[3] > 0) {
         baseW = prevVp[2];
         baseH = prevVp[3];
      }

      if (this.fixedSize) {
         this.width = Math.max(1, this.fixedWidth);
         this.height = Math.max(1, this.fixedHeight);
      } else {
         this.width = Math.max(1, (int)((double)baseW * this.sizeMulti));
         this.height = Math.max(1, (int)((double)baseH * this.sizeMulti));
      }

      try {
         GL.bindFramebuffer(this.id);

         this.texture = GL.genTexture();
         GlStateManager._activeTexture(GL13.GL_TEXTURE0);
         GlStateManager._bindTexture(this.texture);

         GL.defaultPixelStore();
         GL.textureParam(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
         GL.textureParam(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
         GL.textureParam(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
         GL.textureParam(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

         GL.textureImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, this.width, this.height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null);
         GL.framebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.texture, 0);
         if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is not complete!");
         }
      } finally {

         try {
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(prevTex2D0);
            GlStateManager._activeTexture(prevActiveTex);
         } catch (Throwable ignored) {
         }

         try {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
         } catch (Throwable ignored) {
         }

         try {
            if (prevVp[2] > 0 && prevVp[3] > 0) {
               RenderSystem.viewport(prevVp[0], prevVp[1], prevVp[2], prevVp[3]);
            } else {
               Window window = mc.getWindow();
               RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
            }
         } catch (Throwable ignored) {
         }
      }
   }

   public int getId() {
      return this.id;
   }

   public void bind() {
      GL.bindFramebuffer(this.id);
      RenderSystem.viewport(0, 0, this.width, this.height);
   }

   public void setViewport() {
      RenderSystem.viewport(0, 0, this.width, this.height);
   }

   public void unbind() {
      mc.getMainRenderTarget().bindWrite(false);
      Window window = mc.getWindow();
      RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
   }

   public void resize() {
      GL.deleteFramebuffer(this.id);
      GL.deleteTexture(this.texture);
      this.init();
   }

   public void resizeTo(int width, int height) {
      this.fixedSize = true;
      this.fixedWidth = width;
      this.fixedHeight = height;
      this.resize();
   }

   public void clear() {
      RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
      RenderSystem.clear(16640, false);
   }
}
