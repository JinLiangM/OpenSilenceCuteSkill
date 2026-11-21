package lol.silencecute.client.render.naven;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class RoundedClipUtils implements Wrapper {
   private static Shader shader;
   private static boolean initFailed;
   private static boolean warned;
   private static ResourceLocation whiteTextureLocation;

   private RoundedClipUtils() {
   }

   public static boolean isAvailable() {
      if (initFailed) {
         return false;
      }
      if (shader == null) {
         try {
            shader = new Shader("blur.vert", "rounded_clip.frag");
         } catch (Throwable t) {
            initFailed = true;
            if (!warned) {
               warned = true;
               try {
                  ChatUtils.addChatMessage("RoundedClip shader init failed; disabling rounded clip.");
               } catch (Throwable ignored) {
               }
            }
            return false;
         }
      }
      return true;
   }

   public static void render(PoseStack stack, ActiveRenderTarget out, int textureId, float x, float y, float w, float h, float radius) {
      renderColored(stack, out, textureId, x, y, w, h, radius, 1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void renderColored(PoseStack stack, ActiveRenderTarget out, int textureId, float x, float y, float w, float h, float radius, float tintR, float tintG, float tintB, float alpha) {
      if (!isAvailable()) {
         return;
      }
      if (out == null) {
         return;
      }

      int guiW = mc.getWindow().getGuiScaledWidth();
      int guiH = mc.getWindow().getGuiScaledHeight();
      if (guiW <= 0 || guiH <= 0) {
         return;
      }

      double scaleX = (double)out.viewportW / (double)guiW;
      double scaleY = (double)out.viewportH / (double)guiH;

      float rx = (float)(x * scaleX);
      float ry = (float)((guiH - (y + h)) * scaleY);
      float rw = (float)(w * scaleX);
      float rh = (float)(h * scaleY);
      float rr = (float)(radius * Math.min(scaleX, scaleY));

      try {
         boolean depthMaskSaved = true;
         try {
            depthMaskSaved = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
         } catch (Throwable ignored) {
         }

         GL.saveState();
         out.bind();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();

         shader.bind();
         shader.set("uTexture", 0);
         shader.set("uSize", (double)out.viewportW, (double)out.viewportH);
         shader.set("uRect", rx, ry, rw, rh);
         shader.set("uRadius", (double)rr);
         shader.set("uTint", tintR, tintG, tintB, alpha);

         PostProcessRenderer.beginRender(stack);
         try {
            GL.bindTexture(textureId, 0);
            PostProcessRenderer.render(stack);
         } finally {
            PostProcessRenderer.endRender();
         }

         try {
            RenderSystem.depthMask(depthMaskSaved);
         } catch (Throwable ignored) {
         }
      } finally {
         GL.resetTextureSlot();
         GL.restoreState();

         try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         } catch (Throwable ignored) {
         }
      }
   }

   public static int getWhiteTextureId() {
      try {
         if (whiteTextureLocation == null) {
            NativeImage image = new NativeImage(Format.RGBA, 1, 1, false);
            image.setPixelRGBA(0, 0, -1);
            DynamicTexture texture = new DynamicTexture(image);
            whiteTextureLocation = mc.getTextureManager().register("naven_white_round", texture);
         }
         return mc.getTextureManager().getTexture(whiteTextureLocation).getId();
      } catch (Throwable ignored) {
         return 0;
      }
   }
}
