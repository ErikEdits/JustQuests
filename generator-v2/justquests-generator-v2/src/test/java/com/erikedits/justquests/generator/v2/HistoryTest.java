package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.gen.Candidate;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.state.GenState;
import com.erikedits.justquests.generator.v2.internal.state.V1Migration;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.6 — no signature repeats within the 6-day window; allowed again afterwards. */
class HistoryTest {
    private static final long HOUR = 3_600_000L;
    private static final long DAY = 24 * HOUR;

    @Test
    void noRepeatsWithinSixDaysAcrossCycles() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 gen = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        gen.start(Map.of());
        Map<String, Long> lastSeen = new HashMap<>();
        Set<String> seenIds = new HashSet<>();
        int cycles = 0;
        for (int step = 0; step < 28; step++) { // 14 days of 12-hour cycles
            for (Map.Entry<String, JsonObject> e : gen.servedQuests().entrySet()) {
                if (!seenIds.add(e.getKey())) {
                    continue;
                }
                String sig = V1Migration.signatureOf(e.getValue());
                Long prev = lastSeen.put(sig, host.now);
                if (prev != null) {
                    assertTrue(host.now - prev >= 6 * DAY, "signature " + sig + " repeated after "
                        + (host.now - prev) / HOUR + " h");
                }
            }
            host.now += 12 * HOUR;
            if (gen.tick().changed()) {
                cycles++;
            }
        }
        assertEquals(28, cycles);
        GenState state = GenState.fromJson(Json.parseObject(host.store.files.get(GenState.FILE)));
        for (long t : state.history.values()) {
            assertTrue(host.now - t < 6 * DAY + 12 * HOUR, "history not pruned");
        }
    }

    @Test
    void historyBlocksSignaturesAndPruningReleasesThem() {
        FakeHost host = new FakeHost();
        Progression p = TestSupport.unlockedProgression(host);
        var cfg = TestSupport.config(Difficulty.NORMAL, 5);
        Generation.Result first = TestSupport.generate(host, cfg, p, 99L, 5);
        Set<String> history = new HashSet<>();
        for (Candidate c : first.pool().candidates()) {
            history.add(c.signaturePart());
        }
        // keep exactly the signatures of the first run allowed... except the first quest's
        String blocked = first.drafts().get(0).signature();
        Generation.Result again = Generation.run(TestSupport.catalog(), host, cfg, p, Map.of(), Set.of(blocked),
            List.of(), 5, 1L, 99L, 0);
        for (QuestDraft d : again.drafts()) {
            assertFalse(d.signature().equals(blocked), "blocked signature reused");
        }
        GenState s = new GenState();
        s.history.put(blocked, 0L);
        s.history.put("fresh", 5 * DAY);
        s.pruneHistory(6 * DAY, 6 * DAY);
        assertFalse(s.history.containsKey(blocked), "6-day-old entry must be pruned");
        assertTrue(s.history.containsKey("fresh"));
        // with an empty history the signature may appear again
        boolean reappeared = false;
        for (long seed = 99; seed < 99 + 5; seed++) {
            Generation.Result r = TestSupport.generate(host, cfg, p, seed, 5);
            for (QuestDraft d : r.drafts()) {
                reappeared |= d.signature().equals(blocked);
            }
        }
        assertTrue(reappeared, "signature should be allowed again after pruning");
        assertTrue(history.contains(first.drafts().get(0).objectives.get(0).c().signaturePart())
            || first.drafts().get(0).objectives.size() > 1);
    }
}
