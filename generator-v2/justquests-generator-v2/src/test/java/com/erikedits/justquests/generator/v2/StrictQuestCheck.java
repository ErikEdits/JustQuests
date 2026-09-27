package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.HostCapabilities;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Test-side strict validator mirroring §6, written independently of the core's own SchemaCheck.
 */
final class StrictQuestCheck {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Map<String, String> OBJECTIVE_FIELD = Map.of(
        "justquests:collect_item", "item", "justquests:mine_block", "block", "justquests:craft_item", "item",
        "justquests:smelt_item", "item", "justquests:kill_mob", "entity", "justquests:breed_animal", "entity",
        "justquests:tame_animal", "entity", "justquests:consume_item", "item", "justquests:place_block", "block",
        "justquests:visit_dimension", "dimension");
    private static final Set<String> QUEST_KEYS = Set.of("title", "description", "category", "mode", "sort",
        "objectives", "rewards");
    private static final Map<String, Set<String>> REWARD_KEYS = Map.of(
        "justquests:give_item", Set.of("type", "item", "count"),
        "justquests:xp", Set.of("type", "amount"),
        "justquests:effect", Set.of("type", "effect", "seconds", "amplifier"),
        "justquests:loot_table", Set.of("type", "loot_table"),
        "justquests:message", Set.of("type", "message"));

    private StrictQuestCheck() {
    }

    static List<String> check(JsonObject q, HostCapabilities caps) {
        List<String> p = new ArrayList<>();
        for (String k : q.keySet()) {
            if (!QUEST_KEYS.contains(k)) {
                p.add("unknown key " + k);
            }
        }
        if (q.has("requires") || q.has("repeatable") || q.has("cooldown_hours")) {
            p.add("forbidden quest field");
        }
        String title = s(q, "title");
        if (title == null || title.isBlank() || title.length() > 32 || title.contains("{")) {
            p.add("bad title " + title);
        }
        String desc = s(q, "description");
        if (q.has("description") && (desc == null || desc.length() > 140 || desc.contains("{") || desc.contains("}"))) {
            p.add("bad description " + desc);
        }
        if (!"generated".equals(s(q, "category"))) {
            p.add("category");
        }
        int nObj = q.has("objectives") && q.get("objectives").isJsonArray() ? q.getAsJsonArray("objectives").size() : 0;
        if (nObj < 1 || nObj > 3) {
            p.add("objectives " + nObj);
        }
        if (q.has("mode") && (!"all".equals(s(q, "mode")) || nObj < 2)) {
            p.add("mode");
        }
        if (q.has("sort") && !q.get("sort").getAsJsonPrimitive().isNumber()) {
            p.add("sort");
        }
        if (nObj > 0) {
            for (JsonElement e : q.getAsJsonArray("objectives")) {
                JsonObject o = e.getAsJsonObject();
                String type = s(o, "type");
                String field = OBJECTIVE_FIELD.get(type);
                if (field == null) {
                    p.add("objective type " + type);
                    continue;
                }
                for (String k : o.keySet()) {
                    if (!k.equals("type") && !k.equals(field) && !k.equals("count")) {
                        p.add("objective key " + k);
                    }
                }
                String target = s(o, field);
                if (target == null) {
                    p.add("missing " + field);
                } else if (target.startsWith("#")) {
                    if (!caps.supportsTag(type) || !ID.matcher(target.substring(1)).matches()) {
                        p.add("tag not allowed " + type + " " + target);
                    }
                } else if (!ID.matcher(target).matches()) {
                    p.add("bad id " + target);
                }
                boolean counted = !type.equals("justquests:visit_dimension");
                if (counted && (!o.has("count") || o.get("count").getAsInt() <= 0
                    || o.get("count").getAsDouble() != Math.rint(o.get("count").getAsDouble()))) {
                    p.add("count " + o.get("count"));
                }
                if (!counted && o.has("count")) {
                    p.add("visit_dimension has count");
                }
            }
        }
        int nRew = q.has("rewards") && q.get("rewards").isJsonArray() ? q.getAsJsonArray("rewards").size() : 0;
        if (nRew < 1 || nRew > 3) {
            p.add("rewards " + nRew);
        }
        if (nRew > 0) {
            for (JsonElement e : q.getAsJsonArray("rewards")) {
                JsonObject r = e.getAsJsonObject();
                String type = s(r, "type");
                Set<String> keys = REWARD_KEYS.get(type);
                if (keys == null) {
                    p.add("reward type " + type);
                    continue;
                }
                for (String k : r.keySet()) {
                    if (!keys.contains(k)) {
                        p.add("reward key " + k);
                    }
                }
                if (type.equals("justquests:give_item")) {
                    String item = s(r, "item");
                    if (item == null || item.startsWith("#") || !ID.matcher(item).matches()) {
                        p.add("give_item item " + item);
                    }
                    if (r.get("count").getAsInt() <= 0) {
                        p.add("give_item count");
                    }
                }
                if (type.equals("justquests:xp") && r.get("amount").getAsInt() <= 0) {
                    p.add("xp amount");
                }
            }
        }
        return p;
    }

    private static String s(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }
}
