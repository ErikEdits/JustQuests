package com.erikedits.justquests.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** The mod's own en_us.json, read from the jar: the fallback text and the list of known keys. */
final class English {
    private static Map<String, String> texts;

    private English() {}

    static String get(String key) {
        return load().getOrDefault(key, key);
    }

    static boolean has(String key) {
        return load().containsKey(key);
    }

    private static synchronized Map<String, String> load() {
        if (texts != null) return texts;
        Map<String, String> map = new HashMap<>();
        try (InputStream in = English.class.getResourceAsStream("/assets/justquests/lang/en_us.json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : o.entrySet()) map.put(e.getKey(), e.getValue().getAsString());
            }
        } catch (Exception ignored) {
            // no fallback texts; the keys still work for clients with the mod
        }
        texts = map;
        return texts;
    }
}
