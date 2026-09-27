package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.TriState;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.4 — host answers (missing ids, TriState NO) are honoured; all-UNKNOWN still yields full sets. */
class AchievabilityTest {
    private static void forEachObjective(FakeHost host, Difficulty d, int seeds, java.util.function.BiConsumer<String, String> f) {
        for (long seed = 1; seed <= seeds; seed++) {
            var r = TestSupport.generate(host, TestSupport.config(d, 10), TestSupport.unlockedProgression(host), seed, 10);
            for (QuestDraft q : r.drafts()) {
                for (JsonElement e : q.json.getAsJsonArray("objectives")) {
                    JsonObject o = e.getAsJsonObject();
                    f.accept(TestSupport.shortType(o), TestSupport.objectiveTarget(o));
                }
            }
        }
    }

    @Test
    void missingIdsNeverAppear() {
        FakeHost host = new FakeHost();
        Set<String> gone = Set.of("minecraft:iron_ore", "minecraft:raw_iron", "minecraft:iron_ingot", "minecraft:zombie",
            "minecraft:oak_log", "minecraft:wheat", "minecraft:cow", "minecraft:the_nether", "minecraft:emerald",
            "minecraft:diamond", "minecraft:bread");
        host.content.missing.addAll(gone);
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 120; seed++) {
                var r = TestSupport.generate(host, TestSupport.config(d, 10), TestSupport.unlockedProgression(host), seed, 10);
                for (QuestDraft q : r.drafts()) {
                    String text = Json.compact(q.json);
                    for (String id : gone) {
                        assertFalse(text.contains("\"" + id + "\""), id + " appeared: " + text);
                    }
                    assertFalse(text.contains("minecraft:the_nether"), "missing dimension used");
                }
            }
        }
    }

    @Test
    void definiteNoOverridesCatalog() {
        FakeHost host = new FakeHost();
        host.content.craftable.put("minecraft:torch", TriState.NO);
        host.content.craftable.put("minecraft:chest", TriState.NO);
        host.content.smeltable.put("minecraft:iron_ingot", TriState.NO);
        host.content.breedable.put("minecraft:cow", TriState.NO);
        host.content.tamable.put("minecraft:wolf", TriState.NO);
        host.content.consumable.put("minecraft:bread", TriState.NO);
        for (Difficulty d : Difficulty.values()) {
            forEachObjective(host, d, 150, (type, target) -> {
                assertFalse(type.equals("craft_item") && (target.equals("minecraft:torch") || target.equals("minecraft:chest")));
                assertFalse(type.equals("smelt_item") && target.equals("minecraft:iron_ingot"));
                assertFalse(type.equals("breed_animal") && target.equals("minecraft:cow"));
                assertFalse(type.equals("tame_animal") && target.equals("minecraft:wolf"));
                assertFalse(type.equals("consume_item") && target.equals("minecraft:bread"));
            });
        }
    }

    @Test
    void tameOnlyTamableAnimals() {
        FakeHost host = new FakeHost();
        Set<String> allowed = Set.of("minecraft:wolf", "minecraft:cat", "minecraft:parrot");
        for (Difficulty d : Difficulty.values()) {
            forEachObjective(host, d, 200, (type, target) -> {
                if (type.equals("tame_animal")) {
                    assertTrue(allowed.contains(target), "tame target " + target);
                }
                assertFalse(type.equals("breed_animal") && target.equals("minecraft:villager"));
            });
        }
    }

    @Test
    void allUnknownStillFullSets() {
        FakeHost host = new FakeHost(); // every TriState UNKNOWN, englishName null, stack size -1
        for (Difficulty d : Difficulty.values()) {
            for (int n : new int[]{5, 10, 20}) {
                for (long seed = 1; seed <= 40; seed++) {
                    var r = TestSupport.generate(host, TestSupport.config(d, n), TestSupport.unlockedProgression(host), seed, n);
                    assertEquals(n, r.drafts().size(), d + " n=" + n + " seed=" + seed + " relax=" + r.relaxNotes());
                }
            }
        }
    }

    @Test
    void objectiveTypeMissingFromHostIsNeverUsed() {
        FakeHost host = new FakeHost();
        host.caps.objectiveTypes.remove("justquests:tame_animal");
        host.caps.objectiveTypes.remove("justquests:consume_item");
        for (Difficulty d : Difficulty.values()) {
            forEachObjective(host, d, 150, (type, target) -> {
                assertFalse(type.equals("tame_animal") || type.equals("consume_item"), "type " + type);
            });
        }
    }

    @Test
    void alternativeIdIsUsedWhenPrimaryIsMissing() {
        FakeHost host = new FakeHost();
        host.content.missing.add("minecraft:iron_chain");
        host.content.missing.add("minecraft:short_grass");
        boolean[] seen = new boolean[2];
        for (Difficulty d : Difficulty.values()) {
            forEachObjective(host, d, 400, (type, target) -> {
                assertFalse(target.equals("minecraft:iron_chain") || target.equals("minecraft:short_grass"));
                if (target.equals("minecraft:chain")) {
                    seen[0] = true;
                }
                if (target.equals("minecraft:grass")) {
                    seen[1] = true;
                }
            });
        }
        assertTrue(seen[0] || seen[1], "alternative ids never chosen");
    }
}
