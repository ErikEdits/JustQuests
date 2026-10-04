package com.erikedits.justquests.generator.v2.internal.state;

import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** A served quest: definition plus generator metadata (never emitted into the quest JSON). */
public final class QuestRecord {
    public String id;
    public long cycleId;
    public int index;
    public JsonObject json;
    public String signature = "";
    public double estMinutes;
    public int tier;
    public List<String> families = new ArrayList<>();
    public List<String> types = new ArrayList<>();
    public List<String> profiles = new ArrayList<>();
    public String difficulty = "NORMAL";
    public long generatedAt;
    public long seed;
    public double budget;
    public double rewardValue;
    public boolean quick;
    public boolean legacy;
    public String theme;
    public List<String> explain = new ArrayList<>();
    public ClaimRecord claim = new ClaimRecord();

    public String title() {
        return json == null ? "" : Json.text(json, "title", "");
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("cycle", cycleId);
        o.addProperty("index", index);
        o.add("quest", json);
        JsonObject m = new JsonObject();
        m.addProperty("signature", signature);
        m.addProperty("estMinutes", Math.round(estMinutes * 100.0) / 100.0);
        m.addProperty("tier", tier);
        m.add("families", Json.toArray(families));
        m.add("types", Json.toArray(types));
        m.add("profiles", Json.toArray(profiles));
        m.addProperty("difficulty", difficulty);
        m.addProperty("generatedAt", generatedAt);
        m.addProperty("seed", seed);
        m.addProperty("budget", Math.round(budget * 100.0) / 100.0);
        m.addProperty("rewardValue", Math.round(rewardValue * 100.0) / 100.0);
        m.addProperty("quick", quick);
        m.addProperty("legacy", legacy);
        if (theme != null) {
            m.addProperty("theme", theme);
        }
        m.add("explain", Json.toArray(explain));
        o.add("meta", m);
        o.add("claim", claim.toJson());
        return o;
    }

    public static QuestRecord fromJson(JsonObject o) {
        QuestRecord r = new QuestRecord();
        r.id = Json.str(o, "id", null);
        r.cycleId = Json.lng(o, "cycle", 0);
        r.index = Json.integer(o, "index", 0);
        r.json = Json.obj(o, "quest");
        JsonObject m = Json.obj(o, "meta");
        r.signature = Json.str(m, "signature", "");
        r.estMinutes = Json.dbl(m, "estMinutes", 0);
        r.tier = Json.integer(m, "tier", 0);
        r.families = Json.strings(m, "families");
        r.types = Json.strings(m, "types");
        r.profiles = Json.strings(m, "profiles");
        r.difficulty = Json.str(m, "difficulty", "NORMAL");
        r.generatedAt = Json.lng(m, "generatedAt", 0);
        r.seed = Json.lng(m, "seed", 0);
        r.budget = Json.dbl(m, "budget", 0);
        r.rewardValue = Json.dbl(m, "rewardValue", 0);
        r.quick = Json.bool(m, "quick", false);
        r.legacy = Json.bool(m, "legacy", false);
        r.theme = Json.str(m, "theme", null);
        r.explain = Json.strings(m, "explain");
        JsonObject c = Json.obj(o, "claim");
        r.claim = c == null ? new ClaimRecord() : ClaimRecord.fromJson(c);
        return r;
    }
}
