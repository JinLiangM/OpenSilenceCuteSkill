package lol.silencecute.client.render.naven;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class RoundedTextureRenderer implements Wrapper {
   private static Shader shader;
   private static Mesh mesh;
   private static boolean initFailed;
   private static boolean warned;

   private RoundedTextureRenderer() {
   }

   private static boolean ensureInit() {
      if (initFailed) {
         return false;
      }
      if (shader == null) {
         try {
            shader = new Shader("rounded_tex.vert", "rounded_tex.frag");
         } catch (Throwable t) {
            initFailed = true;
            if (!warned) {
               warned = true;
               try {
                  ChatUtils.addChatMessage("RoundedTexture shader init failed; disabling rounded heads.");
               } catch (Throwable ignored) {
               }
            }
            return false;
         }
      }
      if (mesh == null) {
         mesh = new Mesh(DrawMode.Triangles, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2);
      }
      return true;
   }

   public static void drawRoundedSubTex(
      float x,
      float y,
      float w,
      float h,
      float radius,
      ResourceLocation texture,
      float u0,
      float v0,
      float u1,
      float v1,
      float tintR,
      float tintG,
      float tintB,
      float alpha
   ) {
      if (!ensureInit()) {
         return;
      }

      int[] vp = new int[]{0, 0, 0, 0};
      try {
         GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
      } catch (Throwable ignored) {
      }
      int vpW = vp[2] > 0 ? vp[2] : mc.getWindow().getWidth();
      int vpH = vp[3] > 0 ? vp[3] : mc.getWindow().getHeight();

      int guiW = mc.getWindow().getGuiScaledWidth();
      int guiH = mc.getWindow().getGuiScaledHeight();
      if (guiW <= 0 || guiH <= 0 || vpW <= 0 || vpH <= 0) {
         return;
      }

      double scaleX = (double)vpW / (double)guiW;
      double scaleY = (double)vpH / (double)guiH;

      double leftPx = x * scaleX;
      double rightPx = (x + w) * scaleX;
      double bottomPx = (guiH - (y + h)) * scaleY;
      double topPx = (guiH - y) * scaleY;

      float left = (float)(leftPx / (double)vpW * 2.0 - 1.0);
      float right = (float)(rightPx / (double)vpW * 2.0 - 1.0);
      float bottom = (float)(bottomPx / (double)vpH * 2.0 - 1.0);
      float top = (float)(topPx / (double)vpH * 2.0 - 1.0);

      float sizePxW = (float)(w * scaleX);
      float sizePxH = (float)(h * scaleY);
      float radiusPx = (float)(radius * Math.min(scaleX, scaleY));

      GL.saveState();
      try {
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();

         GL.bindTexture(texture);
         shader.bind();
         shader.set("uTexture", 0);
         shader.set("uSize", (double)sizePxW, (double)sizePxH);
         shader.set("uRadius", (double)radiusPx);
         shader.set("uTint", tintR, tintG, tintB);
         shader.set("uAlpha", (double)alpha);

         mesh.begin();
         int i0 = mesh.vec2(left, bottom).vec2(u0, v1).vec2(0.0, 0.0).next();
         int i1 = mesh.vec2(right, bottom).vec2(u1, v1).vec2(1.0, 0.0).next();
         int i2 = mesh.vec2(right, top).vec2(u1, v0).vec2(1.0, 1.0).next();
         int i3 = mesh.vec2(left, top).vec2(u0, v0).vec2(0.0, 1.0).next();
         mesh.quad(i0, i1, i2, i3);
         mesh.end();
         mesh.render(null);
      } finally {
         try {
            GL.resetTextureSlot();
            RenderSystem.defaultBlendFunc();

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         } catch (Throwable ignored) {
         }
         GL.restoreState();
      }
   }
}
