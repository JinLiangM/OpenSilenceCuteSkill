package lol.silencecute.client.render.world;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import lol.silencecute.Client;
import lol.silencecute.entity.LunaGrenadeEntity;
import lol.silencecute.entity.MagneticBombEntity;
import lol.silencecute.entity.LunaShockArrowEntity;
import lol.silencecute.manager.LunaShockArrowService;
import lol.silencecute.client.font.ModFonts;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class ThreatRadar {

    private static final ResourceLocation TEX_ARROW = tex("arrowba.png");
    private static final float  ARROW_ICON_SCALE = 0.85F;
    private static final double ARROW_ICON_Y = 14.0D;
    private static final long   ARROW_BLINK_MS = 200L;
    private static final int    ARROW_RED  = 0xFFFF2D2D;
    private static final int    ARROW_IDLE = 0xFFFFFFFF;

    private static final ResourceLocation TEX_GRENADE = tex("luna_grenade_countdown.png");
    private static final double GRENADE_RANGE   = 15.0D;
    private static final double GRENADE_VISIBLE = 13.0D;
    private static final float  GRENADE_ICON_SCALE = 0.85F;
    private static final double GRENADE_ICON_Y = 15.0D;

    private static final float ARROW_SCALE = 0.55F;
    private static final float TEXT_SCALE  = 0.6F;

    private ThreatRadar() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Client.MOD_ID, "textures/gui/" + name);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();
        int cx = screenW / 2;
        int cy = screenH / 2;

        Camera camera = mc.gameRenderer.getMainCamera();
        View view = new View(
                camera.getPosition(),
                new Vec3(camera.getLookVector()),
                new Vec3(camera.getUpVector()),
                new Vec3(camera.getLeftVector()).reverse(),
                Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0),
                (double) screenW / screenH,
                Math.min(screenW, screenH) / 2 - 60);

        renderArrows(g, mc, view, cx, cy);
        renderGrenades(g, mc, view, cx, cy);
        renderBombs(g, mc, view, cx, cy);
    }

    private static void renderArrows(GuiGraphics g, Minecraft mc, View view, int cx, int cy) {
        for (LunaShockArrowEntity arrow : mc.level.getEntitiesOfClass(
                LunaShockArrowEntity.class,
                mc.player.getBoundingBox().inflate(arrowWarning() + 4.0D))) {
            if (!arrow.isLanded() || arrow.isRemoved()) continue;

            Vec3 pos = arrow.position().add(0.0D, 0.10D, 0.0D);
            double dist = view.camPos.distanceTo(pos);
            if (dist > arrowWarning() || isOccluded(mc, view.camPos, pos)) continue;

            boolean red = dist <= arrowDanger()
                    && (System.currentTimeMillis() / ARROW_BLINK_MS) % 2L == 0L;

            drawThreat(g, view, cx, cy, pos, TEX_ARROW, ARROW_ICON_SCALE, ARROW_ICON_Y,
                    red ? ARROW_RED : ARROW_IDLE, null);
        }
    }

    private static void renderGrenades(GuiGraphics g, Minecraft mc, View view, int cx, int cy) {
        for (LunaGrenadeEntity grenade : mc.level.getEntitiesOfClass(
                LunaGrenadeEntity.class,
                mc.player.getBoundingBox().inflate(GRENADE_RANGE))) {
            if (grenade.getFuse() <= 0 || grenade.isRemoved()) continue;

            Vec3 pos = grenade.position().add(0, 0.25, 0);
            double dist = view.camPos.distanceTo(pos);
            if (dist > GRENADE_VISIBLE || isOccluded(mc, view.camPos, pos)) continue;

            int tint = grenadeTint(dist);
            String distText = String.format("%.1fm", dist);

            drawThreat(g, view, cx, cy, pos, TEX_GRENADE, GRENADE_ICON_SCALE, GRENADE_ICON_Y,
                    tint, distText);
        }
    }

    private static int grenadeTint(double dist) {
        if (dist > 8.0D) return 0xFFFFFFFF;
        long period = dist <= 2.0D ? 40L : dist <= 4.0D ? 90L : dist <= 6.0D ? 160L : 280L;
        double sin = Math.sin(System.currentTimeMillis() / (double) period * Math.PI);
        float factor = (float) ((sin + 1.0) / 2.0);
        int g = (int) (255 * factor);
        int b = (int) (255 * factor);
        return 0xFF000000 | (255 << 16) | (g << 8) | b;
    }

    private record View(Vec3 camPos, Vec3 look, Vec3 up, Vec3 right,
                        double halfFovTan, double aspect, int radarRadius) {}

    private record Projection(boolean inView, double nx, double ny, double angle) {}

    private static Projection project(View view, Vec3 world) {
        Vec3 rel = world.subtract(view.camPos);
        double z = rel.dot(view.look);
        double x = rel.dot(view.right);
        double y = rel.dot(view.up);
        double angle = Math.atan2(x, z);
        if (z > 0.1D) {
            double nx = (x / z) / (view.halfFovTan * view.aspect);
            double ny = (y / z) / view.halfFovTan;
            if (Math.abs(nx) <= 1.0D && Math.abs(ny) <= 1.0D) {
                return new Projection(true, nx, ny, angle);
            }
        }
        return new Projection(false, 0, 0, angle);
    }

    private static boolean isOccluded(Minecraft mc, Vec3 from, Vec3 to) {
        BlockHitResult hit = mc.level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK
                && hit.getLocation().distanceToSqr(from) < to.distanceToSqr(from) - 0.3D;
    }

    private static void drawThreat(GuiGraphics g, View view, int cx, int cy, Vec3 world,
                                   ResourceLocation tex, float scale, double iconY,
                                   int tint, String distText) {
        Projection p = project(view, world);
        if (p.inView) {
            drawIcon(g, tex, cx + p.nx * cx, cy - p.ny * cy - iconY, scale, tint);
            return;
        }
        double sin = Math.sin(p.angle);
        double cos = Math.cos(p.angle);
        drawEdgeArrow(g, cx + sin * view.radarRadius, cy - cos * view.radarRadius,
                p.angle, tint);
        drawIcon(g, tex,
                cx + sin * (view.radarRadius + 15.0D),
                cy - cos * (view.radarRadius + 15.0D), scale, tint);
        if (distText != null) {
            drawCenteredText(g, distText,
                    cx + sin * (view.radarRadius + 30.0D),
                    cy - cos * (view.radarRadius + 30.0D), tint);
        }
    }


    private static final ResourceLocation TEX_C4 = tex("c4.png");
    private static final ResourceLocation TEX_C4_RING = tex("c4_alert_ring.png");
    private static final float C4_ICON_SCALE = 0.85F;
    private static final double C4_ICON_Y = 15.0D;
    private static final int C4_RED = 0xFFFF2D2D;
    private static final float ALERT_RING_SIZE = 48.0F;
    private static final float ALERT_ICON_SCALE = 2.0F;
    private static final double ALERT_OFFSET_Y = 54.0D;

    private static void renderBombs(GuiGraphics g, Minecraft mc, View view, int cx, int cy) {
        double blastRange = MagneticBombEntity.DAMAGE_RADIUS;
        boolean onMe = false;

        for (MagneticBombEntity bomb : mc.level.getEntitiesOfClass(MagneticBombEntity.class,
                mc.player.getBoundingBox().inflate(blastRange + 4.0D))) {
            if (bomb.isRemoved() || !bomb.isStuck()) continue;

            if (bomb.getStuckEntity() == mc.player) {
                onMe = true;
                continue;
            }

            Vec3 pos = bomb.position().add(0.0D, 0.10D, 0.0D);
            double dist = view.camPos.distanceTo(pos);
            if (dist > blastRange) continue;

            int tint = (System.currentTimeMillis() / 150L) % 2L == 0L ? C4_RED : 0xFFFFFFFF;
            drawThreat(g, view, cx, cy, pos, TEX_C4, C4_ICON_SCALE, C4_ICON_Y, tint,
                    String.format("%.1fm", dist));
        }

        if (onMe) {
            drawStuckAlert(g, cx, cy);
        }
    }

    private static void drawStuckAlert(GuiGraphics g, int cx, int cy) {
        long now = System.currentTimeMillis();
        double pulse = 0.70D + 0.30D * (0.5D + 0.5D * Math.sin(now / 130.0D));
        float red = 0.16F;
        double alertY = cy - ALERT_OFFSET_Y;
        float half = ALERT_RING_SIZE / 2.0F;

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);

        RenderSystem.setShaderColor(1.0F, red, red, (float) (pulse * 0.95D));
        g.pose().pushPose();
        g.pose().translate((double) cx, alertY, 0.0D);
        g.blit(TEX_C4_RING, (int) -half, (int) -half, (int) ALERT_RING_SIZE, (int) ALERT_RING_SIZE,
                0, 0, 128, 128, 128, 128);
        g.pose().popPose();

        RenderSystem.setShaderColor(1.0F, red, red, (float) (0.72D + 0.28D * pulse));
        g.pose().pushPose();
        g.pose().translate((double) cx, alertY, 0.0D);
        g.pose().scale(ALERT_ICON_SCALE, ALERT_ICON_SCALE, 1.0F);
        g.blit(TEX_C4, -8, -8, 16, 16, 0, 0, 16, 16, 16, 16);
        g.pose().popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    private static double arrowDanger() {
        return LunaShockArrowService.FIELD_RADIUS;
    }

    private static double arrowWarning() {
        return arrowDanger() + 3.0D;
    }

    private static void drawIcon(GuiGraphics g, ResourceLocation tex, double x, double y,
                                 float scale, int tint) {
        RenderSystem.setShaderColor(
                ((tint >> 16) & 0xFF) / 255.0F,
                ((tint >> 8) & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F,
                1.0F);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.blit(tex, -8, -8, 16, 16, 0, 0, 16, 16, 16, 16);
        g.pose().popPose();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawEdgeArrow(GuiGraphics g, double tipX, double tipY,
                                      double angle, int color) {
        g.pose().pushPose();
        g.pose().translate(tipX, tipY, 0);
        g.pose().scale(ARROW_SCALE, ARROW_SCALE, 1.0F);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) angle));
        g.pose().pushPose();
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(30.0F));
        g.fill(0, 0, 1, 10, color);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-30.0F));
        g.fill(-1, 0, 0, 10, color);
        g.pose().popPose();
        g.pose().popPose();
    }

    private static void drawCenteredText(GuiGraphics g, String text, double x, double y, int color) {
        var font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        ModFonts.drawString(g, text, -ModFonts.width(text) / 2, 0, color, true);
        g.pose().popPose();
    }
}