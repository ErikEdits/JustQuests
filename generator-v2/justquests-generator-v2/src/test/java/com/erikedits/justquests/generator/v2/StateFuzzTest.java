package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hand-edited or half-written state files: random structural mutations of a real
 * {@code generator_v2.json} and {@code generator_v2_stats.json} must never leak an exception out
 * of the core. The generator either reads what it can or backs the file up and starts fresh, and
 * it keeps serving valid quests afterwards.
 */
class StateFuzzTest {
    private static final String STATE = "generator_v2.json";
    private static final String STATS = "generator_v2_stats.json";
    private static final long HOUR = 3_600_000L;

    /** A store with a few rotations, claims, completions and stats. */
    private static FakeHost seededHost() {
        FakeHost h = new FakeHost().withMods("farmersdelight");
        GeneratorConfig cfg = TestSupport.config(Difficulty.NORMAL, 8);
        QuestGeneratorV2 g = new QuestGeneratorV2(h, cfg);
        g.start(Map.of());
        UUID a = new UUID(1, 1);
        UUID b = new UUID(1, 2);
        for (int day = 0; day < 3; day++) {
            List<String> ids = new ArrayList<>(g.servedQuests().keySet());
            g.tryClaim(ids.get(0), a);
            h.now += HOUR;
            g.onComplete(ids.get(0), a);
            g.tryClaim(ids.get(1), b);
            g.tryClaim(ids.get(2), a);
            h.now += 25 * HOUR;
            g.tick();
        }
        g.stop();
        return h;
    }

    private static JsonElement randomValue(SplittableRandom rng) {
        switch (rng.nextInt(9)) {
            case 0:
                return JsonNull.INSTANCE;
            case 1:
                return new JsonPrimitive("garbage");
            case 2:
                return new JsonPrimitive(-rng.nextInt(1_000_000));
            case 3:
                return new JsonPrimitive(Long.MAX_VALUE);
            case 4:
                return new JsonPrimitive(rng.nextBoolean());
            case 5:
                return new JsonArray();
            case 6:
                return new JsonObject();
            case 7:
                return new JsonPrimitive(1.5e300);
            default:
                return new JsonPrimitive("not-a-uuid");
        }
    }

    /** Collects every object and array in the tree so a random one can be mutated. */
    private static void containers(JsonElement e, List<JsonElement> out) {
        if (e.isJsonObject()) {
            out.add(e);
            for (Map.Entry<String, JsonElement> m : e.getAsJsonObject().entrySet()) {
                containers(m.getValue(), out);
            }
        } else if (e.isJsonArray()) {
            out.add(e);
            for (JsonElement c : e.getAsJsonArray()) {
                containers(c, out);
            }
        }
    }

    private static String mutate(String json, SplittableRandom rng) {
        if (rng.nextInt(8) == 0) {
            // half-written file
            return json.substring(0, rng.nextInt(Math.max(1, json.length())));
        }
        JsonElement root = JsonParser.parseString(json);
        int edits = 1 + rng.nextInt(4);
        for (int i = 0; i < edits; i++) {
            List<JsonElement> all = new ArrayList<>();
            containers(root, all);
            JsonElement c = all.get(rng.nextInt(all.size()));
            if (c.isJsonObject()) {
                JsonObject o = c.getAsJsonObject();
                List<String> keys = new ArrayList<>(o.keySet());
                if (keys.isEmpty()) {
                    o.add("unexpected", randomValue(rng));
                    continue;
                }
                String k = keys.get(rng.nextInt(keys.size()));
                if (rng.nextBoolean()) {
                    o.remove(k);
                } else {
                    o.add(k, randomValue(rng));
                }
            } else {
                JsonArray a = c.getAsJsonArray();
                if (a.size() == 0 || rng.nextInt(3) == 0) {
                    a.add(randomValue(rng));
                } else if (rng.nextBoolean()) {
                    a.set(rng.nextInt(a.size()), randomValue(rng));
                } else {
                    a.add(a.get(rng.nextInt(a.size())).deepCopy());
                }
            }
        }
        return root.toString();
    }

    @Test
    void mutatedStateFilesNeverBreakTheGenerator() {
        FakeHost seed = seededHost();
        String state = seed.store.files.get(STATE);
        String stats = seed.store.files.get(STATS);
        assertTrue(state != null && state.length() > 200, "seed state");
        SplittableRandom rng = new SplittableRandom(20260927L);
        int backups = 0;
        for (int round = 0; round < 250; round++) {
            FakeHost h = new FakeHost().withMods("farmersdelight");
            h.now = seed.now + rng.nextInt(48) * HOUR;
            h.store.files.put(STATE, rng.nextInt(5) == 0 ? state : mutate(state, rng));
            if (stats != null) {
                h.store.files.put(STATS, rng.nextInt(3) == 0 ? stats : mutate(stats, rng));
            }
            GeneratorConfig cfg = TestSupport.config(Difficulty.NORMAL, 8);
            QuestGeneratorV2 g = new QuestGeneratorV2(h, cfg);
            g.start(Map.of("justquests:gen/1_1", new UUID(9, 9)));
            String ctx = "round " + round;
            List<String> leaked = h.log.lines.stream().filter(l -> l.contains("failed; continuing")).toList();
            assertTrue(leaked.isEmpty(), ctx + ": exception escaped the core: " + leaked);
            assertFalse(g.servedQuests().isEmpty(), ctx + ": nothing served\n" + h.log.lines);
            for (Map.Entry<String, com.google.gson.JsonObject> e : g.servedQuests().entrySet()) {
                assertTrue(StrictQuestCheck.check(e.getValue(), h.caps).isEmpty(), ctx + " " + e.getKey());
            }
            String q = g.servedQuests().keySet().iterator().next();
            ClaimResult r = g.tryClaim(q, new UUID(3, 3));
            assertTrue(r == ClaimResult.OK || r == ClaimResult.CLAIMED_BY_OTHER || r == ClaimResult.COMPLETED
                || r == ClaimResult.NOT_SERVED, ctx + " " + r);
            h.now += 30 * HOUR;
            g.tick();
            g.reroll();
            g.stats();
            g.status();
            Map<String, List<UUID>> view = new java.util.HashMap<>();
            for (String id : g.servedQuests().keySet()) {
                if (!g.holders(id).isEmpty()) {
                    view.put(id, g.holders(id));
                }
            }
            g.stop();
            leaked = h.log.lines.stream().filter(l -> l.contains("failed; continuing")).toList();
            assertTrue(leaked.isEmpty(), ctx + ": exception escaped the core later: " + leaked);
            backups += (int) h.store.files.keySet().stream().filter(f -> f.contains(".corrupt-")).count();
            // the rewritten file must load cleanly next time
            FakeHost again = new FakeHost().withMods("farmersdelight");
            again.now = h.now;
            again.store.files.putAll(h.store.files);
            QuestGeneratorV2 g2 = new QuestGeneratorV2(again, cfg);
            g2.startWithHolders(view);
            assertEquals(0, again.log.lines.stream().filter(l -> l.contains("unreadable")).count(),
                ctx + ": the file the generator wrote is unreadable: " + again.log.lines);
            assertEquals(g.servedQuests().keySet(), g2.servedQuests().keySet(), ctx);
        }
        assertTrue(backups > 0, "some mutations must have been caught as corrupt");
    }

    @Test
    void schemaCheckNeverThrowsOnMalformedQuests() {
        FakeHost h = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(h, TestSupport.config(Difficulty.HARD, 12));
        g.start(Map.of());
        List<String> samples = new ArrayList<>();
        g.servedQuests().values().forEach(q -> samples.add(q.toString()));
        SplittableRandom rng = new SplittableRandom(99L);
        int rejected = 0;
        for (int i = 0; i < 3000; i++) {
            String json = mutate(samples.get(rng.nextInt(samples.size())), rng);
            JsonElement e;
            try {
                e = JsonParser.parseString(json);
            } catch (RuntimeException truncated) {
                continue;
            }
            if (!e.isJsonObject()) {
                continue;
            }
            List<String> problems = com.erikedits.justquests.generator.v2.internal.gen.SchemaCheck.check(e.getAsJsonObject(),
                h.caps);
            if (!problems.isEmpty()) {
                rejected++;
            }
        }
        assertTrue(rejected > 1000, "most mutations must be caught: " + rejected);
    }

    @Test
    void questsOfARemovedModAreDroppedReportedDeadAndReplaced() {
        FakeHost h = new FakeHost().withMods("farmersdelight");
        GeneratorConfig cfg = TestSupport.config(Difficulty.NORMAL, 20).withModdedShare(1.0);
        QuestGeneratorV2 g = new QuestGeneratorV2(h, cfg);
        g.start(Map.of());
        String fd = g.servedQuests().entrySet().stream().filter(e -> e.getValue().toString().contains("farmersdelight:"))
            .map(Map.Entry::getKey).findFirst().orElseThrow();
        UUID a = new UUID(4, 4);
        assertEquals(ClaimResult.OK, g.tryClaim(fd, a));
        g.stop();
        // the mod is removed: its ids fail the host's codec now
        FakeHost after = new FakeHost();
        after.now = h.now + 60_000L;
        after.store.files.putAll(h.store.files);
        after.validator.reject = q -> q.toString().contains("farmersdelight:");
        QuestGeneratorV2 g2 = new QuestGeneratorV2(after, cfg);
        var start = g2.start(Map.of(fd, a));
        assertTrue(start.deadQuestIds().contains(fd), "the held quest is reported dead: " + start);
        assertEquals(20, g2.servedQuests().size(), "the set is topped up: " + after.log.lines);
        assertTrue(g2.servedQuests().values().stream().noneMatch(q -> q.toString().contains("farmersdelight:")));
        assertTrue(after.log.lines.stream().anyMatch(l -> l.contains("no longer valid")));
        assertTrue(g2.selfTest().isEmpty(), g2.selfTest().toString());
    }
}
