package lol.silencecute.client.lang;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lol.silencecute.Client;
import lol.silencecute.api.Operator;
import lol.silencecute.config.ModConfigData;

public final class Lang {

    private static final String ZH_PATH = "/assets/" + Client.MOD_ID + "/lang/ui_zh_cn.json";
    private static final String EN_PATH = "/assets/" + Client.MOD_ID + "/lang/ui_en_us.json";

    private static final Map<String, String> ZH = new HashMap<>();

    private static final Map<String, String> EN = new HashMap<>();

    private static final Map<String, String> ZH_TO_KEY = new HashMap<>();

    private static boolean loaded;

    private Lang() {
    }

    public static boolean chinese() {
        try {
            return ModConfigData.guiChinese();
        } catch (Exception e) {
            return true;
        }
    }

    public static String tr(String key) {
        if (key == null || key.isEmpty()) return "";
        ensureLoaded();
        boolean zh = chinese();
        String text = (zh ? ZH : EN).get(key);
        if (text != null) return text;
        text = (zh ? EN : ZH).get(key);
        return text != null ? text : key;
    }

    public static String byZh(String zh) {
        if (zh == null || zh.isEmpty()) return "";
        ensureLoaded();
        if (chinese()) return zh;
        String key = ZH_TO_KEY.get(zh);
        if (key == null) return zh;
        String text = EN.get(key);
        return text != null ? text : zh;
    }

    public static String operator(Operator operator) {
        if (operator == null) return "";
        return chinese() ? operator.zhName() : operator.displayName();
    }

    public static String option(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        Operator operator = Operator.byName(raw);
        return operator != null ? operator(operator) : byZh(raw);
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        load(ZH_PATH, ZH);
        load(EN_PATH, EN);
        for (Map.Entry<String, String> entry : ZH.entrySet()) {

            ZH_TO_KEY.putIfAbsent(entry.getValue(), entry.getKey());
        }
    }

    private static void load(String path, Map<String, String> into) {
        try (InputStream in = Lang.class.getResourceAsStream(path)) {
            if (in == null) return;
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    try {
                        into.put(entry.getKey(), entry.getValue().getAsString());
                    } catch (Exception ignored) {

                    }
                }
            }
        } catch (Exception ignored) {

        }
    }
}
