package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.catalog.Catalog;
import com.erikedits.justquests.generator.v2.internal.catalog.CatalogLoader;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.14 — profiles activate only with their mod; modded share and one quest per active mod. */
class ModProfilesTest {
    /** Prettified Mekanism ids ("Ingot Osmium", "Block Salt", "Fluorite Gem") instead of English names. */
    private static final java.util.regex.Pattern INVERTED_NAME = java.util.regex.Pattern.compile(
        "(?i)\\b(ingot (osmium|tin|lead|uranium|steel)|block (osmium|fluorite|salt)|fluorite gems?)\\b");

    @Test
    void inactiveWithoutMod() {
        FakeHost host = new FakeHost();
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 100; seed++) {
                for (QuestDraft q : TestSupport.generate(host, TestSupport.config(d, 10), TestSupport.unlockedProgression(host), seed, 10).drafts()) {
                    String t = Json.compact(q.json);
                    assertFalse(t.contains("farmersdelight:") || t.contains("create:") || t.contains("mekanism:")
                        || t.contains("twilightforest:") || t.contains("botania:"), t);
                    assertEquals(Set.of("vanilla"), q.profiles());
                }
            }
        }
        assertEquals(0, host.log.count("WARN"), "inactive profiles must be silent: " + host.log.lines);
    }

    @Test
    void activeWithModAndShareRespected() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create");
        Progression p = TestSupport.unlockedProgression(host);
        for (Difficulty d : Difficulty.values()) {
            for (int n : new int[]{5, 10, 20}) {
                for (long seed = 1; seed <= 100; seed++) {
                    Generation.Result r = TestSupport.generate(host, TestSupport.config(d, n), p, seed, n);
                    long modded = r.drafts().stream().filter(QuestDraft::modded).count();
                    boolean fd = r.drafts().stream().anyMatch(q -> q.profiles().contains("farmersdelight"));
                    boolean create = r.drafts().stream().anyMatch(q -> q.profiles().contains("create"));
                    if (!r.relaxations().containsKey("modded_share")) {
                        assertTrue(fd && create, d + " n=" + n + " seed=" + seed + ": one quest per active mod");
                        long target = Math.round(n * 0.35);
                        assertTrue(modded >= target && modded <= target + 1, d + " n=" + n + " modded " + modded);
                    }
                }
            }
        }
    }

    @Test
    void onlyLoadedModIsUsed() {
        FakeHost host = new FakeHost().withMods("farmersdelight");
        for (long seed = 1; seed <= 60; seed++) {
            for (QuestDraft q : TestSupport.generate(host, TestSupport.config(Difficulty.NORMAL, 10), TestSupport.unlockedProgression(host), seed, 10).drafts()) {
                assertFalse(Json.compact(q.json).contains("create:"));
            }
        }
    }

    @Test
    void disabledProfileAndZeroShare() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create");
        GeneratorConfig cfg = TestSupport.config(Difficulty.NORMAL, 10).toBuilder().disabledProfiles(Set.of("Create")).build();
        GeneratorConfig zero = TestSupport.config(Difficulty.NORMAL, 10).withModdedShare(0.0);
        for (long seed = 1; seed <= 60; seed++) {
            for (QuestDraft q : TestSupport.generate(host, cfg, TestSupport.unlockedProgression(host), seed, 10).drafts()) {
                assertFalse(q.profiles().contains("create"));
            }
            for (QuestDraft q : TestSupport.generate(host, zero, TestSupport.unlockedProgression(host), seed, 10).drafts()) {
                assertFalse(q.modded(), "moddedShare 0 means vanilla only");
            }
        }
    }

    @Test
    void worldProfileWithFiltersIsDataOnly() {
        FakeHost host = new FakeHost().withMods("examplemod");
        host.store.files.put("generator_v2/profiles/examplemod.json", """
            {"format": 1, "id": "examplemod", "requiresMod": ["examplemod"], "loaders": ["fabric"],
             "entries": [{"key": "ruby", "family": "ex_gems", "tier": 1, "tool": "iron",
               "targets": [{"type": "mine_block", "id": "examplemod:ruby_ore", "effort": 0.8, "min": 2, "max": 16}]}]}
            """);
        Catalog neo = new CatalogLoader(host.log, host.store).load();
        assertTrue(neo.profile("examplemod") != null, "world profile loaded");
        Generation.Result r = Generation.run(neo, host, TestSupport.config(Difficulty.NORMAL, 20),
            TestSupport.unlockedProgression(host), Map.of(), Set.of(), List.of(), 20, 1L, 5L, 0);
        assertTrue(r.pool().candidates().stream().noneMatch(c -> c.profile().equals("examplemod")), "loader filter");
        host.content.loader = "fabric";
        Generation.Result r2 = Generation.run(neo, host, TestSupport.config(Difficulty.NORMAL, 20),
            TestSupport.unlockedProgression(host), Map.of(), Set.of(), List.of(), 20, 1L, 5L, 0);
        assertTrue(r2.pool().candidates().stream().anyMatch(c -> c.profile().equals("examplemod")));
        assertTrue(r2.drafts().stream().anyMatch(q -> q.profiles().contains("examplemod")), "at least one quest per active mod");
    }

    @Test
    void worldBalanceOverrideIsMerged() {
        FakeHost host = new FakeHost();
        host.store.files.put("generator_v2/balance.json", "{\"difficulties\": {\"EASY\": {\"targetMinutes\": [2, 3]}}}");
        Catalog c = new CatalogLoader(host.log, host.store).load();
        assertEquals(2.0, c.balance.level(Difficulty.EASY).minMinutes);
        assertEquals(3.0, c.balance.level(Difficulty.EASY).maxMinutes);
        assertEquals(8.0, c.balance.level(Difficulty.NORMAL).minMinutes, "untouched values keep the bundled defaults");
    }

    @Test
    void allThreeModsTogetherGetAQuestEachAndReadableNames() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create", "mekanism");
        Progression p = TestSupport.unlockedProgression(host);
        int mekQuests = 0;
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 60; seed++) {
                Generation.Result r = TestSupport.generate(host, TestSupport.config(d, 12), p, seed, 12);
                assertEquals(12, r.drafts().size());
                if (!r.relaxations().containsKey("modded_share")) {
                    for (String mod : List.of("farmersdelight", "create", "mekanism")) {
                        assertTrue(r.drafts().stream().anyMatch(q -> q.profiles().contains(mod)),
                            d + " seed " + seed + ": no " + mod + " quest");
                    }
                }
                for (QuestDraft q : r.drafts()) {
                    String t = Json.compact(q.json);
                    if (t.contains("mekanism:")) {
                        mekQuests++;
                        // a dedicated server has no mod lang files: inverted ids must not leak as names
                        assertFalse(INVERTED_NAME.matcher(t).find(), "unreadable Mekanism name in " + t);
                    }
                }
            }
        }
        assertTrue(mekQuests > 60, "Mekanism quests generated: " + mekQuests);
    }

    @Test
    void mekanismMachinesOnlyOnHard() {
        FakeHost host = new FakeHost().withMods("mekanism");
        Progression p = TestSupport.unlockedProgression(host);
        GeneratorConfig easy = TestSupport.config(Difficulty.EASY, 10).withModdedShare(1.0);
        for (long seed = 1; seed <= 80; seed++) {
            for (QuestDraft q : TestSupport.generate(host, easy, p, seed, 10).drafts()) {
                String t = Json.compact(q.json);
                for (String machine : List.of("steel_casing", "enrichment_chamber", "energized_smelter", "configurator")) {
                    assertFalse(t.contains("mekanism:" + machine), "EASY set uses " + machine + ": " + t);
                }
            }
        }
    }

    @Test
    void twilightForestWaitsForItsDimension() {
        FakeHost host = new FakeHost().withMods("twilightforest");
        host.content.dimensions.add("twilightforest:twilight_forest");
        GeneratorConfig cfg = TestSupport.config(Difficulty.NORMAL, 10).withModdedShare(1.0);
        // fresh world: nobody has been there and it is day 0, so nothing from the dimension
        Progression fresh = TestSupport.freshProgression();
        for (long seed = 1; seed <= 40; seed++) {
            for (QuestDraft q : TestSupport.generate(host, cfg, fresh, seed, 10).drafts()) {
                String objectives = q.json.getAsJsonArray("objectives").toString();
                assertFalse(objectives.contains("twilightforest:"), Json.compact(q.json));
            }
        }
        // day 100: unlocked by day; Twilight Forest quests appear, with readable names
        Progression unlocked = TestSupport.unlockedProgression(host);
        assertTrue(unlocked.dimensionUnlocked("twilightforest:twilight_forest"));
        int tf = 0;
        for (long seed = 1; seed <= 40; seed++) {
            for (QuestDraft q : TestSupport.generate(host, cfg, unlocked, seed, 10).drafts()) {
                String t = Json.compact(q.json);
                if (t.contains("twilightforest:")) {
                    tf++;
                    assertFalse(t.contains("Dark Log") || t.contains("Cooked Venison") || t.contains("Twilight Forest}"), t);
                }
            }
        }
        assertTrue(tf > 40, "Twilight Forest quests: " + tf);
        // the dimension itself missing (mod data pack off): nothing, silently
        FakeHost noDim = new FakeHost().withMods("twilightforest");
        Progression p2 = TestSupport.unlockedProgression(noDim);
        for (long seed = 1; seed <= 20; seed++) {
            for (QuestDraft q : TestSupport.generate(noDim, cfg, p2, seed, 10).drafts()) {
                String objectives = q.json.getAsJsonArray("objectives").toString();
                assertFalse(objectives.contains("twilightforest:"), Json.compact(q.json));
            }
        }
    }

    @Test
    void twilightBossesOnlyOnHard() {
        FakeHost host = new FakeHost().withMods("twilightforest");
        host.content.dimensions.add("twilightforest:twilight_forest");
        Progression p = TestSupport.unlockedProgression(host);
        for (Difficulty d : new Difficulty[]{Difficulty.EASY, Difficulty.NORMAL}) {
            GeneratorConfig cfg = TestSupport.config(d, 10).withModdedShare(1.0);
            for (long seed = 1; seed <= 60; seed++) {
                for (QuestDraft q : TestSupport.generate(host, cfg, p, seed, 10).drafts()) {
                    String t = Json.compact(q.json);
                    assertFalse(t.contains("twilightforest:naga\"") || t.contains("twilightforest:lich\"")
                        || t.contains("naga_scale"), d + ": " + t);
                }
            }
        }
    }

    @Test
    void botaniaStartsInTheMeadowAndKeepsPureDaisyWorkOffEasy() {
        FakeHost host = new FakeHost().withMods("botania");
        Progression p = TestSupport.unlockedProgression(host);
        int botania = 0;
        for (Difficulty d : Difficulty.values()) {
            GeneratorConfig cfg = TestSupport.config(d, 10).withModdedShare(1.0);
            for (long seed = 1; seed <= 40; seed++) {
                for (QuestDraft q : TestSupport.generate(host, cfg, p, seed, 10).drafts()) {
                    String objectives = q.json.getAsJsonArray("objectives").toString();
                    if (objectives.contains("botania:")) {
                        botania++;
                    }
                    if (d == Difficulty.EASY) {
                        assertFalse(objectives.contains("livingwood") || objectives.contains("twig_wand")
                            || objectives.contains("mana_pool"), "Pure Daisy work on Easy: " + objectives);
                    }
                }
            }
        }
        assertTrue(botania > 60, "Botania quests: " + botania);
    }

    @Test
    void manyModsSmallSetsRotateThePerModGuarantee() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create", "mekanism", "twilightforest", "botania");
        host.content.dimensions.add("twilightforest:twilight_forest");
        Progression p = TestSupport.unlockedProgression(host);
        List<String> mods = List.of("farmersdelight", "create", "mekanism", "twilightforest", "botania");
        // N = 5 with five mods: at most three modded quests (half the set, rounded up), and over many
        // cycles every mod gets its turn
        java.util.Map<String, Integer> seen = new java.util.TreeMap<>();
        for (long seed = 1; seed <= 60; seed++) {
            Generation.Result r = TestSupport.generate(host, TestSupport.config(Difficulty.HARD, 5), p, seed, 5);
            long modded = r.drafts().stream().filter(QuestDraft::modded).count();
            assertTrue(modded <= 3, "seed " + seed + ": " + modded + " modded of 5");
            assertTrue(r.relaxations().containsKey("per_mod"), r.relaxations().toString());
            for (QuestDraft q : r.drafts()) {
                for (String m : q.profiles()) {
                    seen.merge(m, 1, Integer::sum);
                }
            }
        }
        for (String m : mods) {
            assertTrue(seen.getOrDefault(m, 0) >= 5, m + " rarely appears: " + seen);
        }
        // N = 10: the guarantee holds literally again (every active mod at least once)
        for (long seed = 1; seed <= 40; seed++) {
            Generation.Result r = TestSupport.generate(host, TestSupport.config(Difficulty.HARD, 10), p, seed, 10);
            assertFalse(r.relaxations().containsKey("per_mod"), r.relaxations().toString());
            if (!r.relaxations().containsKey("modded_share")) {
                for (String m : mods) {
                    assertTrue(r.drafts().stream().anyMatch(q -> q.profiles().contains(m)), "seed " + seed + " misses " + m);
                }
            }
        }
    }
}
