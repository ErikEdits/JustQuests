package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.catalog.ThemeDef;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every bundled theme can actually be built: no theme is dead data. Over 18000 quests the rarest theme
 * appears a few times; {@code -Dprobe=true} prints how often each one was used.
 */
class ThemeCoverageTest {
    @Test
    void everyThemeAppears() {
        Map<String, Integer> seen = new TreeMap<>();
        // two packs, as a server would run them: each profile keeps a share like a real pack
        for (String[] mods : new String[][]{
            {"farmersdelight", "create", "mekanism", "twilightforest", "botania"},
            {"ae2", "immersiveengineering", "biomesoplenty", "alexsmobs", "tconstruct", "ars_nouveau"}}) {
            FakeHost host = new FakeHost().withMods(mods);
            host.content.dimensions.add("twilightforest:twilight_forest");
            Progression p = TestSupport.unlockedProgression(host);
            for (Difficulty d : Difficulty.values()) {
                for (long seed = 1; seed <= 300; seed++) {
                    for (QuestDraft q : TestSupport.generate(host, TestSupport.config(d, 20).withModdedShare(0.5), p,
                        seed * 7L, 20).drafts()) {
                        if (q.themeKey != null) {
                            seen.merge(q.themeKey, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        List<String> missing = new ArrayList<>();
        for (ThemeDef t : TestSupport.catalog().themes) {
            if (!seen.containsKey(t.key())) {
                missing.add(t.profile() + ":" + t.key());
            }
        }
        if (Boolean.getBoolean("probe")) {
            seen.forEach((k, v) -> System.out.println("theme " + k + " " + v));
        }
        assertTrue(missing.isEmpty(), missing.size() + " of " + TestSupport.catalog().themes.size()
            + " themes never generated: " + missing);
    }
}
