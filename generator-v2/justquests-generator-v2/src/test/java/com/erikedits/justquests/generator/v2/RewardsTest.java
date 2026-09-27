package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.catalog.RewardDefs;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.12 — rewards: never the target or its family, value within ±20 %, counts ≤ stack size, no commands. */
class RewardsTest {
    @Test
    void rewardRules() {
        Map<String, RewardDefs.Item> byId = new HashMap<>();
        for (RewardDefs.Item it : TestSupport.catalog().items) {
            byId.put(it.id(), it);
            it.alts().forEach(a -> byId.put(a, it));
        }
        FakeHost host = new FakeHost().withMods("farmersdelight", "create");
        host.content.stackSizes.put("minecraft:ender_pearl", 16);
        host.content.stackSizes.put("minecraft:emerald", 64);
        Progression p = TestSupport.unlockedProgression(host);
        int withItem = 0;
        int total = 0;
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 250; seed++) {
                for (QuestDraft q : TestSupport.generate(host, TestSupport.config(d, 10), p, seed, 10).drafts()) {
                    total++;
                    assertTrue(Math.abs(q.totalRewardValue() - q.budget) <= q.budget * 0.2 + 1e-9,
                        "value " + q.totalRewardValue() + " vs budget " + q.budget + " " + q.json);
                    for (QuestDraft.Reward r : q.rewards) {
                        assertFalse(r.type().equals("command"));
                        if (!r.type().equals("give_item")) {
                            continue;
                        }
                        withItem++;
                        assertFalse(q.targets().contains(r.id()), "reward equals target: " + q.json);
                        RewardDefs.Item def = byId.get(r.id());
                        assertTrue(def != null, "reward not from the table: " + r.id());
                        assertFalse(q.families().contains(def.family()), "reward family " + def.family() + " " + q.json);
                        assertTrue(def.tier() <= q.tier() + 1, "reward tier too high: " + q.json);
                        int stack = host.content.stackSizes.getOrDefault(r.id(), def.stack() > 0 ? def.stack() : 64);
                        assertTrue(r.count() >= 1 && r.count() <= stack && r.count() <= def.max(), "count " + q.json);
                        if (d == Difficulty.EASY && q.tier() == 0) {
                            assertFalse(r.id().equals("minecraft:diamond"), "no diamonds on easy tier-0 quests");
                        }
                    }
                }
            }
        }
        assertTrue(withItem > total * 0.8, "most quests should carry an item reward: " + withItem + "/" + total);
    }

    @Test
    void noCommandRewardEvenIfHostSupportsIt() {
        FakeHost host = new FakeHost();
        assertTrue(host.caps.rewardTypes.contains("justquests:command"));
        for (long seed = 1; seed <= 100; seed++) {
            for (QuestDraft q : TestSupport.generate(host, TestSupport.config(Difficulty.HARD, 10),
                TestSupport.unlockedProgression(host), seed, 10).drafts()) {
                assertFalse(q.json.toString().contains("justquests:command"));
            }
        }
    }
}
