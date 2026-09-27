package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.catalog.RewardDefs;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every bundled reward item, effect and loot table is actually handed out somewhere. */
class RewardCoverageTest {
    @Test
    void everyRewardIsUsed() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create", "mekanism", "twilightforest", "botania");
        host.content.dimensions.add("twilightforest:twilight_forest");
        Progression p = TestSupport.unlockedProgression(host);
        Set<String> seen = new TreeSet<>();
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 150; seed++) {
                for (QuestDraft q : TestSupport.generate(host, TestSupport.config(d, 20).withModdedShare(0.5), p,
                    seed * 13L, 20).drafts()) {
                    for (JsonElement e : q.json.getAsJsonArray("rewards")) {
                        JsonObject r = e.getAsJsonObject();
                        for (String f : new String[]{"item", "effect", "loot_table"}) {
                            if (r.has(f)) {
                                seen.add(r.get(f).getAsString());
                            }
                        }
                    }
                }
            }
        }
        List<String> missing = new ArrayList<>();
        var catalog = TestSupport.catalog();
        for (RewardDefs.Item it : catalog.items) {
            if (!seen.contains(it.id()) && it.alts().stream().noneMatch(seen::contains)) {
                missing.add("item " + it.id());
            }
        }
        for (RewardDefs.Effect ef : catalog.effects) {
            if (!seen.contains(ef.id())) {
                missing.add("effect " + ef.id());
            }
        }
        for (RewardDefs.Loot l : catalog.loot) {
            if (!seen.contains(l.id())) {
                missing.add("loot " + l.id());
            }
        }
        assertTrue(missing.isEmpty(), missing.size() + " rewards never handed out: " + missing);
    }
}
