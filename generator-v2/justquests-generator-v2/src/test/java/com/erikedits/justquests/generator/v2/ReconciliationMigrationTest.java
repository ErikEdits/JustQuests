package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.ClaimState;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.StartResult;
import com.erikedits.justquests.generator.v2.internal.state.GenState;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.9 — start-up reconciliation (§10.5) and v1 migration (§11.3). */
class ReconciliationMigrationTest {
    static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static final String V1_FILE = """
        {
          "lastRefresh": 1789990000000,
          "history": {
            "justquests:collect_item|minecraft:oak_log": 1789990000000,
            "justquests:kill_mob|minecraft:husk": 1789990000000
          },
          "quests": {
            "1789990000000_0": {
              "title": "Gather 32 Oak Log", "category": "generated",
              "objectives": [{"type": "justquests:collect_item", "item": "minecraft:oak_log", "count": 32}],
              "rewards": [{"type": "justquests:give_item", "item": "minecraft:bread", "count": 4}]
            },
            "1789990000000_1": {
              "title": "Defeat 8 Husk", "category": "generated",
              "objectives": [{"type": "justquests:kill_mob", "entity": "minecraft:husk", "count": 8}],
              "rewards": [{"type": "justquests:give_item", "item": "minecraft:emerald", "count": 2}]
            }
          }
        }
        """;

    @Test
    void releasesClaimsWhoseHolderLostTheQuest() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        String q = g.servedQuests().keySet().iterator().next();
        g.tryClaim(q, A);
        g.stop();
        QuestGeneratorV2 g2 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        StartResult r = g2.start(Map.of());
        assertEquals(1, r.releasedClaims().size());
        assertEquals(q, r.releasedClaims().get(0).questId());
        assertEquals(ClaimState.AVAILABLE, g2.claim(q).state());
    }

    @Test
    void adoptsUnclaimedActiveQuestsAndReportsDeadOnes() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        String q = g.servedQuests().keySet().iterator().next();
        g.stop();
        QuestGeneratorV2 g2 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        StartResult r = g2.start(Map.of(q, A, "justquests:gen/123_4", B, "justquests:starter/wood", B));
        assertEquals(List.of("justquests:gen/123_4"), r.deadQuestIds());
        assertEquals(ClaimState.CLAIMED, g2.claim(q).state());
        assertEquals(A, g2.claim(q).holder());
    }

    @Test
    void doubleHoldersAreKeptWithWarning() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        String q = g.servedQuests().keySet().iterator().next();
        g.stop();
        QuestGeneratorV2 g2 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        StartResult r = g2.startWithHolders(Map.of(q, Set.of(A, B)));
        assertTrue(r.deadQuestIds().isEmpty());
        assertEquals(2, g2.holders(q).size());
        assertTrue(host.log.lines.stream().anyMatch(l -> l.startsWith("WARN") && l.contains("several players")));
        assertEquals(ClaimResult.ALREADY_YOURS, g2.tryClaim(q, B));
        g2.onComplete(q, A);
        assertEquals(ClaimResult.ALREADY_YOURS, g2.tryClaim(q, B), "B can still finish");
    }

    @Test
    void migratesV1() {
        FakeHost host = new FakeHost();
        host.store.files.put("generated.json", V1_FILE);
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        String active = "justquests:gen/1789990000000_0";
        StartResult r = g.start(Map.of(active, A));
        assertTrue(r.deadQuestIds().isEmpty());
        assertTrue(g.servedQuests().containsKey(active), "active v1 quest is retained");
        assertFalse(g.servedQuests().containsKey("justquests:gen/1789990000000_1"), "inactive v1 quest is dropped");
        assertEquals(ClaimState.CLAIMED, g.claim(active).state());
        assertEquals(5 + 1, g.servedQuests().size());
        GenState s = GenState.fromJson(Json.parseObject(host.store.files.get(GenState.FILE)));
        assertTrue(s.history.containsKey("collect_item:minecraft:oak_log"), "v1 history converted");
        assertTrue(s.history.containsKey("kill_mob:minecraft:husk"));
        assertTrue(s.migratedFromV1);
        assertEquals(V1_FILE, host.store.files.get("generated.json"), "v1 file untouched");
        for (var e : g.servedQuests().entrySet()) {
            if (!e.getKey().equals(active)) {
                String text = Json.compact(e.getValue());
                assertFalse(text.contains("\"minecraft:husk\"") && text.contains("kill_mob") && !text.contains("\"mode\""),
                    "v1 history must block husk hunts");
            }
        }
        g.onComplete(active, A);
        host.now += 12 * 3_600_000L;
        g.tick();
        assertFalse(g.servedQuests().containsKey(active));
    }

    @Test
    void migrationRunsOnlyOnce() {
        FakeHost host = new FakeHost();
        host.store.files.put("generated.json", V1_FILE);
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        g.stop();
        QuestGeneratorV2 g2 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        StartResult r = g2.start(Map.of("justquests:gen/1789990000000_1", A));
        assertEquals(List.of("justquests:gen/1789990000000_1"), r.deadQuestIds(),
            "after migration an unknown v1 id is dead");
    }
}
