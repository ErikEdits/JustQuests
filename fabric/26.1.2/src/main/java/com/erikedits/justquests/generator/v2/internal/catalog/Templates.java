package com.erikedits.justquests.generator.v2.internal.catalog;

import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** English text templates (templates.json). */
public final class Templates {
    /** Title templates per objective type ({Name}, {Names}). */
    public final Map<ObjectiveType, List<String>> titles = new EnumMap<>(ObjectiveType.class);
    /** Objective phrase per type, e.g. "Collect {count} {name}" (no final period). */
    public final Map<ObjectiveType, List<String>> phrases = new EnumMap<>(ObjectiveType.class);
    /**
     * Wording variants selected by a target hint (e.g. {@code cook} for smelted food, {@code drink}
     * for drinks): variant → {@code phrases}/{@code titles} lists replacing the type defaults.
     */
    public final Map<String, Map<String, List<String>>> variants = new LinkedHashMap<>();
    /** Generic fallback titles ({Name}). */
    public final List<String> fallbackTitles = new ArrayList<>();
    /** Hint sentences keyed by hint word ({@code desert}, {@code fortress} …). */
    public final Map<String, String> hints = new LinkedHashMap<>();
    /** Tool phrases keyed by tool ({@code stone} → "a stone pickaxe or better"). */
    public final Map<String, String> tools = new LinkedHashMap<>();
    /** Other fixed sentences keyed by purpose ({@code tag}, {@code smelt}, {@code dimension} …). */
    public final Map<String, String> sentences = new LinkedHashMap<>();
    /** Dimension display names. */
    public final Map<String, String> dimensionNames = new LinkedHashMap<>();
    /** Multi-objective fallback titles when no theme applies ({Family}, {Name}). */
    public final List<String> comboTitles = new ArrayList<>();
    /** Display nouns for families, used by combo titles. */
    public final Map<String, String> familyNames = new LinkedHashMap<>();

    public static Templates parse(JsonObject root) {
        Templates t = new Templates();
        if (root == null) {
            return t;
        }
        readTypeLists(Json.obj(root, "titles"), t.titles);
        readTypeLists(Json.obj(root, "phrases"), t.phrases);
        t.fallbackTitles.addAll(Json.strings(root, "fallbackTitles"));
        JsonObject vars = Json.obj(root, "variants");
        if (vars != null) {
            for (Map.Entry<String, JsonElement> e : vars.entrySet()) {
                if (e.getValue().isJsonObject()) {
                    Map<String, List<String>> v = new LinkedHashMap<>();
                    for (String k : new String[]{"phrases", "titles"}) {
                        List<String> list = Json.strings(e.getValue().getAsJsonObject(), k);
                        if (!list.isEmpty()) {
                            v.put(k, list);
                        }
                    }
                    t.variants.put(e.getKey(), v);
                }
            }
        }
        t.comboTitles.addAll(Json.strings(root, "comboTitles"));
        readStrings(Json.obj(root, "hints"), t.hints);
        readStrings(Json.obj(root, "tools"), t.tools);
        readStrings(Json.obj(root, "sentences"), t.sentences);
        readStrings(Json.obj(root, "dimensionNames"), t.dimensionNames);
        readStrings(Json.obj(root, "familyNames"), t.familyNames);
        return t;
    }

    private static void readTypeLists(JsonObject o, Map<ObjectiveType, List<String>> into) {
        if (o == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            ObjectiveType type = ObjectiveType.parse(e.getKey());
            if (type != null && e.getValue().isJsonArray()) {
                List<String> list = new ArrayList<>();
                for (JsonElement x : e.getValue().getAsJsonArray()) {
                    if (x.isJsonPrimitive()) {
                        list.add(x.getAsString());
                    }
                }
                into.put(type, list);
            }
        }
    }

    private static void readStrings(JsonObject o, Map<String, String> into) {
        if (o == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
                into.put(e.getKey(), e.getValue().getAsString());
            }
        }
    }

    /** Variant list (phrases/titles) for the first hint that has one, or null. */
    public List<String> variant(List<String> hints, String kind) {
        for (String h : hints) {
            Map<String, List<String>> v = variants.get(h);
            if (v != null && v.containsKey(kind)) {
                return v.get(kind);
            }
        }
        return null;
    }

    public String sentence(String key, String def) {
        return sentences.getOrDefault(key, def);
    }
}
