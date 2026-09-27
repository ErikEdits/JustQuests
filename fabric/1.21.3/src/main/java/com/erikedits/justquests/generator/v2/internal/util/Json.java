package com.erikedits.justquests.generator.v2.internal.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Lenient JSON helpers on top of the Gson 2.8.8 API. Getters never throw on wrong types. */
public final class Json {
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    private Json() {
    }

    /**
     * Parses text into an object.
     *
     * @param text JSON text
     * @return the object
     * @throws RuntimeException (Gson's JsonParseException or IllegalStateException) on bad input
     */
    public static JsonObject parseObject(String text) {
        JsonElement e = JsonParser.parseString(text);
        if (!e.isJsonObject()) {
            throw new IllegalStateException("expected a JSON object");
        }
        return e.getAsJsonObject();
    }

    public static String pretty(JsonElement e) {
        return PRETTY.toJson(e);
    }

    public static String compact(JsonElement e) {
        return COMPACT.toJson(e);
    }

    public static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    public static JsonArray arr(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    public static String str(JsonObject o, String key, String def) {
        JsonElement e = o == null ? null : o.get(key);
        if (e != null && e.isJsonPrimitive()) {
            return e.getAsString();
        }
        return def;
    }

    public static double dbl(JsonObject o, String key, double def) {
        JsonElement e = o == null ? null : o.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            return e.getAsDouble();
        }
        return def;
    }

    public static int integer(JsonObject o, String key, int def) {
        JsonElement e = o == null ? null : o.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            return (int) Math.round(e.getAsDouble());
        }
        return def;
    }

    public static long lng(JsonObject o, String key, long def) {
        JsonElement e = o == null ? null : o.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            return e.getAsLong();
        }
        return def;
    }

    public static boolean bool(JsonObject o, String key, boolean def) {
        JsonElement e = o == null ? null : o.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) {
            return e.getAsBoolean();
        }
        return def;
    }

    /** String list from an array of strings or a single string; empty if missing. */
    public static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        JsonElement e = o == null ? null : o.get(key);
        if (e == null) {
            return out;
        }
        if (e.isJsonPrimitive()) {
            out.add(e.getAsString());
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) {
                if (x.isJsonPrimitive()) {
                    out.add(x.getAsString());
                }
            }
        }
        return out;
    }

    public static List<JsonObject> objects(JsonObject o, String key) {
        List<JsonObject> out = new ArrayList<>();
        JsonArray a = arr(o, key);
        if (a != null) {
            for (JsonElement x : a) {
                if (x.isJsonObject()) {
                    out.add(x.getAsJsonObject());
                }
            }
        }
        return out;
    }

    public static JsonArray toArray(Collection<String> values) {
        JsonArray a = new JsonArray();
        for (String v : values) {
            a.add(new JsonPrimitive(v));
        }
        return a;
    }

    public static JsonObject toObject(Map<String, Long> values) {
        JsonObject o = new JsonObject();
        values.forEach(o::addProperty);
        return o;
    }

    /**
     * Deep-merges {@code override} into a copy of {@code base}: objects merge recursively, every
     * other value (including arrays) replaces the base value.
     */
    public static JsonObject deepMerge(JsonObject base, JsonObject override) {
        JsonObject out = base.deepCopy();
        for (Map.Entry<String, JsonElement> e : override.entrySet()) {
            JsonElement cur = out.get(e.getKey());
            if (cur != null && cur.isJsonObject() && e.getValue().isJsonObject()) {
                out.add(e.getKey(), deepMerge(cur.getAsJsonObject(), e.getValue().getAsJsonObject()));
            } else {
                out.add(e.getKey(), e.getValue().deepCopy());
            }
        }
        return out;
    }
}
