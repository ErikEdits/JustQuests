package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.util.Medians;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.11 — effort and rewards grow with difficulty; tier caps hold. */
class DifficultyScalingTest {
    @Test
    void monotonicAndCapped() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create");
        Progression p = TestSupport.unlockedProgression(host);
        Map<Difficulty, Double> minutes = new EnumMap<>(Difficulty.class);
        Map<Difficulty, Double> value = new EnumMap<>(Difficulty.class);
        Map<Difficulty, Double> objectives = new EnumMap<>(Difficulty.class);
        for (Difficulty d : Difficulty.values()) {
            List<Double> m = new ArrayList<>();
            List<Double> v = new ArrayList<>();
            double objs = 0;
            int maxTier = TestSupport.catalog().balance.level(d).maxTier;
            for (long seed = 1; seed <= 300; seed++) {
                for (QuestDraft q : TestSupport.generate(host, TestSupport.config(d, 10), p, seed, 10).drafts()) {
                    m.add(q.estMinutes);
                    v.add(q.rewardValue);
                    objs += q.objectives.size();
                    assertTrue(q.tier() <= maxTier, d + " tier " + q.tier());
                    if (d == Difficulty.EASY) {
                        assertEquals(1, q.objectives.size(), "easy quests have one objective");
                        for (var o : q.objectives) {
                            assertEquals("minecraft:overworld", o.c().dimension() == null ? "minecraft:overworld" : o.c().dimension(),
                                "easy quests stay in the overworld");
                        }
                    }
                }
            }
            minutes.put(d, Medians.median(m));
            value.put(d, Medians.median(v));
            objectives.put(d, objs / m.size());
        }
        assertTrue(minutes.get(Difficulty.EASY) < minutes.get(Difficulty.NORMAL), "minutes " + minutes);
        assertTrue(minutes.get(Difficulty.NORMAL) < minutes.get(Difficulty.HARD), "minutes " + minutes);
        assertTrue(value.get(Difficulty.EASY) < value.get(Difficulty.NORMAL), "value " + value);
        assertTrue(value.get(Difficulty.NORMAL) < value.get(Difficulty.HARD), "value " + value);
        assertTrue(objectives.get(Difficulty.NORMAL) > 1.05 && objectives.get(Difficulty.HARD) > objectives.get(Difficulty.NORMAL),
            "objective counts " + objectives);
        var lv = TestSupport.catalog().balance;
        for (Difficulty d : Difficulty.values()) {
            double med = minutes.get(d);
            assertTrue(med >= lv.level(d).minMinutes * 0.75 && med <= lv.level(d).maxMinutes, d + " median " + med);
        }
    }
}
