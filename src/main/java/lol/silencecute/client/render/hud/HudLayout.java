package lol.silencecute.client.render.hud;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

public final class HudLayout {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "operator_skills-hud.json";

    public static final int AUTOSAVE_TICKS = 1200;

    public record Rect(float x, float y, float w, float h) {}

    private static final Map<String, Rect> STORE = new LinkedHashMap<>();

    private static boolean loaded;
    private static boolean dirty;
    private static int saveCounter;

    private HudLayout() {}

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path path = file();
        if (!Files.isRegularFile(path)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            for (String id : root.keySet()) {
                if (!root.get(id).isJsonObject()) continue;
                JsonObject entry = root.getAsJsonObject(id);
                if (!entry.has("x") || !entry.has("y")) continue;
                float x = entry.get("x").getAsFloat();
                float y = entry.get("y").getAsFloat();
                float w = entry.has("w") ? entry.get("w").getAsFloat() : 0.0F;
                float h = entry.has("h") ? entry.get("h").getAsFloat() : 0.0F;
                STORE.put(id, new Rect(x, y, w, h));
            }
        } catch (Exception e) {
            LOGGER.warn("[silencecute] 读取 HUD 布局失败，使用默认值: {}", e.toString());
        }
    }

    static Rect find(String id) {
        ensureLoaded();
        return STORE.get(id);
    }

    static void put(String id, Rect rect) {
        ensureLoaded();
        STORE.put(id, rect);
    }

    static void markDirty() {
        dirty = true;
    }

    static boolean isDirty() {
        return dirty;
    }

    static void tickAutosave() {
        if (!dirty) return;
        if (++saveCounter >= AUTOSAVE_TICKS) save();
    }

    static void saveIfDirty() {
        if (dirty) save();
    }

    static synchronized void save() {
        ensureLoaded();
        try {
            JsonObject root = new JsonObject();
            STORE.forEach((id, rect) -> {
                JsonObject entry = new JsonObject();
                entry.addProperty("x", rect.x());
                entry.addProperty("y", rect.y());
                entry.addProperty("w", rect.w());
                entry.addProperty("h", rect.h());
                root.add(id, entry);
            });

            Path path = file();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
            dirty = false;
            saveCounter = 0;
        } catch (Exception e) {
            LOGGER.warn("[silencecute] 保存 HUD 布局失败: {}", e.toString());
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }
}
