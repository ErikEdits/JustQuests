package com.erikedits.justquests.generator.v2.internal.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Emits the quest JSON contract of §6 (and nothing else). */
public final class QuestJson {
    private QuestJson() {
    }

    public static JsonObject build(QuestDraft d) {
        JsonObject q = new JsonObject();
        q.addProperty("title", d.title);
        if (d.description != null && !d.description.isEmpty()) {
            q.addProperty("description", d.description);
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
            j.addProperty(o.c().type().field(), o.c().target());
            if (o.c().type().counted()) {
                j.addProperty("count", o.count());
            }
            objs.add(j);
        }
        q.add("objectives", objs);
        JsonArray rewards = new JsonArray();
        for (QuestDraft.Reward r : d.rewards) {
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
                    j.addProperty("message", r.text());
                    break;
                default:
                    throw new IllegalStateException("unknown reward type " + r.type());
            }
            rewards.add(j);
        }
        q.add("rewards", rewards);
        return q;
    }
}
