package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.ClaimState;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * §16.8 / §10.3 — the v1 bug: a rotation must never drop an accepted quest; nor a finished one
 * whose rewards still wait to be claimed.
 */
class RetentionTest {
    private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

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
    void finishedQuestStaysUntilItsRewardsAreClaimed() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        String q = g.servedQuests().keySet().iterator().next();
        JsonObject def = g.servedQuests().get(q);
        assertEquals(ClaimResult.OK, g.tryClaim(q, P));
        g.onComplete(q, P, true);
        for (int i = 0; i < 3; i++) {
            host.now += 12 * 3_600_000L;
            RotationResult r = g.tick();
            assertTrue(r.retained().contains(q), "a quest whose rewards wait must be retained");
            assertEquals(def, g.servedQuests().get(q), "the definition stays, so the rewards can be paid");
            assertTrue(g.isRetained(q));
            assertEquals(List.of(P), g.awaitingRewards(q));
            assertEquals(ClaimState.COMPLETED, g.claim(q).state());
            assertFalse(g.tryClaim(q, OTHER).proceed(), "nobody can take a finished quest");
        }
        long rev = g.servedRevision();
        g.onRewardsClaimed(q, P);
        assertTrue(g.servedRevision() > rev, "the host must re-register");
        assertFalse(g.servedQuests().containsKey(q));
        assertTrue(g.awaitingRewards(q).isEmpty());
    }

    @Test
    void rewardsClaimedBeforeTheRotationLeaveNothingBehind() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        String q = g.servedQuests().keySet().iterator().next();
        g.tryClaim(q, P);
        g.onComplete(q, P, true);
        g.onRewardsClaimed(q, P);
        assertTrue(g.servedQuests().containsKey(q), "a current-cycle quest stays until the rotation");
        host.now += 12 * 3_600_000L;
        assertTrue(g.tick().removed().contains(q));
        assertFalse(g.servedQuests().containsKey(q));
    }

    @Test
    void waitingRewardsSurviveRestartsAndAreReconciled() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        String q = g.servedQuests().keySet().iterator().next();
        g.tryClaim(q, P);
        g.onComplete(q, P, true);
        g.stop();

        host.now += 3 * 24 * 3_600_000L;
        QuestGeneratorV2 g2 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g2.startWithHolders(Map.of(), Map.of(q, Set.of(P)));
        assertTrue(g2.servedQuests().containsKey(q), "kept through downtime and the catch-up rotation");
        g2.stop();

        // a host without the waiting map (older call) keeps what is stored
        QuestGeneratorV2 g3 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g3.startWithHolders(Map.of());
        assertTrue(g3.servedQuests().containsKey(q));
        g3.stop();

        // the rewards were claimed (or reset) while the server was down: the quest goes
        QuestGeneratorV2 g4 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g4.startWithHolders(Map.of(), Map.of());
        assertFalse(g4.servedQuests().containsKey(q));
    }

    @Test
    void switchedOffOnlyQuestsWithWaitingRewardsAreServed() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        String q = g.servedQuests().keySet().iterator().next();
        g.tryClaim(q, P);
        g.onComplete(q, P, true);
        g.updateConfig(TestSupport.config(Difficulty.NORMAL, 5).toBuilder().enabled(false).build());
        assertEquals(Set.of(q), g.servedQuests().keySet());
        g.onRewardsClaimed(q, P);
        assertTrue(g.servedQuests().isEmpty());
    }

    @Test
    void adminResetDropsWaitingRewards() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        String q = g.servedQuests().keySet().iterator().next();
        g.tryClaim(q, P);
        g.onComplete(q, P, true);
        host.now += 12 * 3_600_000L;
        g.tick();
        assertTrue(g.servedQuests().containsKey(q));
        g.releaseAllFor(P);
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
