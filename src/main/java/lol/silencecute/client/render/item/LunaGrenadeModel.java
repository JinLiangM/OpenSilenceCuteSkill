package lol.silencecute.client.render.item;

import lol.silencecute.Client;
import lol.silencecute.client.anim.LunaGrenadeAnimation;
import java.io.BufferedReader;

import java.io.IOException;

import java.util.ArrayList;

import java.util.List;

import java.util.LinkedHashMap;

import java.util.Map;

import lol.silencecute.client.anim.LunaGrenadeAnimation.Transform;

import lol.silencecute.client.anim.LunaGrenadeAnimation.Vector;

import net.minecraft.client.player.AbstractClientPlayer;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import com.google.gson.JsonArray;

import com.google.gson.JsonElement;

import com.google.gson.JsonObject;

import com.google.gson.JsonParser;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexConsumer;

import com.mojang.logging.LogUtils;

import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;

import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.texture.OverlayTexture;

import net.minecraft.resources.ResourceLocation;

public final class LunaGrenadeModel {

    public static final ResourceLocation MODEL_FILE = ResourceLocation.fromNamespaceAndPath(

            Client.MOD_ID, "geo_models/throwable/m67_grenade_geo.json");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(

            Client.MOD_ID, "textures/throwable/m67_uv.png");

    private static volatile ModelData cachedModel;

    private LunaGrenadeModel() {

    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, int light,

            boolean pinPulled) {

        ModelData model = model();

        if (model == null) {

            return;

        }

        poseStack.pushPose();

        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        for (Cube cube : model.cubes) {

            if (!pinPulled || !cube.ring) {

                cube.render(poseStack, consumer, light, model.textureWidth, model.textureHeight);

            }

        }

        poseStack.popPose();

    }

    public static void invalidate() {

        cachedModel = null;

        LunaGrenadeAnimation.invalidate();

    }

    

    public static void renderFirstPerson(PoseStack stack, MultiBufferSource buffers, int light,

                                         AbstractClientPlayer player, Map<String, Transform> animation) {

        ModelData model = model();

        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player)

                instanceof PlayerRenderer renderer)) {

            return;

        }

        Bone view = model.bones.get("idle_view");

        if (view == null) {

            return;

        }

        stack.pushPose();

        Vector camera = animation.getOrDefault("camera", LunaGrenadeAnimation.IDENTITY).rotation();

        rotate(stack, camera.x(), camera.y(), camera.z());

        stack.translate(0, 1.5, 0);

        stack.mulPose(Axis.ZP.rotationDegrees(180));

        stack.translate(-view.pivot[0] / 16F, view.pivot[1] / 16F, -view.pivot[2] / 16F);

        for (Bone bone : model.bones.values()) {

            if (bone.parent == null && !bone.name.equals("camera") && !bone.name.equals("idle_view")) {

                renderBone(bone, stack, buffers, light, player, renderer, model, animation);

            }

        }

        stack.popPose();

    }

    private static void renderBone(Bone bone, PoseStack stack, MultiBufferSource buffers, int light,

            AbstractClientPlayer player, PlayerRenderer renderer, ModelData model,

            Map<String, Transform> animation) {

        Transform transform = animation.getOrDefault(bone.name, LunaGrenadeAnimation.IDENTITY);

        Vector scale = transform.scale();

        if (scale.hidden()) {

            return;

        }

        stack.pushPose();

        Bone parent = model.bones.get(bone.parent);

        float[] pivot = bone.pivot;

        Vector position = transform.position();

        stack.translate((pivot[0] - (parent == null ? 0 : parent.pivot[0]) + position.x()) / 16F,

                ((parent == null ? 24 : parent.pivot[1]) - pivot[1] - position.y()) / 16F,

                (pivot[2] - (parent == null ? 0 : parent.pivot[2]) + position.z()) / 16F);

        rotate(stack, bone.rotation[0], bone.rotation[1], bone.rotation[2]);

        Vector rotation = transform.rotation();

        rotate(stack, rotation.x(), rotation.y(), rotation.z());

        stack.scale(scale.x(), scale.y(), scale.z());

        if (bone.name.equals("righthand_pos") || bone.name.equals("lefthand_pos")) {

            stack.mulPose(Axis.ZP.rotationDegrees(180));

            if (!player.isInvisible()) {

                if (bone.name.equals("righthand_pos")) {

                    renderer.renderRightHand(stack, buffers, light, player);

                } else {

                    renderer.renderLeftHand(stack, buffers, light, player);

                }

            }

        } else {

            if (!bone.cubes.isEmpty()) {

                VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

                for (Cube cube : bone.cubes) {

                    cube.renderBedrock(stack, consumer, light, model.textureWidth, model.textureHeight, pivot);

                }

            }

            for (Bone child : model.bones.values()) {

                if (bone.name.equals(child.parent)) {

                    renderBone(child, stack, buffers, light, player, renderer, model, animation);

                }

            }

        }

        stack.popPose();

    }

    private static void rotate(PoseStack stack, float x, float y, float z) {

        stack.mulPose(Axis.ZP.rotationDegrees(z));

        stack.mulPose(Axis.YP.rotationDegrees(y));

        stack.mulPose(Axis.XP.rotationDegrees(x));

    }

    private static ModelData model() {

        ModelData loaded = cachedModel;

        if (loaded != null) {

            return loaded;

        }

        synchronized (LunaGrenadeModel.class) {

            if (cachedModel == null) {

                cachedModel = loadModel();

            }

            return cachedModel;

        }

    }

    private static ModelData loadModel() {

        try (BufferedReader reader = Minecraft.getInstance().getResourceManager()

                .openAsReader(MODEL_FILE)) {

            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            JsonObject geometry = root.getAsJsonArray("minecraft:geometry")

                    .get(0).getAsJsonObject();

            JsonObject description = geometry.getAsJsonObject("description");

            float textureWidth = description.get("texture_width").getAsFloat();

            float textureHeight = description.get("texture_height").getAsFloat();

            List<Cube> cubes = new ArrayList<>();

            Map<String, Bone> bones = new LinkedHashMap<>();

            for (JsonElement boneElement : geometry.getAsJsonArray("bones")) {

                JsonObject bone = boneElement.getAsJsonObject();

                String boneName = bone.get("name").getAsString();

                List<Cube> localCubes = new ArrayList<>();

                JsonArray boneCubes = bone.getAsJsonArray("cubes");

                if (boneCubes != null) {

                    for (JsonElement cubeElement : boneCubes) {

                        Cube cube = Cube.parse(cubeElement.getAsJsonObject(), boneName.equals("ring"));

                        localCubes.add(cube);

                        if (boneName.equals("base") || boneName.equals("ring")) {

                            cubes.add(cube);

                        }

                    }

                }

                bones.put(boneName, new Bone(boneName,

                        bone.has("parent") ? bone.get("parent").getAsString() : null,

                        vector(bone.getAsJsonArray("pivot"), 3, 0),

                        vector(bone.getAsJsonArray("rotation"), 3, 0), localCubes));

            }

            return new ModelData(textureWidth, textureHeight, cubes, bones);

        } catch (IOException | RuntimeException exception) {

            LogUtils.getLogger().error("Failed to load Luna grenade model {}", MODEL_FILE, exception);

            return new ModelData(16.0F, 16.0F, List.of(), Map.of());

        }

    }

    private record ModelData(float textureWidth, float textureHeight, List<Cube> cubes,

            Map<String, Bone> bones) {

    }

    private record Bone(String name, String parent, float[] pivot, float[] rotation, List<Cube> cubes) {

    }

    private record Face(float u, float v, float width, float height) {

        private static Face parse(JsonObject uv, String key) {

            if (uv == null || !uv.has(key)) {

                return null;

            }

            JsonObject face = uv.getAsJsonObject(key);

            float[] position = vector(face.getAsJsonArray("uv"), 2, 0.0F);

            float[] size = vector(face.getAsJsonArray("uv_size"), 2, 0.0F);

            return new Face(position[0], position[1], size[0], size[1]);

        }

    }

    private record Cube(float minX, float minY, float minZ,

            float maxX, float maxY, float maxZ,

            float pivotX, float pivotY, float pivotZ,

            float rotationX, float rotationY, float rotationZ,

            Face north, Face south, Face west, Face east, Face up, Face down,

            boolean ring) {

        private static Cube parse(JsonObject json, boolean ring) {

            float[] origin = vector(json.getAsJsonArray("origin"), 3, 0.0F);

            float[] size = vector(json.getAsJsonArray("size"), 3, 0.0F);

            float inflate = json.has("inflate") ? json.get("inflate").getAsFloat() : 0.0F;

            float[] pivot = json.has("pivot")

                    ? vector(json.getAsJsonArray("pivot"), 3, 0.0F)

                    : new float[] {0.0F, 0.0F, 0.0F};

            float[] rotation = json.has("rotation")

                    ? vector(json.getAsJsonArray("rotation"), 3, 0.0F)

                    : new float[] {0.0F, 0.0F, 0.0F};

            JsonObject uv = json.getAsJsonObject("uv");

            return new Cube(origin[0] - inflate, origin[1] - inflate, origin[2] - inflate,

                    origin[0] + size[0] + inflate,

                    origin[1] + size[1] + inflate,

                    origin[2] + size[2] + inflate,

                    pivot[0], pivot[1], pivot[2],

                    rotation[0], rotation[1], rotation[2],

                    Face.parse(uv, "north"), Face.parse(uv, "south"),

                    Face.parse(uv, "west"), Face.parse(uv, "east"),

                    Face.parse(uv, "up"), Face.parse(uv, "down"), ring);

        }

        private void renderBedrock(PoseStack stack, VertexConsumer consumer, int light,

                float textureWidth, float textureHeight, float[] bonePivot) {

            stack.pushPose();

            stack.translate((pivotX - bonePivot[0]) / 16F, (bonePivot[1] - pivotY) / 16F,

                    (pivotZ - bonePivot[2]) / 16F);

            rotate(stack, rotationX, rotationY, rotationZ);

            float x0 = (minX - pivotX) / 16F;

            float y0 = (pivotY - maxY) / 16F;

            float z0 = (minZ - pivotZ) / 16F;

            float x1 = (maxX - pivotX) / 16F;

            float y1 = (pivotY - minY) / 16F;

            float z1 = (maxZ - pivotZ) / 16F;

            quad(stack, consumer, light, north, textureWidth, textureHeight,

                    x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0, 0, -1);

            quad(stack, consumer, light, south, textureWidth, textureHeight,

                    x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0, 0, 1);

            quad(stack, consumer, light, west, textureWidth, textureHeight,

                    x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, -1, 0, 0);

            quad(stack, consumer, light, east, textureWidth, textureHeight,

                    x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, 1, 0, 0);

            quad(stack, consumer, light, up, textureWidth, textureHeight,

                    x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0);

            quad(stack, consumer, light, down, textureWidth, textureHeight,

                    x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0);

            stack.popPose();

        }

        private void render(PoseStack stack, VertexConsumer consumer, int light,

                float textureWidth, float textureHeight) {

            stack.pushPose();

            stack.translate(pivotX / 16.0F, pivotY / 16.0F, pivotZ / 16.0F);

            if (rotationZ != 0.0F) {

                stack.mulPose(Axis.ZP.rotationDegrees(rotationZ));

            }

            if (rotationY != 0.0F) {

                stack.mulPose(Axis.YP.rotationDegrees(rotationY));

            }

            if (rotationX != 0.0F) {

                stack.mulPose(Axis.XP.rotationDegrees(rotationX));

            }

            stack.translate(-pivotX / 16.0F, -pivotY / 16.0F, -pivotZ / 16.0F);

            float x0 = minX / 16.0F;

            float y0 = minY / 16.0F;

            float z0 = minZ / 16.0F;

            float x1 = maxX / 16.0F;

            float y1 = maxY / 16.0F;

            float z1 = maxZ / 16.0F;

            quad(stack, consumer, light, north, textureWidth, textureHeight,

                    x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1);

            quad(stack, consumer, light, south, textureWidth, textureHeight,

                    x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1);

            quad(stack, consumer, light, west, textureWidth, textureHeight,

                    x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0);

            quad(stack, consumer, light, east, textureWidth, textureHeight,

                    x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0);

            quad(stack, consumer, light, up, textureWidth, textureHeight,

                    x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0);

            quad(stack, consumer, light, down, textureWidth, textureHeight,

                    x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0);

            stack.popPose();

        }

    }

    private static void quad(PoseStack stack, VertexConsumer consumer, int light,

            Face face, float textureWidth, float textureHeight,

            float x0, float y0, float z0, float x1, float y1, float z1,

            float x2, float y2, float z2, float x3, float y3, float z3,

            float normalX, float normalY, float normalZ) {

        if (face == null) {

            return;

        }

        float u0 = face.u / textureWidth;

        float v0 = face.v / textureHeight;

        float u1 = (face.u + face.width) / textureWidth;

        float v1 = (face.v + face.height) / textureHeight;

        vertex(stack, consumer, light, x0, y0, z0, u0, v1, normalX, normalY, normalZ);

        vertex(stack, consumer, light, x1, y1, z1, u1, v1, normalX, normalY, normalZ);

        vertex(stack, consumer, light, x2, y2, z2, u1, v0, normalX, normalY, normalZ);

        vertex(stack, consumer, light, x3, y3, z3, u0, v0, normalX, normalY, normalZ);

    }

    private static void vertex(PoseStack stack, VertexConsumer consumer, int light,

            float x, float y, float z, float u, float v,

            float normalX, float normalY, float normalZ) {

        PoseStack.Pose pose = stack.last();

        consumer.vertex(pose.pose(), x, y, z)

                .color(255, 255, 255, 255)

                .uv(u, v)

                .overlayCoords(OverlayTexture.NO_OVERLAY)

                .uv2(light)

                .normal(pose.normal(), normalX, normalY, normalZ)

                .endVertex();

    }

    private static float[] vector(JsonArray array, int length, float fallback) {

        float[] result = new float[length];

        for (int index = 0; index < length; index++) {

            result[index] = array != null && array.size() > index

                    ? array.get(index).getAsFloat() : fallback;

        }

        return result;

    }

}

