package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.catalog.CatalogLoader;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.16 — generation &lt; 50 ms warm for 20 quests; no-op tick &lt; 0.1 ms; catalog load &lt; 200 ms. */
class PerformanceTest {
    @Test
    void generationIsFast() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create");
        Progression p = TestSupport.unlockedProgression(host);
        for (int i = 0; i < 60; i++) {
            TestSupport.generate(host, TestSupport.config(Difficulty.HARD, 20), p, i, 20);
        }
        int runs = 100;
        long t0 = System.nanoTime();
        for (int i = 0; i < runs; i++) {
            TestSupport.generate(host, TestSupport.config(Difficulty.values()[i % 3], 20), p, 1000 + i, 20);
        }
        double avgMs = (System.nanoTime() - t0) / 1e6 / runs;
        assertTrue(avgMs < 50.0, "average generation " + avgMs + " ms");
    }

    @Test
    void noOpTickIsCheap() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 20));
        g.start(Map.of());
        for (int i = 0; i < 10_000; i++) {
            g.tick();
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            assertFalse(g.tick().changed());
        }
        double avgMs = (System.nanoTime() - t0) / 1e6 / 10_000;
        assertTrue(avgMs < 0.1, "tick average " + avgMs + " ms");
    }

    @Test
    void catalogLoadIsFast() {
        FakeHost host = new FakeHost();
        new CatalogLoader(host.log, host.store).load();
        long t0 = System.nanoTime();
        new CatalogLoader(host.log, host.store).load();
        double ms = (System.nanoTime() - t0) / 1e6;
        assertTrue(ms < 200.0, "catalog load " + ms + " ms");
    }
}
