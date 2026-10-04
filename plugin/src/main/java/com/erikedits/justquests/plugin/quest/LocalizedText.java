package com.erikedits.justquests.plugin.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quest text that is either a plain string or a per-language map ({"en_us": ..., "de_de": ...}),
 * resolved per player with English as the fallback - the same format the mod reads.
 */
public final class LocalizedText {
    public static final String DEFAULT_LANG = "en_us";
    public static final LocalizedText EMPTY = new LocalizedText(Map.of("", ""));

    private final Map<String, String> byLang;

    private LocalizedText(Map<String, String> byLang) {
        this.byLang = byLang;
    }

    public static LocalizedText of(String text) {
        return new LocalizedText(Map.of("", text));
    }

    public static LocalizedText parse(JsonElement e) {
        if (e == null || e.isJsonNull()) return EMPTY;
        if (e.isJsonPrimitive()) return of(e.getAsString());
        if (!e.isJsonObject()) throw new IllegalArgumentException("text must be a string or a language map");
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : ((JsonObject) e).entrySet()) {
            map.put(entry.getKey().toLowerCase(java.util.Locale.ROOT), entry.getValue().getAsString());
        }
        return map.isEmpty() ? EMPTY : new LocalizedText(map);
    }

    /** Text for the client language, falling back to its base language, English, then any entry. */
    public String get(String lang) {
        if (byLang.containsKey("")) return byLang.get("");
        String exact = byLang.get(lang);
        if (exact != null) return exact;
        int u = lang.indexOf('_');
        if (u > 0) {
            String prefix = lang.substring(0, u + 1);
            for (Map.Entry<String, String> e : byLang.entrySet()) {
                if (e.getKey().startsWith(prefix)) return e.getValue();
            }
        }
        String en = byLang.get(DEFAULT_LANG);
        return en != null ? en : byLang.values().iterator().next();
    }

    public String getDefault() {
        return get(DEFAULT_LANG);
    }

    public boolean isBlank() {
        return getDefault().isBlank();
    }
}
