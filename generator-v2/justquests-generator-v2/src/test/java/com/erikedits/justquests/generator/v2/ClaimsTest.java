package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.ClaimState;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.7 — the claim state machine of §10, every transition and denial. */
class ClaimsTest {
    static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    static final long HOUR = 3_600_000L;

    static QuestGeneratorV2 gen(FakeHost host, GeneratorConfig cfg) {
        QuestGeneratorV2 g = new QuestGeneratorV2(host, cfg);
        g.start(Map.of());
        return g;
    }

    static List<String> ids(QuestGeneratorV2 g) {
        return new ArrayList<>(g.servedQuests().keySet());
    }

    @Test
    void basicTransitionsAndDenials() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = gen(host, TestSupport.config(Difficulty.NORMAL, 5));
        List<String> q = ids(g);
        assertEquals(ClaimResult.NOT_GENERATED, g.tryClaim("justquests:starter/wood", A));
        assertEquals(ClaimResult.NOT_SERVED, g.tryClaim("justquests:gen/1_99", A));
        assertEquals(ClaimResult.OK, g.tryClaim(q.get(0), A));
        assertEquals(ClaimState.CLAIMED, g.claim(q.get(0)).state());
        assertEquals(A, g.claim(q.get(0)).holder());
        assertEquals(host.now, g.claim(q.get(0)).claimedAtMillis());
        assertEquals(ClaimResult.ALREADY_YOURS, g.tryClaim(q.get(0), A));
        assertEquals(ClaimResult.CLAIMED_BY_OTHER, g.tryClaim(q.get(0), B));
        assertEquals(ClaimResult.HAS_ACTIVE_GENERATED, g.tryClaim(q.get(1), A));
        assertEquals(ClaimResult.OK, g.tryClaim(q.get(1), B));
        g.onComplete(q.get(0), A);
        assertEquals(ClaimState.COMPLETED, g.claim(q.get(0)).state());
        assertEquals(ClaimResult.COMPLETED, g.tryClaim(q.get(0), C));
        assertEquals(ClaimResult.COMPLETED, g.tryClaim(q.get(0), A));
        assertEquals(ClaimResult.OK, g.tryClaim(q.get(2), A), "one-active rule must free A after completion");
        g.onAbandon(q.get(2), A);
        assertEquals(ClaimState.AVAILABLE, g.claim(q.get(2)).state());
        assertEquals(ClaimResult.OK, g.tryClaim(q.get(2), C));
        assertEquals(ClaimView0.AVAILABLE, g.claim("justquests:gen/unknown_1").state());
        g.updateConfig(TestSupport.config(Difficulty.NORMAL, 5).withEnabled(false));
        assertEquals(ClaimResult.DISABLED, g.tryClaim(q.get(3), A));
        assertTrue(g.servedQuests().isEmpty());
        assertEquals(ClaimResult.NOT_GENERATED, g.tryClaim("minecraft:whatever", A));
        g.updateConfig(TestSupport.config(Difficulty.NORMAL, 5));
        assertEquals(5, g.servedQuests().size());
        assertEquals(ClaimState.CLAIMED, g.claim(q.get(2)).state(), "claims survive disable/enable");
    }

    /** Alias to keep the assertion above readable. */
    static final class ClaimView0 {
        static final ClaimState AVAILABLE = ClaimState.AVAILABLE;
    }

    @Test
    void nonExclusiveAllowsSharedQuests() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = gen(host, TestSupport.config(Difficulty.NORMAL, 5).withExclusiveClaims(false));
        String q = ids(g).get(0);
        assertEquals(ClaimResult.OK, g.tryClaim(q, A));
        assertEquals(ClaimResult.OK, g.tryClaim(q, B));
        assertEquals(List.of(A, B), g.holders(q));
        g.onComplete(q, A);
        assertEquals(ClaimState.CLAIMED, g.claim(q).state(), "B still holds it");
        assertEquals(ClaimResult.COMPLETED, g.tryClaim(q, A), "A already finished it");
        g.onComplete(q, B);
        assertEquals(ClaimState.AVAILABLE, g.claim(q).state(), "completion does not lock it for others");
        assertEquals(ClaimResult.OK, g.tryClaim(q, C));
    }

    @Test
    void oneActiveRuleCanBeDisabled() {
        FakeHost host = new FakeHost();
        GeneratorConfig cfg = TestSupport.config(Difficulty.NORMAL, 5).withOneActivePerPlayer(false);
        QuestGeneratorV2 g = gen(host, cfg);
        List<String> q = ids(g);
        assertEquals(ClaimResult.OK, g.tryClaim(q.get(0), A));
        assertEquals(ClaimResult.OK, g.tryClaim(q.get(1), A));
    }

    @Test
    void abandonCurrentVsRetained() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = gen(host, TestSupport.config(Difficulty.NORMAL, 5));
        List<String> q = ids(g);
        assertEquals(ClaimResult.OK, g.tryClaim(q.get(0), A));
        g.onAbandon(q.get(0), A);
        assertTrue(g.servedQuests().containsKey(q.get(0)), "current-cycle quest stays offered");
        assertEquals(ClaimState.AVAILABLE, g.claim(q.get(0)).state());
        assertEquals(ClaimResult.OK, g.tryClaim(q.get(1), A));
        host.now += 12 * HOUR;
        RotationResult r = g.tick();
        assertTrue(r.changed());
        assertTrue(r.retained().contains(q.get(1)));
        long rev = g.servedRevision();
        g.onAbandon(q.get(1), A);
        assertFalse(g.servedQuests().containsKey(q.get(1)), "abandoned retained quest is removed entirely");
        assertTrue(g.servedRevision() > rev);
        assertEquals(ClaimResult.NOT_SERVED, g.tryClaim(q.get(1), B));
        g.onAbandon("justquests:gen/1_1", A); // unknown: no-op
    }

    @Test
    void noReleaseOnAbandonRemovesQuest() {
        FakeHost host = new FakeHost();
        GeneratorConfig cfg = TestSupport.config(Difficulty.NORMAL, 5).toBuilder().releaseOnAbandon(false).build();
        QuestGeneratorV2 g = gen(host, cfg);
        String q = ids(g).get(0);
        g.tryClaim(q, A);
        g.onAbandon(q, A);
        assertFalse(g.servedQuests().containsKey(q));
        assertEquals(ClaimResult.NOT_SERVED, g.tryClaim(q, B));
    }

    @Test
    void expiryReleasesClaims() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = gen(host, TestSupport.config(Difficulty.NORMAL, 5).withClaimExpiryHours(2));
        List<String> q = ids(g);
        host.now += 30 * 60_000L;
        assertEquals(ClaimResult.OK, g.tryClaim(q.get(0), A));
        host.now += HOUR;
        assertTrue(g.tick().expiredClaims().isEmpty());
        host.now += HOUR + 1;
        RotationResult r = g.tick();
        assertEquals(1, r.expiredClaims().size());
        assertEquals(q.get(0), r.expiredClaims().get(0).questId());
        assertEquals(A, r.expiredClaims().get(0).holder());
        assertEquals("expiry", r.reason());
        assertEquals(ClaimState.AVAILABLE, g.claim(q.get(0)).state());
        assertTrue(g.tick().expiredClaims().isEmpty());
    }

    @Test
    void releaseAllForAndForceRelease() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = gen(host, TestSupport.config(Difficulty.NORMAL, 5).withOneActivePerPlayer(false));
        List<String> q = ids(g);
        g.tryClaim(q.get(0), A);
        g.tryClaim(q.get(1), A);
        g.tryClaim(q.get(2), B);
        List<String> released = g.releaseAllFor(A);
        assertEquals(List.of(q.get(0), q.get(1)).size(), released.size());
        assertTrue(released.containsAll(List.of(q.get(0), q.get(1))));
        assertEquals(ClaimState.AVAILABLE, g.claim(q.get(0)).state());
        assertEquals(Optional.of(B), g.forceRelease(q.get(2)));
        assertEquals(ClaimState.AVAILABLE, g.claim(q.get(2)).state());
        assertEquals(Optional.empty(), g.forceRelease(q.get(2)));
        assertTrue(g.releaseAllFor(C).isEmpty());
    }

    @Test
    void claimsAndStatePersistAcrossRestart() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = gen(host, TestSupport.config(Difficulty.NORMAL, 5));
        String q = ids(g).get(0);
        g.tryClaim(q, A);
        g.stop();
        QuestGeneratorV2 g2 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g2.start(Map.of(q, A));
        assertEquals(ClaimState.CLAIMED, g2.claim(q).state());
        assertEquals(ClaimResult.CLAIMED_BY_OTHER, g2.tryClaim(q, B));
        assertEquals(g.servedQuests(), g2.servedQuests());
    }

    @Test
    void completedQuestLeavesAtNextRotation() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = gen(host, TestSupport.config(Difficulty.NORMAL, 5));
        String q = ids(g).get(0);
        g.tryClaim(q, A);
        g.onComplete(q, A);
        assertTrue(g.servedQuests().containsKey(q), "stays until the next rotation");
        host.now += 12 * HOUR;
        RotationResult r = g.tick();
        assertTrue(r.removed().contains(q));
        assertFalse(g.servedQuests().containsKey(q));
    }
}
