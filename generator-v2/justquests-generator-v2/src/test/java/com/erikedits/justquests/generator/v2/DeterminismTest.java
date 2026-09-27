package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** §16.2 — identical inputs produce byte-identical served JSON across instances. */
class DeterminismTest {
    private static String served(QuestGeneratorV2 gen) {
        JsonObject all = new JsonObject();
        gen.servedQuests().forEach(all::add);
        return Json.compact(all);
    }

    private static QuestGeneratorV2 started(long worldSeed, Difficulty d, boolean mods) {
        FakeHost host = mods ? new FakeHost().withMods("farmersdelight", "create") : new FakeHost();
        host.world.seed = worldSeed;
        host.world.shares.put("minecraft:story/enter_the_nether", 1.0);
        QuestGeneratorV2 gen = new QuestGeneratorV2(host, TestSupport.config(d, 10));
        gen.start(Map.of());
        return gen;
    }

    @Test
    void sameInputsSameOutput() {
        for (Difficulty d : Difficulty.values()) {
            for (boolean mods : new boolean[]{false, true}) {
                QuestGeneratorV2 a = started(42L, d, mods);
                QuestGeneratorV2 b = started(42L, d, mods);
                assertEquals(served(a), served(b), "served set differs for " + d + " mods=" + mods);
                a.reroll();
                b.reroll();
                assertEquals(served(a), served(b), "reroll differs for " + d);
                JsonArray pa = new JsonArray();
                a.preview(8).forEach(pa::add);
                JsonArray pb = new JsonArray();
                b.preview(8).forEach(pb::add);
                assertEquals(Json.compact(pa), Json.compact(pb), "preview differs for " + d);
            }
        }
    }

    @Test
    void differentSeedDifferentSet() {
        assertNotEquals(served(started(1L, Difficulty.NORMAL, false)), served(started(2L, Difficulty.NORMAL, false)));
    }

    @Test
    void generationRunIsDeterministic() {
        FakeHost h1 = new FakeHost();
        FakeHost h2 = new FakeHost();
        for (long seed = 1; seed <= 50; seed++) {
            var r1 = TestSupport.generate(h1, TestSupport.config(Difficulty.HARD, 20), TestSupport.unlockedProgression(h1), seed, 20);
            var r2 = TestSupport.generate(h2, TestSupport.config(Difficulty.HARD, 20), TestSupport.unlockedProgression(h2), seed, 20);
            JsonArray x = new JsonArray();
            r1.drafts().forEach(dr -> x.add(dr.json));
            JsonArray y = new JsonArray();
            r2.drafts().forEach(dr -> y.add(dr.json));
            assertEquals(Json.compact(x), Json.compact(y));
        }
    }
}
