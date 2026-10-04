package com.erikedits.justquests.generator.v2.internal.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

/** Emits the quest JSON contract of §6 (and nothing else). */
public final class QuestJson {
    private QuestJson() {
    }

    public static JsonObject build(QuestDraft d) {
        JsonObject q = new JsonObject();
        q.add("title", text(d.title, d.titles));
        if (d.description != null && !d.description.isEmpty()) {
            q.add("description", text(d.description, d.descriptions));
        }
        q.addProperty("category", "generated");
        q.addProperty("sort", Math.max(0, (int) Math.round(d.estMinutes)));
        if (d.objectives.size() > 1) {
            q.addProperty("mode", "all");
        }
        JsonArray objs = new JsonArray();
        for (QuestDraft.Objective o : d.objectives) {
            JsonObject j = new JsonObject();
            j.addProperty("type", o.c().type().typeId());
            String potion = o.c().def().potion();
            if (potion != null) {
                // item filter object (mod 0.3.2+): {"id": "minecraft:potion", "potion": "minecraft:swiftness"}
                JsonObject item = new JsonObject();
                item.addProperty("id", o.c().target());
                item.addProperty("potion", potion);
                j.add(o.c().type().field(), item);
            } else {
                j.addProperty(o.c().type().field(), o.c().target());
            }
            if (o.c().type().counted()) {
                j.addProperty("count", o.count());
            }
            objs.add(j);
        }
        q.add("objectives", objs);
        JsonArray rewards = new JsonArray();
        for (QuestDraft.Reward r : d.rewards) {
            rewards.add(reward(d, r));
        }
        q.add("rewards", rewards);
        return q;
    }

    private static JsonObject reward(QuestDraft d, QuestDraft.Reward r) {
        JsonObject j = new JsonObject();
        j.addProperty("type", "justquests:" + r.type());
        switch (r.type()) {
            case "give_item":
                j.addProperty("item", r.id());
                j.addProperty("count", r.count());
                break;
            case "xp":
                j.addProperty("amount", r.count());
                break;
            case "effect":
                j.addProperty("effect", r.id());
                j.addProperty("seconds", r.count());
                j.addProperty("amplifier", r.amplifier());
                break;
            case "loot_table":
                j.addProperty("loot_table", r.id());
                break;
            case "message":
                j.add("message", text(r.text(), d.messageTexts));
                break;
            case "choice":
                JsonArray options = new JsonArray();
                for (QuestDraft.Reward o : d.choice) {
                    options.add(reward(d, o));
                }
                j.add("options", options);
                break;
            default:
                throw new IllegalStateException("unknown reward type " + r.type());
        }
        return j;
    }

    /** A plain string, or a per-language map ({@code en_us} first) when other languages are present. */
    private static JsonElement text(String english, Map<String, String> all) {
        if (all == null || all.size() <= 1) {
            return new JsonPrimitive(english);
        }
        JsonObject o = new JsonObject();
        o.addProperty("en_us", english);
        for (Map.Entry<String, String> e : all.entrySet()) {
            if (!e.getKey().equals("en_us") && e.getValue() != null && !e.getValue().isEmpty()) {
                o.addProperty(e.getKey(), e.getValue());
            }
        }
        return o;
    }
}
