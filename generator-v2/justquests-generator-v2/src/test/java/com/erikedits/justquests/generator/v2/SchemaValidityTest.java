package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.gen.SchemaCheck;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.3 — every generated quest passes a strict §6 validator. */
class SchemaValidityTest {
    private static void checkAll(FakeHost host, int seeds) {
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= seeds; seed++) {
                var r = TestSupport.generate(host, TestSupport.config(d, 10), TestSupport.unlockedProgression(host), seed, 10);
                for (QuestDraft q : r.drafts()) {
                    List<String> p = StrictQuestCheck.check(q.json, host.caps);
                    assertTrue(p.isEmpty(), d + " seed " + seed + ": " + p + " in " + Json.compact(q.json));
                    assertTrue(SchemaCheck.check(q.json, host.caps).isEmpty());
                    String text = Json.compact(q.json);
                    assertFalse(text.contains("justquests:command"), "command reward emitted");
                    assertFalse(text.contains("gain_advancement") || text.contains("reach_level")
                        || text.contains("reach_location"), "forbidden objective emitted");
                }
            }
        }
    }

    @Test
    void vanillaSetsAreValid() {
        checkAll(new FakeHost(), 150);
    }

    @Test
    void moddedSetsAreValid() {
        checkAll(new FakeHost().withMods("farmersdelight", "create"), 150);
    }

    @Test
    void noTagsWhenHostDisallowsThem() {
        FakeHost host = new FakeHost();
        host.caps.tagTypes.clear();
        for (long seed = 1; seed <= 200; seed++) {
            var r = TestSupport.generate(host, TestSupport.config(Difficulty.NORMAL, 10), TestSupport.unlockedProgression(host), seed, 10);
            for (QuestDraft q : r.drafts()) {
                assertFalse(Json.compact(q.json).contains("\"#"), "tag emitted: " + Json.compact(q.json));
            }
        }
    }

    @Test
    void servedQuestsThroughFacadeAreValid() {
        FakeHost host = new FakeHost().withMods("farmersdelight");
        QuestGeneratorV2 gen = new QuestGeneratorV2(host, TestSupport.config(Difficulty.HARD, 20));
        gen.start(Map.of());
        assertFalse(gen.servedQuests().isEmpty());
        gen.servedQuests().forEach((id, q) -> {
            assertTrue(id.matches("justquests:gen/\\d+_\\d+"), id);
            assertTrue(StrictQuestCheck.check(q, host.caps).isEmpty(), id + " " + Json.compact(q));
        });
        assertTrue(host.validator.calls >= gen.servedQuests().size(), "host validator must see every served quest");
    }
}
