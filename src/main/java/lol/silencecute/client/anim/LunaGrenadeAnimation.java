package lol.silencecute.client.anim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import lol.silencecute.Client;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class LunaGrenadeAnimation {
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(
            Client.MOD_ID, "animations/throwable/m67_grenade.animation.json");
    private static Map<String, Clip> clips;
    private static final Vector ZERO = new Vector(0, 0, 0);
    private static final Vector ONE = new Vector(1, 1, 1);
    public static final Transform IDENTITY = new Transform(ZERO, ZERO, ONE);

    private LunaGrenadeAnimation() {
    }

    public static void invalidate() {
        clips = null;
    }

    public static float duration(String name) {
        Clip clip = clips().get(name);
        return clip == null ? 0 : clip.duration;
    }

    public static Map<String, Transform> sample(String name, float seconds, boolean loop) {
        Map<String, Transform> result = new HashMap<>();
        apply(result, clips().get("static_idle"), 0);
        Clip clip = clips().get(name);
        if (clip != null) {
            float time = Math.max(0, seconds);
            time = loop && clip.duration > 0 ? time % clip.duration : Math.min(time, clip.duration);
            apply(result, clip, time);
        }
        return result;
    }

    public static Map<String, Transform> blend(Map<String, Transform> from,
                                               Map<String, Transform> to, float amount) {
        if (from == null || amount >= 1) {
            return to;
        }
        Map<String, Transform> result = new HashMap<>();
        for (String bone : from.keySet()) {
            result.put(bone, from.get(bone).blend(to.getOrDefault(bone, IDENTITY), amount));
        }
        for (String bone : to.keySet()) {
            result.putIfAbsent(bone, IDENTITY.blend(to.get(bone), amount));
        }
        return result;
    }

    private static void apply(Map<String, Transform> result, Clip clip, float time) {
        if (clip == null) {
            return;
        }
        clip.bones.forEach((name, tracks) -> {
            Transform base = result.getOrDefault(name, IDENTITY);
            result.put(name, new Transform(tracks.position.sample(time, base.position),
                    tracks.rotation.sample(time, base.rotation),
                    tracks.scale.sample(time, base.scale)));
        });
    }

    private static Map<String, Clip> clips() {
        if (clips != null) {
            return clips;
        }
        Map<String, Clip> loaded = new HashMap<>();
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(FILE)) {
            JsonObject animations = JsonParser.parseReader(reader).getAsJsonObject()
                    .getAsJsonObject("animations");
            for (var entry : animations.entrySet()) {
                JsonObject json = entry.getValue().getAsJsonObject();
                Map<String, Tracks> bones = new HashMap<>();
                for (var boneEntry : json.getAsJsonObject("bones").entrySet()) {
                    JsonObject bone = boneEntry.getValue().getAsJsonObject();
                    bones.put(boneEntry.getKey(), new Tracks(Track.parse(bone.get("position")),
                            Track.parse(bone.get("rotation")), Track.parse(bone.get("scale"))));
                }
                float duration = json.has("animation_length")
                        ? json.get("animation_length").getAsFloat() : 0;
                loaded.put(entry.getKey(), new Clip(duration, bones));
            }
        } catch (IOException | RuntimeException exception) {
            LogUtils.getLogger().error("Failed to load M67 animation {}", FILE, exception);
        }
        clips = loaded;
        return clips;
    }

    public record Vector(float x, float y, float z) {
        Vector blend(Vector other, float amount) {
            return new Vector(x + (other.x - x) * amount, y + (other.y - y) * amount,
                    z + (other.z - z) * amount);
        }

        public boolean hidden() {
            return Math.abs(x) < 0.00001F || Math.abs(y) < 0.00001F || Math.abs(z) < 0.00001F;
        }

        static Vector parse(JsonElement value) {
            if (value.isJsonPrimitive()) {
                float scalar = value.getAsFloat();
                return new Vector(scalar, scalar, scalar);
            }
            var array = value.getAsJsonArray();
            return new Vector(array.get(0).getAsFloat(), array.get(1).getAsFloat(),
                    array.get(2).getAsFloat());
        }
    }

    public record Transform(Vector position, Vector rotation, Vector scale) {
        Transform blend(Transform other, float amount) {
            return new Transform(position.blend(other.position, amount),
                    rotation.blend(other.rotation, amount), scale.blend(other.scale, amount));
        }
    }

    private record Clip(float duration, Map<String, Tracks> bones) {
    }

    private record Tracks(Track position, Track rotation, Track scale) {
    }

    private record Key(float time, Vector pre, Vector post, boolean smooth) {
    }

    private record Track(List<Key> keys) {
        static Track parse(JsonElement value) {
            if (value == null) {
                return new Track(List.of());
            }
            if (!value.isJsonObject()) {
                Vector vector = Vector.parse(value);
                return new Track(List.of(new Key(0, vector, vector, false)));
            }
            List<Key> keys = new ArrayList<>();
            for (var entry : value.getAsJsonObject().entrySet()) {
                float time = Float.parseFloat(entry.getKey());
                JsonElement frame = entry.getValue();
                if (frame.isJsonObject()) {
                    JsonObject object = frame.getAsJsonObject();
                    Vector post = Vector.parse(object.has("post") ? object.get("post") : object.get("pre"));
                    Vector pre = object.has("pre") ? Vector.parse(object.get("pre")) : post;
                    boolean smooth = object.has("lerp_mode")
                            && "catmullrom".equals(object.get("lerp_mode").getAsString());
                    keys.add(new Key(time, pre, post, smooth));
                } else {
                    Vector vector = Vector.parse(frame);
                    keys.add(new Key(time, vector, vector, false));
                }
            }
            keys.sort(Comparator.comparingDouble(Key::time));
            return new Track(List.copyOf(keys));
        }

        Vector sample(float time, Vector fallback) {
            if (keys.isEmpty()) {
                return fallback;
            }

            if (time < keys.get(0).time) {
                return keys.get(0).pre;
            }
            for (int i = 1; i < keys.size(); i++) {
                Key right = keys.get(i);
                if (time < right.time) {
                    Key left = keys.get(i - 1);
                    float progress = (time - left.time) / (right.time - left.time);
                    if (!left.smooth && !right.smooth) {
                        return left.post.blend(right.pre, progress);
                    }
                    Vector before = keys.get(Math.max(0, i - 2)).post;
                    Vector after = keys.get(Math.min(keys.size() - 1, i + 1)).pre;
                    return new Vector(catmull(before.x, left.post.x, right.pre.x, after.x, progress),
                            catmull(before.y, left.post.y, right.pre.y, after.y, progress),
                            catmull(before.z, left.post.z, right.pre.z, after.z, progress));
                }
            }
            return keys.get(keys.size() - 1).post;
        }

        private static float catmull(float a, float b, float c, float d, float t) {
            return 0.5F * ((2 * b) + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t * t
                    + (-a + 3 * b - 3 * c + d) * t * t * t);
        }
    }
}
