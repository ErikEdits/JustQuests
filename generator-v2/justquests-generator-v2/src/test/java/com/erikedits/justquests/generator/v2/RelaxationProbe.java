package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

/** Diagnostic (not an assertion test): prints relaxation frequencies. Run with -Dprobe=true. */
class RelaxationProbe {
    @Test
    @Tag("probe")
    void stateSizes() {
        if (!Boolean.getBoolean("probe")) {
            return;
        }
        FakeHost host = new FakeHost().withMods("farmersdelight", "create", "mekanism", "botania");
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.HARD, 20));
        g.start(Map.of());
        for (int i = 0; i < 180; i++) {
            host.now += 12 * 3_600_000L;
            g.tick();
            var ids = java.util.List.copyOf(g.servedQuests().keySet());
            var p = java.util.UUID.randomUUID();
            if (g.tryClaim(ids.get(i % ids.size()), p).proceed() && i % 3 != 0) {
                g.onComplete(ids.get(i % ids.size()), p);
            }
        }
        System.out.println("state bytes " + host.store.files.get("generator_v2.json").length()
            + ", stats bytes " + host.store.files.get("generator_v2_stats.json").length());
    }

    @Test
    @Tag("probe")
    void probe() {
        if (!Boolean.getBoolean("probe")) {
            return;
        }
        for (boolean mods : new boolean[]{false, true}) {
            for (Difficulty d : Difficulty.values()) {
                for (int n : new int[]{1, 5, 10, 20}) {
                    FakeHost host = new FakeHost();
                    if (mods) {
                        host.withMods("farmersdelight", "create", "mekanism", "twilightforest", "botania");
                        host.content.dimensions.add("twilightforest:twilight_forest");
                    }
                    Progression p = TestSupport.unlockedProgression(host);
                    Map<String, Long> relax = new TreeMap<>();
                    int total = 0;
                    int relaxedSets = 0;
                    long t0 = System.nanoTime();
                    int runs = 200;
                    for (int seed = 1; seed <= runs; seed++) {
                        Generation.Result r = TestSupport.generate(host, TestSupport.config(d, n), p, seed, n);
                        total += r.drafts().size();
                        r.relaxations().forEach((k, v) -> relax.merge(k, v, Long::sum));
                        if (!r.relaxations().isEmpty()) {
                            relaxedSets++;
                        }
                    }
                    long ms = (System.nanoTime() - t0) / 1_000_000L;
                    System.out.printf("mods=%s %-6s n=%2d quests=%5d/%5d avg %.1f ms relaxedSets=%d/%d relax=%s%n", mods, d,
                        n, total, runs * n, ms / (double) runs, relaxedSets, runs, relax);
                }
            }
        }
    }
}
