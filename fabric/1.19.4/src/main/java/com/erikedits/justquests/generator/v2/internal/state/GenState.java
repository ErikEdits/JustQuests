package com.erikedits.justquests.generator.v2.internal.state;

import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Everything persisted in {@code generator_v2.json} (format 1). */
public final class GenState {
    public static final int FORMAT = 1;
    public static final String FILE = "generator_v2.json";

    /** Current cycle id (boundary, epoch seconds); 0 if never generated. */
    public long cycleId;
    public long cycleStartedAt;
    public int rerollCounter;
    public int nextIndex;
    public long lastSeenMillis;
    public String difficulty = "NORMAL";
    /** Quests of the current cycle, in generation order. */
    public final LinkedHashMap<String, QuestRecord> current = new LinkedHashMap<>();
    /** Claimed, unfinished quests from older cycles. */
    public final LinkedHashMap<String, QuestRecord> retained = new LinkedHashMap<>();
    /** Signature → last used epoch ms (6-day window). */
    public final TreeMap<String, Long> history = new TreeMap<>();
    public Progression progression = new Progression();
    /** family|type → effort multiplier (self-calibration). */
    public final TreeMap<String, Double> calibration = new TreeMap<>();
    public long calibratedCycle;
    public boolean migratedFromV1;

    public QuestRecord find(String id) {
        QuestRecord r = current.get(id);
        return r != null ? r : retained.get(id);
    }

    public boolean isRetained(String id) {
        return retained.containsKey(id);
    }

    public List<QuestRecord> served() {
        List<QuestRecord> out = new ArrayList<>(current.values());
        out.addAll(retained.values());
        return out;
    }

    /** Drops history entries older than the window. */
    public void pruneHistory(long now, long windowMs) {
        Iterator<Map.Entry<String, Long>> it = history.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() >= windowMs) {
                it.remove();
            }
        }
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("format", FORMAT);
        o.addProperty("cycleId", cycleId);
        o.addProperty("cycleStartedAt", cycleStartedAt);
        o.addProperty("rerollCounter", rerollCounter);
        o.addProperty("nextIndex", nextIndex);
        o.addProperty("lastSeenMillis", lastSeenMillis);
        o.addProperty("difficulty", difficulty);
        o.addProperty("migratedFromV1", migratedFromV1);
        JsonArray cur = new JsonArray();
        current.values().forEach(r -> cur.add(r.toJson()));
        o.add("quests", cur);
        JsonArray ret = new JsonArray();
        retained.values().forEach(r -> ret.add(r.toJson()));
        o.add("retained", ret);
        o.add("history", Json.toObject(history));
        o.add("progression", progression.toJson());
        JsonObject cal = new JsonObject();
        calibration.forEach((k, v) -> cal.addProperty(k, Math.round(v * 1000.0) / 1000.0));
        o.add("calibration", cal);
        o.addProperty("calibratedCycle", calibratedCycle);
        return o;
    }

    /** Parses a state file; throws on structurally invalid content (caller backs up and starts fresh). */
    public static GenState fromJson(JsonObject o) {
        int format = Json.integer(o, "format", -1);
        if (format != FORMAT) {
            throw new IllegalStateException("unsupported state format " + format);
        }
        GenState s = new GenState();
        s.cycleId = Json.lng(o, "cycleId", 0);
        s.cycleStartedAt = Json.lng(o, "cycleStartedAt", 0);
        s.rerollCounter = Json.integer(o, "rerollCounter", 0);
        s.nextIndex = Json.integer(o, "nextIndex", 0);
        s.lastSeenMillis = Json.lng(o, "lastSeenMillis", 0);
        s.difficulty = Json.str(o, "difficulty", "NORMAL");
        s.migratedFromV1 = Json.bool(o, "migratedFromV1", false);
        for (JsonObject q : Json.objects(o, "quests")) {
            QuestRecord r = QuestRecord.fromJson(q);
            if (r.id != null && r.json != null) {
                s.current.put(r.id, r);
            }
        }
        for (JsonObject q : Json.objects(o, "retained")) {
            QuestRecord r = QuestRecord.fromJson(q);
            if (r.id != null && r.json != null) {
                s.retained.put(r.id, r);
            }
        }
        JsonObject h = Json.obj(o, "history");
        if (h != null) {
            for (Map.Entry<String, JsonElement> e : h.entrySet()) {
                if (e.getValue().isJsonPrimitive()) {
                    s.history.put(e.getKey(), e.getValue().getAsLong());
                }
            }
        }
        s.progression = Progression.fromJson(Json.obj(o, "progression"));
        JsonObject cal = Json.obj(o, "calibration");
        if (cal != null) {
            for (Map.Entry<String, JsonElement> e : cal.entrySet()) {
                if (e.getValue().isJsonPrimitive()) {
                    s.calibration.put(e.getKey(), e.getValue().getAsDouble());
                }
            }
        }
        s.calibratedCycle = Json.lng(o, "calibratedCycle", 0);
        return s;
    }
}
