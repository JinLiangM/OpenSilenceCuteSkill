package lol.silencecute.client.render.naven;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32C;

public class GL implements Wrapper {
   private static final FloatBuffer MAT = BufferUtils.createFloatBuffer(16);
   private static final Object DEPTH = getTracker("DEPTH");
   private static final Object BLEND = getTracker("BLEND");
   private static final Object CULL = getTracker("CULL");
   private static final Object SCISSOR = getTracker("SCISSOR");
   private static boolean depthSaved;
   private static boolean blendSaved;
   private static boolean cullSaved;
   private static boolean scissorSaved;
   public static int CURRENT_IBO;
   private static int prevIbo;
   private static boolean changeBufferRenderer = true;

   public static int genVertexArray() {
      return GlStateManager._glGenVertexArrays();
   }

   public static int genBuffer() {
      return GlStateManager._glGenBuffers();
   }

   public static int genTexture() {
      return GlStateManager._genTexture();
   }

   public static int genFramebuffer() {
      return GlStateManager.glGenFramebuffers();
   }

   public static void saveState() {
      depthSaved = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      blendSaved = GL11.glIsEnabled(GL11.GL_BLEND);
      cullSaved = GL11.glIsEnabled(GL11.GL_CULL_FACE);
      scissorSaved = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
   }

   public static void restoreState() {
      if (depthSaved) {
         GlStateManager._enableDepthTest();
      } else {
         GlStateManager._disableDepthTest();
      }

      if (blendSaved) {
         GlStateManager._enableBlend();
      } else {
         GlStateManager._disableBlend();
      }

      if (cullSaved) {
         GlStateManager._enableCull();
      } else {
         GlStateManager._disableCull();
      }

      if (scissorSaved) {
         GlStateManager._enableScissorTest();
      } else {
         GlStateManager._disableScissorTest();
      }

      disableLineSmooth();
   }

   public static void deleteBuffer(int buffer) {
      GlStateManager._glDeleteBuffers(buffer);
   }

   public static void deleteVertexArray(int vao) {
      GlStateManager._glDeleteVertexArrays(vao);
   }

   public static void deleteShader(int shader) {
      GlStateManager.glDeleteShader(shader);
   }

   public static void deleteTexture(int id) {
      GlStateManager._deleteTexture(id);
   }

   public static void deleteFramebuffer(int fbo) {
      GlStateManager._glDeleteFramebuffers(fbo);
   }

   public static void deleteProgram(int program) {
      GlStateManager.glDeleteProgram(program);
   }

   public static void bindVertexArray(int vao) {
      if (vao >= 0) {
         try {
            GlStateManager._glBindVertexArray(vao);
            if (changeBufferRenderer) {
               ReflectUtil.setFieldValue(BufferUploader.class, "lastImmediateBuffer", null, null);
            }
         } catch (Exception var2) {
            System.err.println("Error binding VAO " + vao + ": " + var2.getMessage());
         }
      } else {
         System.err.println("WARNING: Attempted to bind invalid VAO: " + vao);
      }
   }

   public static void bindVertexBuffer(int vbo) {
      GlStateManager._glBindBuffer(34962, vbo);
   }

   public static void bindIndexBuffer(int ibo) {
      if (ibo != 0) {
         prevIbo = CURRENT_IBO;
      }

      try {
         int bindTarget = ibo != 0 ? ibo : prevIbo;
         GlStateManager._glBindBuffer(34963, bindTarget);
      } catch (Exception var2) {
         System.err.println("Error binding IBO: " + var2.getMessage());
      }
   }

   public static void bindFramebuffer(int fbo) {
      GlStateManager._glBindFramebuffer(36160, fbo);
   }

   public static void bufferData(int target, ByteBuffer data, int usage) {
      GlStateManager._glBufferData(target, data, usage);
   }

   public static void drawElements(int mode, int first, int type) {
      GlStateManager._drawElements(mode, first, type, 0L);
   }

   public static void enableVertexAttribute(int i) {
      RenderSystem.assertOnRenderThread();
      GL20.glEnableVertexAttribArray(i);
   }

   public static void vertexAttribute(int index, int size, int type, boolean normalized, int stride, long pointer) {
      GlStateManager._vertexAttribPointer(index, size, type, normalized, stride, pointer);
   }

   public static int createShader(int type) {
      return GlStateManager.glCreateShader(type);
   }

   public static void shaderSource(int shader, String source) {
      GlStateManager.glShaderSource(shader, ImmutableList.of(source));
   }

   public static String compileShader(int shader) {
      GlStateManager.glCompileShader(shader);
      return GlStateManager.glGetShaderi(shader, 35713) == 0 ? GlStateManager.glGetShaderInfoLog(shader, 512) : null;
   }

   public static int createProgram() {
      return GlStateManager.glCreateProgram();
   }

   public static String linkProgram(int program, int vertShader, int fragShader) {
      GlStateManager.glAttachShader(program, vertShader);
      GlStateManager.glAttachShader(program, fragShader);
      GlStateManager.glLinkProgram(program);
      return GlStateManager.glGetProgrami(program, 35714) == 0 ? GlStateManager.glGetProgramInfoLog(program, 512) : null;
   }

   public static void useProgram(int program) {
      GlStateManager._glUseProgram(program);
   }

   public static void viewport(int x, int y, int width, int height) {
      GlStateManager._viewport(x, y, width, height);
   }

   public static int getUniformLocation(int program, String name) {
      return GlStateManager._glGetUniformLocation(program, name);
   }

   public static void uniformInt(int location, int v) {
      GlStateManager._glUniform1i(location, v);
   }

   public static void uniformFloat(int location, float v) {
      GL32C.glUniform1f(location, v);
   }

   public static void uniformFloat2(int location, float v1, float v2) {
      GL32C.glUniform2f(location, v1, v2);
   }

   public static void uniformFloat3(int location, float v1, float v2, float v3) {
      GL32C.glUniform3f(location, v1, v2, v3);
   }

   public static void uniformFloat4(int location, float v1, float v2, float v3, float v4) {
      GL32C.glUniform4f(location, v1, v2, v3, v4);
   }

   public static void uniformFloat3Array(int location, float[] v) {
      GL32C.glUniform3fv(location, v);
   }

   public static void uniformMatrix(int location, Matrix4f v) {
      v.get(MAT);
      GlStateManager._glUniformMatrix4(location, false, MAT);
   }

   public static void pixelStore(int name, int param) {
      GlStateManager._pixelStore(name, param);
   }

   public static void textureParam(int target, int name, int param) {
      GlStateManager._texParameter(target, name, param);
   }

   public static void textureImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
      GL32C.glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
   }

   public static void defaultPixelStore() {
      pixelStore(3312, 0);
      pixelStore(3313, 0);
      pixelStore(3314, 0);
      pixelStore(32878, 0);
      pixelStore(3315, 0);
      pixelStore(3316, 0);
      pixelStore(32877, 0);
      pixelStore(3317, 4);
   }

   public static void generateMipmap(int target) {
      GL32C.glGenerateMipmap(target);
   }

   public static void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
      GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
   }

   public static void clear(int mask) {
      GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager._clear(mask, false);
   }

   public static void enableDepth() {
      GlStateManager._enableDepthTest();
   }

   public static void disableDepth() {
      GlStateManager._disableDepthTest();
   }

   public static void enableBlend() {
      GlStateManager._enableBlend();
      GlStateManager._blendFunc(770, 771);
   }

   public static void disableBlend() {
      GlStateManager._disableBlend();
   }

   public static void enableCull() {
      GlStateManager._enableCull();
   }

   public static void disableCull() {
      GlStateManager._disableCull();
   }

   public static void enableScissorTest() {
      GlStateManager._enableScissorTest();
   }

   public static void disableScissorTest() {
      GlStateManager._disableScissorTest();
   }

   public static void scissorBox(int x, int y, int width, int height) {
      int[] vp = new int[]{0, 0, 0, 0};
      try {
         GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
      } catch (Throwable ignored) {
      }

      int vpX = vp[0];
      int vpY = vp[1];
      int vpW = vp[2] > 0 ? vp[2] : mc.getWindow().getWidth();
      int vpH = vp[3] > 0 ? vp[3] : mc.getWindow().getHeight();

      int guiW = mc.getWindow().getGuiScaledWidth();
      int guiH = mc.getWindow().getGuiScaledHeight();
      if (guiW <= 0 || guiH <= 0) {
         GlStateManager._scissorBox(0, 0, 0, 0);
         return;
      }

      double scaleX = (double)vpW / (double)guiW;
      double scaleY = (double)vpH / (double)guiH;

      int sx = vpX + (int)Math.round((double)x * scaleX);
      int sy = vpY + (int)Math.round((double)(guiH - (y + height)) * scaleY);
      int sw = (int)Math.round((double)width * scaleX);
      int sh = (int)Math.round((double)height * scaleY);

      if (sw < 0) {
         sx += sw;
         sw = -sw;
      }
      if (sh < 0) {
         sy += sh;
         sh = -sh;
      }

      int minX = vpX;
      int minY = vpY;
      int maxX = vpX + vpW;
      int maxY = vpY + vpH;

      if (sx < minX) {
         sw -= (minX - sx);
         sx = minX;
      }
      if (sy < minY) {
         sh -= (minY - sy);
         sy = minY;
      }
      if (sx >= maxX || sy >= maxY || sw <= 0 || sh <= 0) {
         GlStateManager._scissorBox(0, 0, 0, 0);
         return;
      }
      if (sx + sw > maxX) {
         sw = maxX - sx;
      }
      if (sy + sh > maxY) {
         sh = maxY - sy;
      }

      GlStateManager._scissorBox(sx, sy, sw, sh);
   }

   public static void enableScissor(int x, int y, int width, int height) {
      enableScissorTest();
      scissorBox(x, y, width, height);
   }

   public static void enableLineSmooth() {

   }

   public static void disableLineSmooth() {

   }

   public static void bindTexture(ResourceLocation id) {
      GlStateManager._activeTexture(33984);
      mc.getTextureManager().bindForSetup(id);
   }

   public static void bindTexture(int i, int slot) {
      GlStateManager._activeTexture(33984 + slot);
      GlStateManager._bindTexture(i);
   }

   public static void bindTexture(int i) {
      bindTexture(i, 0);
   }

   public static void resetTextureSlot() {
      GlStateManager._activeTexture(33984);
   }

   private static Object getTracker(String fieldName) {
      try {
         Class<?> glStateManager = GlStateManager.class;
         Field field = glStateManager.getDeclaredField(fieldName);
         field.setAccessible(true);
         Object state = field.get(null);
         String trackerName = "com.mojang.blaze3d.platform.GlStateManager$BooleanState";
         Field capStateField = null;

         for (Field f : state.getClass().getDeclaredFields()) {
            if (f.getType().getName().equals(trackerName)) {
               capStateField = f;
               break;
            }
         }

         capStateField.setAccessible(true);
         return capStateField.get(state);
      } catch (IllegalAccessException | NoSuchFieldException var10) {
         return null;
      }
   }

   public static boolean setBooleanStateEnabled(Object tracker, boolean enabled) {
      if (tracker == null) {
         return false;
      }

      try {

         Field enabledField = tracker.getClass().getDeclaredField("enabled");
         enabledField.setAccessible(true);

         enabledField.set(tracker, enabled);
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   public static boolean getBooleanStateEnabled(Object tracker) {
      if (tracker == null) {
         return false;
      }

      try {

         Field enabledField = tracker.getClass().getDeclaredField("enabled");
         enabledField.setAccessible(true);

         return (boolean) enabledField.get(tracker);
      } catch (Exception e) {
         return false;
      }
   }
}
