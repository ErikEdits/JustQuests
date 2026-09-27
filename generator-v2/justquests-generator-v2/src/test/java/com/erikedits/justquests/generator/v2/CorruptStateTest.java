package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.StartResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.15 — a corrupt state file leads to a fresh start with a backup, never an exception. */
class CorruptStateTest {
    @Test
    void corruptStateIsBackedUp() {
        for (String bad : new String[]{"{not json", "[1,2,3]", "{\"format\": 99}", "", "{\"format\": 1, \"quests\": 5}"}) {
            FakeHost host = new FakeHost();
            host.store.files.put("generator_v2.json", bad);
            host.store.files.put("generator_v2_stats.json", "garbage{");
            QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
            StartResult r = g.start(Map.of());
            assertTrue(r.initialRotation().changed(), "fresh generation for " + bad);
            assertEquals(5, g.servedQuests().size());
            long backups = host.store.files.keySet().stream().filter(f -> f.startsWith("generator_v2.corrupt-")).count();
            if (!bad.equals("{\"format\": 1, \"quests\": 5}")) {
                assertEquals(1, backups, "backup for " + bad);
                String backup = host.store.files.entrySet().stream().filter(e -> e.getKey().startsWith("generator_v2.corrupt-"))
                    .findFirst().orElseThrow().getValue();
                assertEquals(bad, backup);
            }
            assertTrue(host.store.files.keySet().stream().anyMatch(f -> f.startsWith("generator_v2_stats.corrupt-")));
            assertFalse(host.store.files.get("generator_v2.json").equals(bad), "fresh state written");
        }
    }

    @Test
    void throwingStoreDoesNotBreakTheGenerator() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(new ThrowingStoreHost(host), TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        assertEquals(5, g.servedQuests().size());
        g.tick();
        g.save();
        g.stop();
    }

    /** Wraps a FakeHost with a store that throws on every call. */
    static final class ThrowingStoreHost implements com.erikedits.justquests.generator.v2.api.GeneratorHost {
        private final FakeHost h;

        ThrowingStoreHost(FakeHost h) {
            this.h = h;
        }

        @Override
        public com.erikedits.justquests.generator.v2.api.ContentView content() {
            return h.content;
        }

        @Override
        public com.erikedits.justquests.generator.v2.api.WorldContext world() {
            return h.world;
        }

        @Override
        public com.erikedits.justquests.generator.v2.api.StateStore store() {
            return new com.erikedits.justquests.generator.v2.api.StateStore() {
                @Override
                public java.util.Optional<String> read(String fileName) {
                    throw new IllegalStateException("disk on fire");
                }

                @Override
                public void write(String fileName, String content) {
                    throw new IllegalStateException("disk on fire");
                }
            };
        }

        @Override
        public com.erikedits.justquests.generator.v2.api.QuestValidator validator() {
            return h.validator;
        }

        @Override
        public com.erikedits.justquests.generator.v2.api.HostCapabilities capabilities() {
            return h.caps;
        }

        @Override
        public com.erikedits.justquests.generator.v2.api.GenLog log() {
            return h.log;
        }

        @Override
        public long currentTimeMillis() {
            return h.now;
        }
    }
}
