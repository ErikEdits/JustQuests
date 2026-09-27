package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.StatsSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §15 — anonymous stats with bounded size; optional self-calibration (§9.9). */
class StatsTest {
    private static final long MIN = 60_000L;

    @Test
    void recordsAnonymousAggregates() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        List<String> ids = List.copyOf(g.servedQuests().keySet());
        UUID p = UUID.randomUUID();
        g.tryClaim(ids.get(0), p);
        host.now += 12 * MIN;
        g.onComplete(ids.get(0), p);
        g.tryClaim(ids.get(1), p);
        g.onAbandon(ids.get(1), p);
        StatsSummary s = g.stats();
        assertEquals(5, s.questsGenerated());
        assertEquals(2, s.questsClaimed());
        assertEquals(1, s.questsCompleted());
        assertEquals(1, s.questsAbandoned());
        assertEquals(12.0, s.medianClaimToCompleteMin(), 1e-9);
        assertEquals(0.5, s.abandonRate(), 1e-9);
        assertFalse(s.byDifficulty().isEmpty());
        assertTrue(s.byProfile().containsKey("vanilla"));
        assertTrue(s.toText().contains("Generated 5"));
        String file = host.store.files.get("generator_v2_stats.json");
        assertFalse(file.contains(p.toString()), "stats must not contain player UUIDs");
    }

    @Test
    void statsFileStaysSmallAndStateIsBounded() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create");
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.HARD, 20));
        g.start(Map.of());
        for (int i = 0; i < 180; i++) { // 90 days
            host.now += 12 * 60 * MIN;
            g.tick();
            List<String> ids = List.copyOf(g.servedQuests().keySet());
            UUID p = UUID.randomUUID();
            if (g.tryClaim(ids.get(i % ids.size()), p).proceed()) {
                host.now += 20 * MIN;
                g.onComplete(ids.get(i % ids.size()), p);
            }
        }
        assertTrue(host.store.files.get("generator_v2_stats.json").length() < 256 * 1024, "stats file size");
        assertTrue(host.store.files.get("generator_v2.json").length() < 256 * 1024, "state file size");
        assertEquals(180L * 20 + 20, g.stats().questsGenerated());
    }

    @Test
    void disabledStatsWriteNothing() {
        FakeHost host = new FakeHost();
        GeneratorConfig cfg = TestSupport.config(Difficulty.NORMAL, 5).toBuilder().statsEnabled(false).build();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, cfg);
        g.start(Map.of());
        assertFalse(host.store.files.containsKey("generator_v2_stats.json"));
        assertFalse(g.stats().enabled());
    }

    @Test
    void adaptiveBalancingMovesSlowly() {
        FakeHost host = new FakeHost();
        GeneratorConfig cfg = TestSupport.config(Difficulty.NORMAL, 20).toBuilder().adaptiveBalancing(true)
            .oneActivePerPlayer(false).build();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, cfg);
        g.start(Map.of());
        Map<String, Double> previous = Map.of();
        boolean moved = false;
        for (int cycle = 0; cycle < 40; cycle++) {
            // players take three times as long as estimated for everything (state stores 3 decimals)
            for (String id : List.copyOf(g.servedQuests().keySet())) {
                UUID p = UUID.randomUUID();
                if (g.tryClaim(id, p).proceed()) {
                    double est = estimate(g.explain(id));
                    host.now += (long) (3 * est * MIN);
                    g.onComplete(id, p);
                }
            }
            host.now += 12 * 60 * MIN;
            g.tick();
            Map<String, Double> cal = calibration(host);
            for (Map.Entry<String, Double> e : cal.entrySet()) {
                assertTrue(e.getValue() >= 0.5 && e.getValue() <= 2.0, "bounded multiplier " + e);
                double before = previous.getOrDefault(e.getKey(), 1.0);
                assertTrue(Math.abs(e.getValue() / before - 1.0) <= 0.10 + 0.002, "step > 10 %: " + e + " from " + before);
                moved |= e.getValue() > 1.0;
            }
            previous = cal;
        }
        assertTrue(moved, "calibration should slow estimates down: " + previous);
        boolean shown = false;
        for (String id : g.servedQuests().keySet()) {
            shown |= g.explain(id).contains("calibration");
        }
        assertTrue(shown, "calibration visible in explain");
    }

    private static double estimate(String explain) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("estimated ([0-9.]+) min").matcher(explain);
        return m.find() ? Double.parseDouble(m.group(1)) : 10.0;
    }

    private static Map<String, Double> calibration(FakeHost host) {
        var state = com.erikedits.justquests.generator.v2.internal.util.Json.parseObject(host.store.files.get("generator_v2.json"));
        Map<String, Double> out = new java.util.TreeMap<>();
        state.getAsJsonObject("calibration").entrySet().forEach(e -> out.put(e.getKey(), e.getValue().getAsDouble()));
        return out;
    }
}
