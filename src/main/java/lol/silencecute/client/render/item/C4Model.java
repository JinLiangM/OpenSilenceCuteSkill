package lol.silencecute.client.render.item;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;

import lol.silencecute.Client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class C4Model {

    public static final ResourceLocation MODEL_FILE = ResourceLocation.fromNamespaceAndPath(
            Client.MOD_ID, "geo_models/throwable/c4_geo.json");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Client.MOD_ID, "textures/throwable/c4_uv.png");

    private static final float ENTITY_TRANSFORM_Y = 5.0F / 16.0F;

    private static final float ITEM_RENDER_CENTER = -0.5F;

    private static final float ITEM_ORIGIN_XZ = 0.5F;

    private static final float ITEM_ORIGIN_Y = 1.5F;

    private static final Set<String> HIDDEN_BONES = new LinkedHashSet<>(List.of(
            "righthand", "righthand_pos", "lefthand", "lefthand_pos", "camera", "idle_view"));

    private static final float PIXEL = 1.0F / 16.0F;

    private static volatile Model cachedModel;

    private C4Model() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Model model = model();
        if (model == null) return;

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        poseStack.pushPose();
        poseStack.translate(0.0D, ENTITY_TRANSFORM_Y, 0.0D);
        poseStack.translate(ITEM_RENDER_CENTER, ITEM_RENDER_CENTER, ITEM_RENDER_CENTER);
        poseStack.translate(ITEM_ORIGIN_XZ, ITEM_ORIGIN_Y, ITEM_ORIGIN_XZ);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        model.render(poseStack, consumer, light, overlay);
        poseStack.popPose();
    }

    public static void invalidate() {
        cachedModel = null;
    }

    private static Model model() {
        Model loaded = cachedModel;
        if (loaded != null) return loaded;
        synchronized (C4Model.class) {
            if (cachedModel == null) {
                cachedModel = loadModel();
            }
            return cachedModel;
        }
    }


    private static final class Model {
        private final int texWidth;
        private final int texHeight;
        private final List<Part> roots;

        private Model(int texWidth, int texHeight, List<Part> roots) {
            this.texWidth = texWidth;
            this.texHeight = texHeight;
            this.roots = roots;
        }

        private void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
            for (Part root : this.roots) {
                root.render(poseStack, consumer, light, overlay, this.texWidth, this.texHeight);
            }
        }
    }

    private static final class Part {
        private final String name;
        private boolean hidden;
        private float x;
        private float y;
        private float z;
        private float xRot;
        private float yRot;
        private float zRot;
        private final List<Cube> cubes = new ArrayList<>();
        private final List<Part> children = new ArrayList<>();

        private Part(String name) {
            this.name = name;
        }

        private void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                            int texWidth, int texHeight) {
            if (this.hidden) return;
            if (this.cubes.isEmpty() && this.children.isEmpty()) return;

            poseStack.pushPose();
            poseStack.translate(this.x * PIXEL, this.y * PIXEL, this.z * PIXEL);
            if (this.zRot != 0.0F) poseStack.mulPose(Axis.ZP.rotation(this.zRot));
            if (this.yRot != 0.0F) poseStack.mulPose(Axis.YP.rotation(this.yRot));
            if (this.xRot != 0.0F) poseStack.mulPose(Axis.XP.rotation(this.xRot));
            for (Cube cube : this.cubes) {
                cube.render(poseStack, consumer, light, overlay, texWidth, texHeight);
            }
            for (Part child : this.children) {
                child.render(poseStack, consumer, light, overlay, texWidth, texHeight);
            }
            poseStack.popPose();
        }
    }

    private static final class Cube {
        private float x;
        private float y;
        private float z;
        private float width;
        private float height;
        private float depth;
        private float delta;
        private boolean mirror;

        private boolean perFace;

        private float boxU;
        private float boxV;

        private final boolean[] facePresent = new boolean[6];
        private final float[][] faceUV = new float[6][4];

        private boolean rotated;
        private float pivotX;
        private float pivotY;
        private float pivotZ;
        private float rotX;
        private float rotY;
        private float rotZ;

        private void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                            int texWidth, int texHeight) {
            if (this.rotated) {
                poseStack.pushPose();
                poseStack.translate(this.pivotX * PIXEL, this.pivotY * PIXEL, this.pivotZ * PIXEL);
                if (this.rotZ != 0.0F) poseStack.mulPose(Axis.ZP.rotation(this.rotZ));
                if (this.rotY != 0.0F) poseStack.mulPose(Axis.YP.rotation(this.rotY));
                if (this.rotX != 0.0F) poseStack.mulPose(Axis.XP.rotation(this.rotX));
            }

            float minX = this.x - this.delta;
            float minY = this.y - this.delta;
            float minZ = this.z - this.delta;
            float maxX = this.x + this.width + this.delta;
            float maxY = this.y + this.height + this.delta;
            float maxZ = this.z + this.depth + this.delta;

            if (this.mirror && !this.perFace) {
                float tmp = maxX;
                maxX = minX;
                minX = tmp;
            }

            float[][] v = new float[8][];
            v[0] = new float[]{minX, minY, minZ};
            v[1] = new float[]{maxX, minY, minZ};
            v[2] = new float[]{maxX, maxY, minZ};
            v[3] = new float[]{minX, maxY, minZ};
            v[4] = new float[]{minX, minY, maxZ};
            v[5] = new float[]{maxX, minY, maxZ};
            v[6] = new float[]{maxX, maxY, maxZ};
            v[7] = new float[]{minX, maxY, maxZ};

            emit(poseStack, consumer, light, overlay, texWidth, texHeight, v, new int[]{5, 4, 0, 1}, Direction.DOWN);
            emit(poseStack, consumer, light, overlay, texWidth, texHeight, v, new int[]{2, 3, 7, 6}, Direction.UP);
            emit(poseStack, consumer, light, overlay, texWidth, texHeight, v, new int[]{1, 0, 3, 2}, Direction.NORTH);
            emit(poseStack, consumer, light, overlay, texWidth, texHeight, v, new int[]{4, 5, 6, 7}, Direction.SOUTH);
            emit(poseStack, consumer, light, overlay, texWidth, texHeight, v, new int[]{0, 4, 7, 3}, Direction.WEST);
            emit(poseStack, consumer, light, overlay, texWidth, texHeight, v, new int[]{5, 1, 2, 6}, Direction.EAST);

            if (this.rotated) {
                poseStack.popPose();
            }
        }

        private void emit(PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                          int texWidth, int texHeight, float[][] vertices, int[] order, Direction direction) {
            float u1 = 0.0F;
            float v1 = 0.0F;
            float u2 = 0.0F;
            float v2 = 0.0F;

            if (this.perFace) {
                int index = direction.get3DDataValue();
                if (!this.facePresent[index]) return;
                u1 = this.faceUV[index][0] / texWidth;
                v1 = this.faceUV[index][1] / texHeight;
                u2 = (this.faceUV[index][0] + this.faceUV[index][2]) / texWidth;
                v2 = (this.faceUV[index][1] + this.faceUV[index][3]) / texHeight;
            } else {
                int dx = (int) this.width;
                int dy = (int) this.height;
                int dz = (int) this.depth;
                float p1 = this.boxU + dz;
                float p2 = this.boxU + dz + dx;
                float p3 = this.boxU + dz + dx + dx;
                float p4 = this.boxU + dz + dx + dz;
                float p5 = this.boxU + dz + dx + dz + dx;
                float p6 = this.boxV + dz;
                float p7 = this.boxV + dz + dy;
                float p8 = this.boxV;
                float p9 = this.boxU;
                switch (direction) {
                    case DOWN -> {
                        u1 = p1;
                        v1 = p8;
                        u2 = p2;
                        v2 = p6;
                    }
                    case UP -> {
                        u1 = p2;
                        v1 = p6;
                        u2 = p3;
                        v2 = p8;
                    }
                    case WEST -> {
                        u1 = p9;
                        v1 = p6;
                        u2 = p1;
                        v2 = p7;
                    }
                    case NORTH -> {
                        u1 = p1;
                        v1 = p6;
                        u2 = p2;
                        v2 = p7;
                    }
                    case EAST -> {
                        u1 = p2;
                        v1 = p6;
                        u2 = p4;
                        v2 = p7;
                    }
                    case SOUTH -> {
                        u1 = p4;
                        v1 = p6;
                        u2 = p5;
                        v2 = p7;
                    }
                    default -> {
                        return;
                    }
                }
                u1 /= texWidth;
                v1 /= texHeight;
                u2 /= texWidth;
                v2 /= texHeight;
            }

            float[] u = new float[]{u2, u1, u1, u2};
            float[] vv = new float[]{v1, v1, v2, v2};

            int[] indices = order;
            if (this.mirror && !this.perFace) {
                indices = new int[]{order[3], order[2], order[1], order[0]};
            }

            PoseStack.Pose pose = poseStack.last();
            Vector3f normal = new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            if (this.mirror && !this.perFace) {
                normal.mul(-1.0F, 1.0F, 1.0F);
            }
            normal.mul(pose.normal());

            for (int i = 0; i < 4; i++) {
                float[] vertex = vertices[indices[i]];
                Vector4f pos = new Vector4f(vertex[0] * PIXEL, vertex[1] * PIXEL, vertex[2] * PIXEL, 1.0F);
                pos.mul(pose.pose());
                consumer.vertex(pos.x(), pos.y(), pos.z(),
                        1.0F, 1.0F, 1.0F, 1.0F,
                        u[i], vv[i],
                        overlay, light,
                        normal.x(), normal.y(), normal.z());
            }
        }
    }


    private static Model loadModel() {
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(MODEL_FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject description = geometry.getAsJsonObject("description");
            int texWidth = description.get("texture_width").getAsInt();
            int texHeight = description.get("texture_height").getAsInt();

            Map<String, JsonObject> boneJson = new LinkedHashMap<>();
            for (JsonElement element : geometry.getAsJsonArray("bones")) {
                JsonObject bone = element.getAsJsonObject();
                boneJson.put(bone.get("name").getAsString(), bone);
            }

            Map<String, Part> parts = new LinkedHashMap<>();
            for (String name : boneJson.keySet()) {
                Part part = new Part(name);
                part.hidden = HIDDEN_BONES.contains(name);
                parts.put(name, part);
            }

            List<Part> roots = new ArrayList<>();
            for (Map.Entry<String, JsonObject> entry : boneJson.entrySet()) {
                JsonObject bone = entry.getValue();
                Part part = parts.get(entry.getKey());

                float[] pivot = vector(bone.getAsJsonArray("pivot"), 3);
                String parentName = bone.has("parent") ? bone.get("parent").getAsString() : null;
                JsonObject parent = parentName == null ? null : boneJson.get(parentName);
                float[] parentPivot = parent == null ? null : vector(parent.getAsJsonArray("pivot"), 3);

                part.x = parentPivot == null ? pivot[0] : pivot[0] - parentPivot[0];
                part.y = parentPivot == null ? 24.0F - pivot[1] : parentPivot[1] - pivot[1];
                part.z = parentPivot == null ? pivot[2] : pivot[2] - parentPivot[2];

                if (bone.has("rotation")) {
                    float[] rotation = vector(bone.getAsJsonArray("rotation"), 3);
                    part.xRot = radians(rotation[0]);
                    part.yRot = radians(rotation[1]);
                    part.zRot = radians(rotation[2]);
                }

                if (parent == null) {
                    roots.add(part);
                } else {
                    parts.get(parentName).children.add(part);
                }

                JsonArray cubes = bone.getAsJsonArray("cubes");
                if (cubes == null) continue;

                for (JsonElement cubeElement : cubes) {
                    part.cubes.add(parseCube(cubeElement.getAsJsonObject(), pivot));
                }
            }

            return new Model(texWidth, texHeight, roots);
        } catch (IOException | RuntimeException exception) {
            LogUtils.getLogger().error("Failed to load C4 model {}", MODEL_FILE, exception);
            return new Model(64, 64, List.of());
        }
    }

    private static Cube parseCube(JsonObject json, float[] bonePivot) {
        Cube cube = new Cube();
        float[] origin = vector(json.getAsJsonArray("origin"), 3);
        float[] size = vector(json.getAsJsonArray("size"), 3);
        cube.delta = json.has("inflate") ? json.get("inflate").getAsFloat() : 0.0F;
        cube.mirror = json.has("mirror") && json.get("mirror").getAsBoolean();
        cube.width = size[0];
        cube.height = size[1];
        cube.depth = size[2];

        JsonObject uv = json.getAsJsonObject("uv");
        boolean perFace = uv != null && (uv.has("north") || uv.has("south") || uv.has("east")
                || uv.has("west") || uv.has("up") || uv.has("down"));
        cube.perFace = perFace;

        if (json.has("rotation")) {
            float[] cubePivot = vector(json.getAsJsonArray("pivot"), 3);
            float[] rotation = vector(json.getAsJsonArray("rotation"), 3);
            cube.rotated = true;
            cube.rotX = radians(rotation[0]);
            cube.rotY = radians(rotation[1]);
            cube.rotZ = radians(rotation[2]);
            cube.pivotX = cubePivot[0] - bonePivot[0];
            cube.pivotY = bonePivot[1] - cubePivot[1];
            cube.pivotZ = cubePivot[2] - bonePivot[2];

            cube.x = origin[0] - cubePivot[0];
            cube.y = cubePivot[1] - origin[1] - size[1];
            cube.z = origin[2] - cubePivot[2];
        } else {
            cube.x = origin[0] - bonePivot[0];
            cube.y = bonePivot[1] - origin[1] - size[1];
            cube.z = origin[2] - bonePivot[2];
        }

        if (perFace) {
            for (Direction direction : Direction.values()) {
                JsonObject face = uv.getAsJsonObject(direction.getName());
                if (face == null) continue;
                float[] faceUv = vector(face.getAsJsonArray("uv"), 2);
                float[] faceSize = vector(face.getAsJsonArray("uv_size"), 2);
                int index = direction.get3DDataValue();
                cube.facePresent[index] = true;
                cube.faceUV[index] = new float[]{faceUv[0], faceUv[1], faceSize[0], faceSize[1]};
            }
        } else {
            float[] boxUv = uv == null ? null : vector(uv.getAsJsonArray("uv"), 2);
            cube.boxU = boxUv == null ? 0.0F : boxUv[0];
            cube.boxV = boxUv == null ? 0.0F : boxUv[1];
        }

        return cube;
    }

    private static float radians(float degrees) {
        return (float) (degrees * Math.PI / 180.0D);
    }

    private static float[] vector(JsonArray array, int length) {
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            result[i] = array != null && array.size() > i ? array.get(i).getAsFloat() : 0.0F;
        }
        return result;
    }
}
