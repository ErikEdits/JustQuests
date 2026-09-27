package com.erikedits.justquests.generator.v2.internal.stats;

import com.erikedits.justquests.generator.v2.api.StatsSummary;
import com.erikedits.justquests.generator.v2.internal.state.QuestRecord;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.erikedits.justquests.generator.v2.internal.util.Medians;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Anonymous test-phase statistics (§15), persisted in {@code generator_v2_stats.json}. Holds a
 * bounded list of per-quest records; older records are folded into aggregate counters and a bounded
 * sample list, so the file stays small for months of play. No player UUIDs or names are stored.
 */
public final class StatsBook {
    public static final String FILE = "generator_v2_stats.json";
    static final int MAX_RECORDS = 300;
    static final int MAX_SAMPLES = 400;

    /** Per-quest record. */
    static final class Rec {
        String id;
        long cycle;
        String difficulty;
        List<String> types = new ArrayList<>();
        int tier;
        List<String> families = new ArrayList<>();
        List<String> profiles = new ArrayList<>();
        double est;
        long generated;
        long claims;
        long lastClaim;
        long completions;
        long lastCompletion;
        long abandons;
        long expiries;
    }

    /** One claim→complete observation. */
    record Sample(double observedMin, double estimatedMin, String key) {
    }

    private final LinkedHashMap<String, Rec> records = new LinkedHashMap<>();
    private final Map<String, long[]> aggType = new TreeMap<>();
    private final Map<String, long[]> aggTier = new TreeMap<>();
    private final Map<String, long[]> aggDifficulty = new TreeMap<>();
    private final Map<String, long[]> aggProfile = new TreeMap<>();
    private final List<Sample> samples = new ArrayList<>();
    private final Map<String, Long> relaxations = new TreeMap<>();
    private final Map<String, Long> rejections = new TreeMap<>();

    public void onGenerated(QuestRecord q, long now) {
        Rec r = new Rec();
        r.id = q.id;
        r.cycle = q.cycleId;
        r.difficulty = q.difficulty;
        r.types = new ArrayList<>(q.types);
        r.tier = q.tier;
        r.families = new ArrayList<>(q.families);
        r.profiles = new ArrayList<>(q.profiles);
        r.est = q.estMinutes;
        r.generated = now;
        records.put(r.id, r);
        prune();
    }

    public void onClaimed(String id, long now) {
        Rec r = records.get(id);
        if (r != null) {
            r.claims++;
            r.lastClaim = now;
        }
    }

    public void onCompleted(String id, long now, double observedMinutes) {
        Rec r = records.get(id);
        if (r == null) {
            return;
        }
        r.completions++;
        r.lastCompletion = now;
        if (observedMinutes > 0 && r.est > 0) {
            String key = r.families.size() == 1 && r.types.size() == 1 ? r.families.get(0) + "|" + r.types.get(0) : "";
            samples.add(new Sample(observedMinutes, r.est, key));
            while (samples.size() > MAX_SAMPLES) {
                samples.remove(0);
            }
        }
    }

    public void onAbandoned(String id) {
        Rec r = records.get(id);
        if (r != null) {
            r.abandons++;
        }
    }

    public void onExpired(String id) {
        Rec r = records.get(id);
        if (r != null) {
            r.expiries++;
        }
    }

    public void addRelaxations(Map<String, Long> m) {
        m.forEach((k, v) -> relaxations.merge(k, v, Long::sum));
    }

    public void addRejections(Map<String, Long> m) {
        m.forEach((k, v) -> rejections.merge(k, v, Long::sum));
    }

    private void prune() {
        while (records.size() > MAX_RECORDS) {
            String first = records.keySet().iterator().next();
            Rec r = records.remove(first);
            fold(r);
        }
    }

    private void fold(Rec r) {
        long[] c = counts(r);
        for (String t : r.types) {
            add(aggType, t, c);
        }
        add(aggTier, Integer.toString(r.tier), c);
        add(aggDifficulty, r.difficulty, c);
        for (String p : r.profiles) {
            add(aggProfile, p, c);
        }
    }

    private static long[] counts(Rec r) {
        return new long[]{1, r.claims, r.completions, r.abandons, r.expiries};
    }

    private static void add(Map<String, long[]> m, String key, long[] c) {
        long[] cur = m.computeIfAbsent(key, k -> new long[5]);
        for (int i = 0; i < 5; i++) {
            cur[i] += c[i];
        }
    }

    /** family|type → median(observed/estimated) for keys with at least {@code minSamples}. */
    public Map<String, Double> calibrationRatios(int minSamples) {
        Map<String, List<Double>> by = new TreeMap<>();
        for (Sample s : samples) {
            if (!s.key().isEmpty() && s.estimatedMin() > 0) {
                by.computeIfAbsent(s.key(), k -> new ArrayList<>()).add(s.observedMin() / s.estimatedMin());
            }
        }
        Map<String, Double> out = new TreeMap<>();
        by.forEach((k, v) -> {
            if (v.size() >= minSamples) {
                out.put(k, Medians.median(v));
            }
        });
        return out;
    }

    public StatsSummary summary(boolean enabled) {
        Map<String, long[]> byType = copy(aggType);
        Map<String, long[]> byTier = copy(aggTier);
        Map<String, long[]> byDiff = copy(aggDifficulty);
        Map<String, long[]> byProfile = copy(aggProfile);
        long gen = 0;
        long claims = 0;
        long done = 0;
        long abandoned = 0;
        long expired = 0;
        for (long[] c : aggDifficulty.values()) {
            gen += c[0];
            claims += c[1];
            done += c[2];
            abandoned += c[3];
            expired += c[4];
        }
        for (Rec r : records.values()) {
            long[] c = counts(r);
            for (String t : r.types) {
                add(byType, t, c);
            }
            add(byTier, Integer.toString(r.tier), c);
            add(byDiff, r.difficulty, c);
            for (String p : r.profiles) {
                add(byProfile, p, c);
            }
            gen += c[0];
            claims += c[1];
            done += c[2];
            abandoned += c[3];
            expired += c[4];
        }
        List<Double> observed = new ArrayList<>();
        List<Double> estimated = new ArrayList<>();
        List<Double> ratios = new ArrayList<>();
        Map<String, List<Double>> byKey = new TreeMap<>();
        for (Sample s : samples) {
            observed.add(s.observedMin());
            estimated.add(s.estimatedMin());
            if (s.estimatedMin() > 0) {
                ratios.add(s.observedMin() / s.estimatedMin());
                if (!s.key().isEmpty()) {
                    byKey.computeIfAbsent(s.key(), k -> new ArrayList<>()).add(s.observedMin() / s.estimatedMin());
                }
            }
        }
        Map<String, Double> cal = new LinkedHashMap<>();
        byKey.forEach((k, v) -> cal.put(k, Medians.median(v)));
        return new StatsSummary(enabled, gen, claims, done, abandoned, expired, buckets(byType), buckets(byTier),
            buckets(byDiff), buckets(byProfile), Medians.median(observed), Medians.median(estimated),
            Medians.median(ratios), cal, claims == 0 ? 0.0 : (double) abandoned / claims, relaxations, rejections);
    }

    private static Map<String, long[]> copy(Map<String, long[]> m) {
        Map<String, long[]> out = new TreeMap<>();
        m.forEach((k, v) -> out.put(k, v.clone()));
        return out;
    }

    private static Map<String, StatsSummary.Bucket> buckets(Map<String, long[]> m) {
        Map<String, StatsSummary.Bucket> out = new LinkedHashMap<>();
        m.forEach((k, c) -> out.put(k, new StatsSummary.Bucket(c[0], c[1], c[2], c[3], c[4])));
        return out;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("format", 1);
        JsonArray recs = new JsonArray();
        for (Rec r : records.values()) {
            JsonObject j = new JsonObject();
            j.addProperty("id", r.id);
            j.addProperty("cycle", r.cycle);
            j.addProperty("difficulty", r.difficulty);
            j.add("types", Json.toArray(r.types));
            j.addProperty("tier", r.tier);
            j.add("families", Json.toArray(r.families));
            j.add("profiles", Json.toArray(r.profiles));
            j.addProperty("est", Math.round(r.est * 10.0) / 10.0);
            j.addProperty("generated", r.generated);
            j.addProperty("claims", r.claims);
            j.addProperty("lastClaim", r.lastClaim);
            j.addProperty("completions", r.completions);
            j.addProperty("lastCompletion", r.lastCompletion);
            j.addProperty("abandons", r.abandons);
            j.addProperty("expiries", r.expiries);
            recs.add(j);
        }
        o.add("records", recs);
        JsonObject agg = new JsonObject();
        agg.add("byType", aggJson(aggType));
        agg.add("byTier", aggJson(aggTier));
        agg.add("byDifficulty", aggJson(aggDifficulty));
        agg.add("byProfile", aggJson(aggProfile));
        o.add("aggregate", agg);
        JsonArray s = new JsonArray();
        for (Sample x : samples) {
            JsonArray a = new JsonArray();
            a.add(Math.round(x.observedMin() * 10.0) / 10.0);
            a.add(Math.round(x.estimatedMin() * 10.0) / 10.0);
            a.add(x.key());
            s.add(a);
        }
        o.add("samples", s);
        o.add("relaxations", Json.toObject(relaxations));
        o.add("rejections", Json.toObject(rejections));
        return o;
    }

    private static JsonObject aggJson(Map<String, long[]> m) {
        JsonObject o = new JsonObject();
        m.forEach((k, v) -> {
            JsonArray a = new JsonArray();
            for (long x : v) {
                a.add(x);
            }
            o.add(k, a);
        });
        return o;
    }

    public static StatsBook fromJson(JsonObject o) {
        StatsBook b = new StatsBook();
        for (JsonObject j : Json.objects(o, "records")) {
            Rec r = new Rec();
            r.id = Json.str(j, "id", null);
            if (r.id == null) {
                continue;
            }
            r.cycle = Json.lng(j, "cycle", 0);
            r.difficulty = Json.str(j, "difficulty", "NORMAL");
            r.types = Json.strings(j, "types");
            r.tier = Json.integer(j, "tier", 0);
            r.families = Json.strings(j, "families");
            r.profiles = Json.strings(j, "profiles");
            r.est = Json.dbl(j, "est", 0);
            r.generated = Json.lng(j, "generated", 0);
            r.claims = Json.lng(j, "claims", 0);
            r.lastClaim = Json.lng(j, "lastClaim", 0);
            r.completions = Json.lng(j, "completions", 0);
            r.lastCompletion = Json.lng(j, "lastCompletion", 0);
            r.abandons = Json.lng(j, "abandons", 0);
            r.expiries = Json.lng(j, "expiries", 0);
            b.records.put(r.id, r);
        }
        JsonObject agg = Json.obj(o, "aggregate");
        readAgg(Json.obj(agg, "byType"), b.aggType);
        readAgg(Json.obj(agg, "byTier"), b.aggTier);
        readAgg(Json.obj(agg, "byDifficulty"), b.aggDifficulty);
        readAgg(Json.obj(agg, "byProfile"), b.aggProfile);
        JsonArray s = Json.arr(o, "samples");
        if (s != null) {
            for (JsonElement e : s) {
                if (e.isJsonArray() && e.getAsJsonArray().size() == 3) {
                    JsonArray a = e.getAsJsonArray();
                    try {
                        b.samples.add(new Sample(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsString()));
                    } catch (RuntimeException ignored) {
                        // skip malformed sample
                    }
                }
            }
        }
        readLongs(Json.obj(o, "relaxations"), b.relaxations);
        readLongs(Json.obj(o, "rejections"), b.rejections);
        b.prune();
        return b;
    }

    private static void readAgg(JsonObject o, Map<String, long[]> into) {
        if (o == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (e.getValue().isJsonArray() && e.getValue().getAsJsonArray().size() == 5) {
                long[] v = new long[5];
                for (int i = 0; i < 5; i++) {
                    v[i] = e.getValue().getAsJsonArray().get(i).getAsLong();
                }
                into.put(e.getKey(), v);
            }
        }
    }

    private static void readLongs(JsonObject o, Map<String, Long> into) {
        if (o == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
                into.put(e.getKey(), e.getValue().getAsLong());
            }
        }
    }
}
