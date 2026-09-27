package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.gen.Candidate;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Targets that pass the pipeline are actually picked. Cheap single crafts (a loom, iron boots) fit only
 * the quick slot or a theme and are rare, so a few may be missing from any finite sample; the test
 * allows 5 % and lists them with {@code -Dprobe=true}.
 */
class TargetCoverageTest {
    private static String key(Candidate c) {
        return c.profile() + ":" + c.entry().key() + ":" + c.type().shortName() + ":" + c.target();
    }

    @Test
    void everyUsableTargetAppears() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create", "mekanism", "twilightforest", "botania");
        host.content.dimensions.add("twilightforest:twilight_forest");
        Progression p = TestSupport.unlockedProgression(host);
        Set<String> usable = new TreeSet<>();
        Map<String, Integer> seen = new TreeMap<>();
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 300; seed++) {
                Generation.Result r = TestSupport.generate(host, TestSupport.config(d, 20).withModdedShare(0.5), p,
                    seed * 17L, 20);
                if (seed == 1) {
                    r.pool().candidates().forEach(c -> usable.add(key(c)));
                }
                for (QuestDraft q : r.drafts()) {
                    for (QuestDraft.Objective o : q.objectives) {
                        seen.merge(key(o.c()), 1, Integer::sum);
                    }
                }
            }
        }
        Set<String> missing = new TreeSet<>(usable);
        missing.removeAll(seen.keySet());
        if (Boolean.getBoolean("probe")) {
            System.out.println("usable " + usable.size() + ", seen " + seen.size() + ", missing " + missing.size());
            missing.forEach(m -> System.out.println("  never: " + m));
        }
        assertTrue(missing.size() <= usable.size() / 20,
            missing.size() + " of " + usable.size() + " usable targets never picked: " + missing);
    }
}
