package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.8 / §10.3 — the v1 bug: a rotation must never drop an accepted quest. */
class RetentionTest {
    private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void claimedQuestSurvivesRotationsUntilCompleted() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        String q = g.servedQuests().keySet().iterator().next();
        JsonObject def = g.servedQuests().get(q);
        assertEquals(ClaimResult.OK, g.tryClaim(q, P));
        for (int i = 0; i < 6; i++) {
            host.now += 12 * 3_600_000L;
            RotationResult r = g.tick();
            assertTrue(r.changed());
            assertTrue(r.retained().contains(q), "claimed quest must be retained");
            assertFalse(r.removed().contains(q));
            assertEquals(def, g.servedQuests().get(q), "definition must stay identical so progress keeps counting");
            assertEquals(5 + 1, g.servedQuests().size(), "retained quests do not reduce the new set");
            assertEquals(ClaimResult.ALREADY_YOURS, g.tryClaim(q, P));
        }
        g.onComplete(q, P);
        assertTrue(g.servedQuests().containsKey(q));
        host.now += 12 * 3_600_000L;
        RotationResult r = g.tick();
        assertTrue(r.removed().contains(q));
        assertFalse(g.servedQuests().containsKey(q));
    }

    @Test
    void retainedQuestSurvivesRestartAndCatchUp() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        String q = g.servedQuests().keySet().iterator().next();
        g.tryClaim(q, P);
        g.stop();
        host.now += 3 * 24 * 3_600_000L;
        QuestGeneratorV2 g2 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        var start = g2.start(Map.of(q, P));
        assertEquals("catch-up", start.initialRotation().reason());
        assertTrue(g2.servedQuests().containsKey(q));
        assertTrue(start.deadQuestIds().isEmpty());
    }
}
