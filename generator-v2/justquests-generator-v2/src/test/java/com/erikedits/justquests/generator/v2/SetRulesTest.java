package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** §16.5 — set rules across 1000 seeds × each difficulty × N ∈ {1,5,10,20}. */
class SetRulesTest {
    private int relaxedRuns;
    private int runs;

    private void checkSet(Generation.Result r, Difficulty d, int n, long seed) {
        runs++;
        if (!r.relaxations().isEmpty()) {
            relaxedRuns++;
        }
        String ctx = d + " n=" + n + " seed=" + seed + " relaxed=" + r.relaxNotes();
        Set<String> families = new HashSet<>();
        Set<String> targets = new HashSet<>();
        Set<String> titles = new HashSet<>();
        Set<String> types = new HashSet<>();
        Map<String, Integer> typeCount = new HashMap<>();
        boolean quick = false;
        var level = TestSupport.catalog().balance.level(d);
        double quickLimit = level.quickLimit(TestSupport.catalog().balance.quickFraction);
        for (QuestDraft q : r.drafts()) {
            for (String f : q.families()) {
                if (!families.add(f) && !r.relaxations().containsKey("family")) {
                    // one quest may use a family twice (theme/combo), but never two quests
                    boolean inThisQuestOnly = q.objectives.stream().filter(o -> o.c().family().equals(f)).count() > 1;
                    if (!inThisQuestOnly) {
                        fail("family " + f + " used twice: " + ctx);
                    }
                }
            }
            for (String t : q.targets()) {
                assertTrue(targets.add(t), "duplicate target " + t + ": " + ctx);
            }
            assertTrue(titles.add(q.title.toLowerCase(Locale.ROOT)), "duplicate title " + q.title + ": " + ctx);
            for (String t : q.types()) {
                types.add(t);
                typeCount.merge(t, 1, Integer::sum);
            }
            quick |= q.estMinutes <= quickLimit + 1e-9;
        }
        assertEquals(n, r.drafts().size(), "set not filled: " + ctx);
        if (!r.relaxations().containsKey("distinct_types")) {
            assertTrue(types.size() >= Math.min(n, 3), "only " + types + ": " + ctx);
        }
        if (!r.relaxations().containsKey("type_share")) {
            int max = Math.max(1, (int) Math.floor(0.4 * n + 1e-9));
            typeCount.forEach((t, c) -> assertTrue(c <= max, t + " in " + c + " quests (max " + max + "): " + ctx));
        }
        if (!r.relaxations().containsKey("quick")) {
            assertTrue(quick, "no quick quest: " + ctx);
        }
    }

    @Test
    void thousandSeedsVanilla() {
        FakeHost host = new FakeHost();
        Progression p = TestSupport.unlockedProgression(host);
        for (Difficulty d : Difficulty.values()) {
            for (int n : new int[]{1, 5, 10, 20}) {
                for (long seed = 1; seed <= 1000; seed++) {
                    try {
                        checkSet(TestSupport.generate(host, TestSupport.config(d, n), p, seed * 7919L, n), d, n, seed);
                    } catch (RuntimeException e) {
                        fail("generation threw for " + d + " n=" + n + " seed=" + seed, e);
                    }
                }
            }
        }
        assertTrue(relaxedRuns <= runs / 100, "too many relaxed runs: " + relaxedRuns + "/" + runs);
    }

    @Test
    void modProfilesActive() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create");
        Progression p = TestSupport.unlockedProgression(host);
        for (Difficulty d : Difficulty.values()) {
            for (int n : new int[]{1, 5, 10, 20}) {
                for (long seed = 1; seed <= 200; seed++) {
                    checkSet(TestSupport.generate(host, TestSupport.config(d, n), p, seed, n), d, n, seed);
                }
            }
        }
        assertTrue(relaxedRuns <= runs / 10, "too many relaxed runs: " + relaxedRuns + "/" + runs);
    }

    @Test
    void tinyPoolRelaxesButNeverThrows() {
        FakeHost host = new FakeHost();
        // leave only a handful of ids alive
        for (var e : TestSupport.catalog().allEntries()) {
            for (var t : e.targets()) {
                if (t.id() != null && !e.family().equals("wood") && !e.family().equals("stone")) {
                    host.content.missing.add(t.id());
                }
            }
        }
        host.content.itemTags.clear();
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 50; seed++) {
                Generation.Result r = TestSupport.generate(host, TestSupport.config(d, 20), TestSupport.freshProgression(), seed, 20);
                assertTrue(r.drafts().size() <= 20);
                assertTrue(r.relaxations().containsKey("family"), "two families cannot fill 20 slots without relaxing");
                Set<String> targets = new HashSet<>();
                for (QuestDraft q : r.drafts()) {
                    for (String t : q.targets()) {
                        assertTrue(targets.add(t), "duplicate targets are never relaxed");
                    }
                }
            }
        }
    }
}
