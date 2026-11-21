package lol.silencecute.client.render.naven;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class ActiveRenderTarget {

   public final int framebuffer;
   public final int drawFramebuffer;
   public final int readFramebuffer;
   public final int viewportX;
   public final int viewportY;
   public final int viewportW;
   public final int viewportH;

   private ActiveRenderTarget(int drawFramebuffer, int readFramebuffer, int viewportX, int viewportY, int viewportW, int viewportH) {
      this.framebuffer = drawFramebuffer;
      this.drawFramebuffer = drawFramebuffer;
      this.readFramebuffer = readFramebuffer;
      this.viewportX = viewportX;
      this.viewportY = viewportY;
      this.viewportW = viewportW;
      this.viewportH = viewportH;
   }

   public static ActiveRenderTarget capture() {
      int drawFbo;
      int readFbo;
      try {
         drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
      } catch (Throwable ignored) {
         drawFbo = 0;
      }
      try {
         readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
      } catch (Throwable ignored) {
         readFbo = 0;
      }
      if (drawFbo == 0 && readFbo == 0) {
         try {
            int fbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            drawFbo = fbo;
            readFbo = fbo;
         } catch (Throwable ignored) {
            drawFbo = 0;
            readFbo = 0;
         }
      } else if (drawFbo == 0) {
         drawFbo = readFbo;
      } else if (readFbo == 0) {
         readFbo = drawFbo;
      }

      int[] vp = new int[]{0, 0, 0, 0};
      try {
         GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
      } catch (Throwable ignored) {
      }

      if (vp[2] <= 1 || vp[3] <= 1) {
         try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
               int w = mc.getWindow().getWidth();
               int h = mc.getWindow().getHeight();
               if (w > 1 && h > 1) {
                  vp[0] = 0;
                  vp[1] = 0;
                  vp[2] = w;
                  vp[3] = h;
               }
            }
         } catch (Throwable ignored) {
         }
      }

      return new ActiveRenderTarget(drawFbo, readFbo, vp[0], vp[1], Math.max(1, vp[2]), Math.max(1, vp[3]));
   }

   public void bind() {
      try {
         GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
      } catch (Throwable ignored) {
      }
      try {
         GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFramebuffer);
      } catch (Throwable ignored) {
      }
      RenderSystem.viewport(this.viewportX, this.viewportY, this.viewportW, this.viewportH);
   }
}
