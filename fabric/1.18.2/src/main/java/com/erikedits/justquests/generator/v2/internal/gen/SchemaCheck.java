package com.erikedits.justquests.generator.v2.internal.gen;

import com.erikedits.justquests.generator.v2.api.HostCapabilities;
import com.erikedits.justquests.generator.v2.internal.catalog.ObjectiveType;
import com.erikedits.justquests.generator.v2.internal.util.Ids;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strict structural check mirroring §6 for generated quests. The core runs it before the host's
 * validator (a quest failing here is never served); the tests use it on every generated quest.
 */
public final class SchemaCheck {
    public static final int TITLE_MAX = 32;
    public static final int DESCRIPTION_MAX = 140;
    private static final Set<String> QUEST_KEYS = Set.of("title", "description", "category", "mode", "sort",
        "objectives", "rewards");
    private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2,3}_[a-z]{2,4}");
    private static final Set<String> REWARD_TYPES = Set.of("justquests:give_item", "justquests:xp",
        "justquests:effect", "justquests:loot_table", "justquests:message");

    private SchemaCheck() {
    }

    /**
     * @param q    quest JSON
     * @param caps host capabilities (tag support); may be null to forbid all tags
     * @return problems; empty if valid
     */
    public static List<String> check(JsonObject q, HostCapabilities caps) {
        List<String> p = new ArrayList<>();
        for (String k : q.keySet()) {
            if (!QUEST_KEYS.contains(k)) {
                p.add("unknown key " + k);
            }
        }
        Map<String, String> titles = texts(q.get("title"));
        if (titles == null || titles.get("en_us").isBlank()) {
            p.add("missing title");
        } else {
            for (String title : titles.values()) {
                if (title.isBlank()) {
                    p.add("blank title");
                }
                if (title.length() > TITLE_MAX) {
                    p.add("title too long: " + title);
                }
                if (title.contains("{") || title.contains("}")) {
                    p.add("placeholder in title: " + title);
                }
            }
        }
        if (q.has("description")) {
            Map<String, String> descriptions = texts(q.get("description"));
            if (descriptions == null) {
                p.add("description not a string or language map");
            } else {
                for (String d : descriptions.values()) {
                    if (d.length() > DESCRIPTION_MAX) {
                        p.add("description too long (" + d.length() + ")");
                    }
                    if (d.contains("{") || d.contains("}")) {
                        p.add("placeholder in description: " + d);
                    }
                }
            }
        }
        if (!"generated".equals(str(q, "category"))) {
            p.add("category must be generated");
        }
        if (q.has("mode") && !"all".equals(str(q, "mode"))) {
            p.add("mode must be all");
        }
        if (q.has("sort") && !isInt(q.get("sort"))) {
            p.add("sort must be an int");
        }
        if (!q.has("objectives") || !q.get("objectives").isJsonArray()) {
            p.add("objectives missing");
        } else {
            int n = q.getAsJsonArray("objectives").size();
            if (n < 1 || n > 3) {
                p.add("objective count " + n);
            }
            for (JsonElement e : q.getAsJsonArray("objectives")) {
                checkObjective(e, caps, p);
            }
        }
        if (!q.has("rewards") || !q.get("rewards").isJsonArray()) {
            p.add("rewards missing");
        } else {
            int n = q.getAsJsonArray("rewards").size();
            if (n < 1 || n > 3) {
                p.add("reward count " + n);
            }
            for (JsonElement e : q.getAsJsonArray("rewards")) {
                checkReward(e, p);
            }
        }
        return p;
    }

    private static void checkObjective(JsonElement e, HostCapabilities caps, List<String> p) {
        if (!e.isJsonObject()) {
            p.add("objective not an object");
            return;
        }
        JsonObject o = e.getAsJsonObject();
        String typeId = str(o, "type");
        ObjectiveType t = typeId != null && typeId.startsWith("justquests:") ? ObjectiveType.parse(typeId) : null;
        if (t == null) {
            p.add("forbidden or unknown objective type " + typeId);
            return;
        }
        for (String k : o.keySet()) {
            if (!k.equals("type") && !k.equals(t.field()) && !(t.counted() && k.equals("count"))) {
                p.add("unknown objective key " + k);
            }
        }
        String target = str(o, t.field());
        JsonElement filter = o.get(t.field());
        if (target == null && filter != null && filter.isJsonObject() && t.itemBased()) {
            // item filter object: {"id": ..., "potion": ...}
            JsonObject f = filter.getAsJsonObject();
            for (String k : f.keySet()) {
                if (!k.equals("id") && !k.equals("potion")) {
                    p.add("unknown item filter key " + k);
                }
            }
            target = str(f, "id");
            String potion = str(f, "potion");
            if (f.has("potion") && (potion == null || !Ids.isValid(potion))) {
                p.add("bad potion " + f.get("potion"));
            }
        }
        if (target == null) {
            p.add(t.shortName() + " missing " + t.field());
        } else if (target.startsWith("#")) {
            if (!t.taggable() || caps == null || !caps.supportsTag(t.typeId())) {
                p.add("tag not allowed for " + t.shortName());
            } else if (!Ids.isValidTag(target)) {
                p.add("bad tag " + target);
            }
        } else if (!Ids.isValid(target)) {
            p.add("bad id " + target);
        }
        if (t.counted()) {
            if (!o.has("count") || !isInt(o.get("count")) || o.get("count").getAsInt() <= 0) {
                p.add(t.shortName() + " needs count > 0");
            }
        }
    }

    private static void checkReward(JsonElement e, List<String> p) {
        if (!e.isJsonObject()) {
            p.add("reward not an object");
            return;
        }
        JsonObject o = e.getAsJsonObject();
        String type = str(o, "type");
        if (type == null || !REWARD_TYPES.contains(type)) {
            p.add("forbidden or unknown reward type " + type);
            return;
        }
        Map<String, Set<String>> keys = Map.of(
            "justquests:give_item", Set.of("type", "item", "count"),
            "justquests:xp", Set.of("type", "amount"),
            "justquests:effect", Set.of("type", "effect", "seconds", "amplifier"),
            "justquests:loot_table", Set.of("type", "loot_table"),
            "justquests:message", Set.of("type", "message"));
        for (String k : o.keySet()) {
            if (!keys.get(type).contains(k)) {
                p.add("unknown reward key " + k);
            }
        }
        switch (type) {
            case "justquests:give_item":
                if (!Ids.isValid(str(o, "item"))) {
                    p.add("give_item needs a plain item id");
                }
                if (!o.has("count") || !isInt(o.get("count")) || o.get("count").getAsInt() <= 0) {
                    p.add("give_item needs count > 0");
                }
                break;
            case "justquests:xp":
                if (!o.has("amount") || !isInt(o.get("amount")) || o.get("amount").getAsInt() <= 0) {
                    p.add("xp needs amount > 0");
                }
                break;
            case "justquests:effect":
                if (!Ids.isValid(str(o, "effect"))) {
                    p.add("effect needs an id");
                }
                break;
            case "justquests:loot_table":
                if (!Ids.isValid(str(o, "loot_table"))) {
                    p.add("loot_table needs an id");
                }
                break;
            default:
                Map<String, String> m = texts(o.get("message"));
                if (m == null) {
                    p.add("bad message");
                } else {
                    for (String s : m.values()) {
                        if (s.isBlank() || s.contains("{")) {
                            p.add("bad message");
                        }
                    }
                }
                break;
        }
    }

    /**
     * A text field: a string, or a per-language map of strings that has {@code en_us}. Returns
     * language → text ({@code en_us} for a plain string), or null if malformed.
     */
    static Map<String, String> texts(JsonElement e) {
        if (e == null) {
            return null;
        }
        Map<String, String> out = new LinkedHashMap<>();
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            out.put("en_us", e.getAsString());
            return out;
        }
        if (!e.isJsonObject()) {
            return null;
        }
        for (Map.Entry<String, JsonElement> x : e.getAsJsonObject().entrySet()) {
            JsonElement v = x.getValue();
            if (!LANGUAGE.matcher(x.getKey()).matches() || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) {
                return null;
            }
            out.put(x.getKey(), v.getAsString());
        }
        return out.containsKey("en_us") ? out : null;
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    private static boolean isInt(JsonElement e) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
            return false;
        }
        double d = e.getAsDouble();
        return d == Math.rint(d) && Math.abs(d) < Integer.MAX_VALUE;
    }
}
