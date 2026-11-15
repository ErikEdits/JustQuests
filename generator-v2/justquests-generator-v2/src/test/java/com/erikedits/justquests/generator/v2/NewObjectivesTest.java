package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.catalog.Balance;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.gen.QuestJson;
import com.erikedits.justquests.generator.v2.internal.gen.SchemaCheck;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 0.6.0: the new objective types are generated on a host that has them, and milestones refine the progression. */
class NewObjectivesTest {
    private static final List<String> NEW = List.of("fish_item", "trade_villager", "brew_potion", "submit_item",
        "find_biome", "find_structure", "cut_item", "cook_item");

    /** A host of 0.6.0 with Farmer's Delight: the new types, biomes, structures and potions. */
    private static FakeHost host060() {
        FakeHost host = new FakeHost().withMods("farmersdelight");
        for (String t : NEW) {
            host.caps.objectiveTypes.add("justquests:" + t);
        }
        host.content.kinds060 = true;
        return host;
    }

    @Test
    void newTypesAreGeneratedWithTheirFields() {
        FakeHost host = host060();
        Progression p = TestSupport.unlockedProgression(host);
        Map<String, Integer> seen = new TreeMap<>();
        boolean profession = false, via = false, ownTag = false, potion = false;
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 150; seed++) {
                Generation.Result r = TestSupport.generate(host, TestSupport.config(d, 20).withModdedShare(0.5), p,
                    seed * 31L, 20);
                for (QuestDraft q : r.drafts()) {
                    for (QuestDraft.Objective o : q.objectives) {
                        seen.merge(o.c().type().shortName(), 1, Integer::sum);
                    }
                    JsonObject json = QuestJson.build(q);
                    List<String> problems = SchemaCheck.check(json, host.caps);
                    assertTrue(problems.isEmpty(), "schema problems " + problems + " in " + json);
                    for (JsonElement e : json.getAsJsonArray("objectives")) {
                        JsonObject o = e.getAsJsonObject();
                        profession |= o.has("profession");
                        via |= o.has("via");
                        ownTag |= o.has("structure") && o.get("structure").getAsString().startsWith("#")
                            || o.has("biome") && o.get("biome").getAsString().startsWith("#");
                        potion |= o.has("potion") && "justquests:brew_potion".equals(o.get("type").getAsString());
                        if (o.has("structure") || o.has("biome")) {
                            assertFalse(o.has("count"), "find goals have no count: " + o);
                        }
                    }
                }
            }
        }
        for (String t : NEW) {
            assertTrue(seen.getOrDefault(t, 0) > 0, t + " never generated: " + seen);
        }
        assertTrue(profession, "no trade with a profession");
        assertTrue(via, "no cook_item with via");
        assertTrue(ownTag, "no biome or structure #tag");
        assertTrue(potion, "no brew_potion with its potion");
    }

    @Test
    void oldHostsGetNoNewTypes() {
        FakeHost host = new FakeHost().withMods("farmersdelight");
        Progression p = TestSupport.unlockedProgression(host);
        for (long seed = 1; seed <= 40; seed++) {
            Generation.Result r = TestSupport.generate(host, TestSupport.config(Difficulty.HARD, 20), p, seed, 20);
            for (QuestDraft q : r.drafts()) {
                for (QuestDraft.Objective o : q.objectives) {
                    assertFalse(NEW.contains(o.c().type().shortName()), "an old host got " + o.c().type());
                }
            }
        }
    }

    @Test
    void milestonesRefineTheProgression() {
        Balance balance = TestSupport.catalog().balance;
        FakeHost host = new FakeHost();
        host.world.day = 1;
        Progression p = new Progression();
        p.update(host.world, balance, TestSupport.catalog().profiles);
        assertEquals(-1, p.milestoneTier(balance));
        assertEquals(0.3, p.typeFactor(balance, "trade_villager"), 1e-9);
        // a quarter of the players online mined diamonds: tier 2 gets its full weight on day 1
        host.world.shares.put("minecraft:story/mine_diamond", 0.5);
        host.world.shares.put("minecraft:adventure/trade", 0.5);
        assertTrue(p.update(host.world, balance, TestSupport.catalog().profiles));
        assertEquals(2, p.milestoneTier(balance));
        assertEquals(1.0, p.typeFactor(balance, "trade_villager"), 1e-9);
        // milestones never go back and survive the state file
        host.world.shares.clear();
        p.update(host.world, balance, TestSupport.catalog().profiles);
        Progression back = Progression.fromJson(p.toJson());
        assertTrue(back.milestone("diamonds") && back.milestone("trading"), "lost: " + back.milestones());
        assertEquals(Set.of("diamonds", "trading"), back.milestones().keySet());
    }
}
