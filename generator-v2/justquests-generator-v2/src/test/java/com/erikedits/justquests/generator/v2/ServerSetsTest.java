package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratedQuest;
import com.erikedits.justquests.generator.v2.api.RewardOptions;
import com.erikedits.justquests.generator.v2.api.SetRequest;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.gen.SchemaCheck;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The server plugin's extras: reward choice and scale, and stand-alone sets (personal, weekly, server goal). */
class ServerSetsTest {
    private static QuestGeneratorV2 started(FakeHost host) {
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        return g;
    }

    private static SetRequest request(long seed, int count, Difficulty d, double minutes, Set<String> history) {
        return new SetRequest(seed, count, d, minutes, 1.0, false, true, null, history, Set.of());
    }

    @Test
    void choiceRewardsAreValidAndOffered() {
        FakeHost host = new FakeHost();
        host.caps.rewardTypes.add("justquests:choice");
        int choices = 0;
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 20; seed++) {
                Generation.Result r = Generation.run(TestSupport.catalog(), host, TestSupport.config(d, 6),
                    TestSupport.unlockedProgression(host), Map.of(), Set.of(), List.of(), 6, 1_790_000_000L, seed, 0,
                    new RewardOptions(true, 1.0));
                for (QuestDraft q : r.drafts()) {
                    assertTrue(SchemaCheck.check(q.json, host.caps).isEmpty(), () -> SchemaCheck.check(q.json, host.caps) + " " + q.json);
                    for (JsonObject reward : Json.objects(q.json, "rewards")) {
                        if (!"justquests:choice".equals(Json.str(reward, "type", ""))) continue;
                        choices++;
                        Set<String> options = new HashSet<>();
                        for (JsonElement o : reward.getAsJsonArray("options")) options.add(o.toString());
                        assertEquals(reward.getAsJsonArray("options").size(), options.size(), "options differ");
                    }
                }
            }
        }
        assertTrue(choices > 100, "choices offered: " + choices);
    }

    @Test
    void noChoiceWithoutHostSupport() {
        FakeHost host = new FakeHost();   // no justquests:choice
        Generation.Result r = Generation.run(TestSupport.catalog(), host, TestSupport.config(Difficulty.NORMAL, 8),
            TestSupport.unlockedProgression(host), Map.of(), Set.of(), List.of(), 8, 1_790_000_000L, 5, 0,
            new RewardOptions(true, 1.0));
        for (QuestDraft q : r.drafts()) assertFalse(q.json.toString().contains("justquests:choice"));
    }

    @Test
    void rewardScaleRaisesRewards() {
        FakeHost host = new FakeHost();
        double normal = 0, doubled = 0;
        for (long seed = 1; seed <= 30; seed++) {
            for (QuestDraft q : Generation.run(TestSupport.catalog(), host, TestSupport.config(Difficulty.NORMAL, 6),
                TestSupport.unlockedProgression(host), Map.of(), Set.of(), List.of(), 6, 1L, seed, 0, RewardOptions.STANDARD).drafts()) {
                normal += q.rewardValue;
            }
            for (QuestDraft q : Generation.run(TestSupport.catalog(), host, TestSupport.config(Difficulty.NORMAL, 6),
                TestSupport.unlockedProgression(host), Map.of(), Set.of(), List.of(), 6, 1L, seed, 0, new RewardOptions(false, 2.0)).drafts()) {
                doubled += q.rewardValue;
                assertTrue(SchemaCheck.check(q.json, host.caps).isEmpty());
            }
        }
        double ratio = doubled / normal;
        assertTrue(ratio > 1.6 && ratio < 2.4, "reward ratio " + ratio);
    }

    @Test
    void setsAreDeterministicAndAvoidHistory() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = started(host);
        List<GeneratedQuest> a = g.generateSet(request(42, 3, Difficulty.NORMAL, 1.0, Set.of()));
        List<GeneratedQuest> b = g.generateSet(request(42, 3, Difficulty.NORMAL, 1.0, Set.of()));
        assertEquals(3, a.size());
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i).json(), b.get(i).json());
        Set<String> history = new HashSet<>();
        a.forEach(q -> history.add(q.signature()));
        for (GeneratedQuest q : g.generateSet(request(42, 3, Difficulty.NORMAL, 1.0, history))) {
            assertFalse(history.contains(q.signature()), "repeated " + q.signature());
        }
        // a stand-alone set changes nothing on the board
        assertEquals(5, g.servedQuests().size());
    }

    @Test
    void longerSetsTakeLongerAndPayMore() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = started(host);
        double normalMin = 0, longMin = 0, normalValue = 0, longValue = 0;
        int normalN = 0, longN = 0;
        for (long seed = 1; seed <= 15; seed++) {
            for (GeneratedQuest q : g.generateSet(request(seed, 4, Difficulty.HARD, 1.0, Set.of()))) {
                normalMin += q.estMinutes();
                normalValue += q.rewardValue();
                normalN++;
            }
            for (GeneratedQuest q : g.generateSet(request(seed, 4, Difficulty.HARD, 5.0, Set.of()))) {
                longMin += q.estMinutes();
                longValue += q.rewardValue();
                longN++;
                assertTrue(SchemaCheck.check(q.json(), host.caps).isEmpty(), () -> SchemaCheck.check(q.json(), host.caps).toString());
            }
        }
        assertTrue(longN >= normalN * 0.75, "long quests generated: " + longN + " of " + normalN);
        assertTrue(longMin / longN > 3.0 * (normalMin / normalN), "minutes " + longMin / longN + " vs " + normalMin / normalN);
        assertTrue(longValue / longN > 3.0 * (normalValue / normalN), "value " + longValue / longN + " vs " + normalValue / normalN);
    }

    @Test
    void serverGoalShape() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = started(host);
        Set<String> kept = Set.of("mine_block", "collect_item", "kill_mob", "breed_animal", "smelt_item");
        Set<String> excluded = new HashSet<>();
        for (String type : host.caps.objectiveTypes) excluded.add(type.replace("justquests:", ""));
        excluded.removeAll(kept);
        int n = 0, big = 0;
        for (long seed = 1; seed <= 20; seed++) {
            for (GeneratedQuest q : g.generateSet(new SetRequest(seed, 1, Difficulty.NORMAL, 30.0, 0.2, true, true, null,
                Set.of(), excluded))) {
                n++;
                List<JsonObject> objectives = Json.objects(q.json(), "objectives");
                assertEquals(1, objectives.size());
                String type = TestSupport.shortType(objectives.get(0));
                assertTrue(kept.contains(type), type);
                if (q.estMinutes() > 120) big++;
            }
        }
        assertTrue(n >= 15, "goals generated: " + n);
        assertTrue(big >= n * 0.8, big + " of " + n + " goals are long");
    }

    @Test
    void personalProgressionIgnoresGameDay() {
        FakeHost host = new FakeHost();
        host.world.day = 200;   // the server's days would unlock the Nether and the End
        QuestGeneratorV2 g = started(host);
        FakeHost.World player = new FakeHost.World();
        player.day = 200;
        player.online = 1;      // this player has not been to the Nether
        for (long seed = 1; seed <= 25; seed++) {
            for (GeneratedQuest q : g.generateSet(new SetRequest(seed, 3, Difficulty.HARD, 1.0, 1.0, false, false, player,
                Set.of(), Set.of()))) {
                String json = q.json().toString();
                assertFalse(json.contains("minecraft:the_nether") || json.contains("minecraft:the_end"), json);
            }
        }
    }
}
