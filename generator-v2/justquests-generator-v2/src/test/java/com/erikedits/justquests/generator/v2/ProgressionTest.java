package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.13 — Nether/End content only after unlock; the snapshot persists and never regresses. */
class ProgressionTest {
    private static boolean anyTier(FakeHost host, Progression p, int tier, int seeds) {
        for (long seed = 1; seed <= seeds; seed++) {
            for (QuestDraft q : TestSupport.generate(host, TestSupport.config(Difficulty.HARD, 10), p, seed, 10).drafts()) {
                if (q.tier() >= tier) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void freshWorldHasNoNetherOrEnd() {
        FakeHost host = new FakeHost();
        host.world.day = 0;
        host.world.online = 0;
        Progression p = new Progression();
        p.update(host.world, TestSupport.catalog().balance, TestSupport.catalog().profiles);
        assertFalse(anyTier(host, p, 3, 100));
    }

    @Test
    void netherUnlocksByShareAndDayAndPersists() {
        FakeHost host = new FakeHost();
        host.world.day = 1;
        Progression p = new Progression();
        host.world.shares.put("minecraft:story/enter_the_nether", 0.2);
        p.update(host.world, TestSupport.catalog().balance, TestSupport.catalog().profiles);
        assertFalse(p.nether(), "20 % < 25 % threshold");
        host.world.shares.put("minecraft:story/enter_the_nether", 0.5);
        p.update(host.world, TestSupport.catalog().balance, TestSupport.catalog().profiles);
        assertTrue(p.nether());
        assertFalse(p.end());
        assertTrue(anyTier(host, p, 3, 60));
        assertFalse(anyTier(host, p, 4, 100));
        host.world.shares.clear();
        host.world.online = 0;
        p.update(host.world, TestSupport.catalog().balance, TestSupport.catalog().profiles);
        assertTrue(p.nether(), "unlocks never regress");
        Progression restored = Progression.fromJson(Json.parseObject(Json.compact(p.toJson())));
        assertTrue(restored.nether());
        Progression byDay = new Progression();
        host.world.day = 10;
        byDay.update(host.world, TestSupport.catalog().balance, TestSupport.catalog().profiles);
        assertTrue(byDay.nether(), "day 10 unlocks the Nether");
        assertFalse(byDay.end());
        host.world.online = 2;
        host.world.shares.put("minecraft:end/root", 0.5);
        byDay.update(host.world, TestSupport.catalog().balance, TestSupport.catalog().profiles);
        assertTrue(byDay.end());
        assertTrue(anyTier(host, byDay, 4, 80));
    }

    @Test
    void snapshotSurvivesRestartWithNobodyOnline() {
        FakeHost host = new FakeHost();
        host.world.day = 1;
        host.world.shares.put("minecraft:story/enter_the_nether", 1.0);
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.HARD, 5));
        g.start(Map.of());
        g.stop();
        host.world.online = 0;
        host.world.shares.clear();
        QuestGeneratorV2 g2 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.HARD, 5));
        g2.start(Map.of());
        assertTrue(g2.status().contains("Nether unlocked"), g2.status());
    }
}
