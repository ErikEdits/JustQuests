package com.erikedits.justquests.generator.v2.internal.state;

import com.erikedits.justquests.generator.v2.internal.util.Ids;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads v1's {@code generated.json} ({@code lastRefresh}, {@code history}, {@code quests}) and converts
 * signatures to the v2 format ({@code justquests:collect_item|minecraft:oak_log} →
 * {@code collect_item:minecraft:oak_log}). The v1 file is never modified or deleted.
 */
public final class V1Migration {
    public static final String FILE = "generated.json";

    /** Parsed v1 content. */
    public static final class V1Data {
        public long lastRefresh;
        public final Map<String, Long> history = new LinkedHashMap<>();
        /** Full quest id → quest JSON. */
        public final Map<String, JsonObject> quests = new LinkedHashMap<>();
    }

    private V1Migration() {
    }

    public static V1Data parse(String text) {
        JsonObject o = Json.parseObject(text);
        V1Data d = new V1Data();
        d.lastRefresh = Json.lng(o, "lastRefresh", 0);
        JsonObject h = Json.obj(o, "history");
        if (h != null) {
            for (Map.Entry<String, JsonElement> e : h.entrySet()) {
                if (e.getValue().isJsonPrimitive()) {
                    d.history.put(convertSignature(e.getKey()), e.getValue().getAsLong());
                }
            }
        }
        JsonObject q = Json.obj(o, "quests");
        if (q != null) {
            for (Map.Entry<String, JsonElement> e : q.entrySet()) {
                if (e.getValue().isJsonObject()) {
                    d.quests.put(Ids.GEN_PREFIX + e.getKey(), e.getValue().getAsJsonObject());
                }
            }
        }
        return d;
    }

    /** v1 {@code justquests:<type>|<target>} → v2 {@code <type>:<target>}; unknown formats pass through. */
    public static String convertSignature(String v1) {
        int bar = v1.indexOf('|');
        if (bar < 0) {
            return v1;
        }
        String type = v1.substring(0, bar);
        String target = v1.substring(bar + 1);
        if (type.startsWith("justquests:")) {
            type = type.substring("justquests:".length());
        }
        return type + ":" + target;
    }

    /** Signature of an arbitrary quest JSON (used for imported v1 definitions). */
    public static String signatureOf(JsonObject quest) {
        java.util.TreeSet<String> parts = new java.util.TreeSet<>();
        for (JsonObject obj : Json.objects(quest, "objectives")) {
            String type = Json.str(obj, "type", "?").replace("justquests:", "");
            String target = null;
            for (String f : new String[]{"item", "block", "entity", "dimension", "advancement"}) {
                if (obj.has(f)) {
                    target = Json.str(obj, f, null);
                    break;
                }
            }
            parts.add(type + ":" + target);
        }
        return String.join("|", parts);
    }
}
