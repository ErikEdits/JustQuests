package com.erikedits.justquests.plugin.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The player-facing texts. Vanilla clients have no mod language files, so the plugin translates on
 * the server: the shared lang/*.json of the mod plus the plugin's own lang-plugin/*.json, picked by
 * the player's client language with English as the fallback.
 */
public final class Lang {
    public static final String DEFAULT = "en_us";
    public static final List<String> CODES = List.of("en_us", "de_de", "fr_fr", "es_es", "ja_jp");

    private static final Map<String, Map<String, String>> TEXTS = new HashMap<>();

    private Lang() {}

    public static void load() {
        TEXTS.clear();
        for (String code : CODES) {
            Map<String, String> map = new HashMap<>();
            read("/lang/" + code + ".json", map);
            read("/lang-plugin/" + code + ".json", map);
            TEXTS.put(code, map);
        }
    }

    private static void read(String path, Map<String, String> into) {
        try (InputStream in = Lang.class.getResourceAsStream(path)) {
            if (in == null) return;
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) into.put(e.getKey(), e.getValue().getAsString());
        } catch (Exception ignored) {
            // a missing language falls back to English
        }
    }

    /** The client language of a player ("de_de"), English for the console. */
    public static String of(CommandSender sender) {
        return sender instanceof Player p ? code(p.getLocale()) : DEFAULT;
    }

    /** A client locale reduced to a language the plugin has, keeping the code for quest texts. */
    public static String code(String locale) {
        return locale == null || locale.isEmpty() ? DEFAULT : locale.toLowerCase(Locale.ROOT);
    }

    /** The text for the key in the language (or its base language), else English, else the key. */
    public static String get(String lang, String key) {
        Map<String, String> map = TEXTS.get(lang);
        if (map == null) map = TEXTS.get(base(lang));
        String s = map == null ? null : map.get(key);
        if (s == null) s = TEXTS.getOrDefault(DEFAULT, Map.of()).get(key);
        return s == null ? key : s;
    }

    /** How many English keys the language lacks. */
    public static int missing(String lang) {
        Map<String, String> map = TEXTS.getOrDefault(lang, Map.of());
        int n = 0;
        for (String key : TEXTS.getOrDefault(DEFAULT, Map.of()).keySet()) {
            if (!map.containsKey(key)) n++;
        }
        return n;
    }

    public static boolean has(String key) {
        return TEXTS.getOrDefault(DEFAULT, Map.of()).containsKey(key);
    }

    /** de_at -> de_de, es_mx -> es_es: a regional variant uses the main language of the plugin. */
    private static String base(String lang) {
        int u = lang.indexOf('_');
        String prefix = (u < 0 ? lang : lang.substring(0, u)) + "_";
        for (String code : CODES) {
            if (code.startsWith(prefix)) return code;
        }
        return DEFAULT;
    }
}
