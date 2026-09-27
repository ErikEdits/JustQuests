package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.ContentKind;
import com.erikedits.justquests.generator.v2.api.ContentView;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.GeneratorHost;
import com.erikedits.justquests.generator.v2.api.GenLog;
import com.erikedits.justquests.generator.v2.api.HostCapabilities;
import com.erikedits.justquests.generator.v2.api.QuestValidator;
import com.erikedits.justquests.generator.v2.api.StateStore;
import com.erikedits.justquests.generator.v2.api.TriState;
import com.erikedits.justquests.generator.v2.api.WorldContext;
import com.erikedits.justquests.generator.v2.internal.util.English;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Robustness: a hostile or broken host must never make the facade throw, and whatever the core
 * serves must still be schema-valid and only reference ids the host confirmed.
 */
class HostFuzzTest {
    private static final long MIN = 60_000L;

    /**
     * Content view with deterministic per-id answers (so "exists" is consistent within a run) and
     * random failures per call: exceptions, null returns and hostile display names.
     */
    static final class FuzzContent implements ContentView {
        final FakeHost.Content base;
        final long salt;
        final int missingPercent;
        final double throwChance;
        final SplittableRandom callRng;
        int thrown;

        FuzzContent(FakeHost.Content base, long salt, int missingPercent, double throwChance) {
            this.base = base;
            this.salt = salt;
            this.missingPercent = missingPercent;
            this.throwChance = throwChance;
            this.callRng = new SplittableRandom(salt ^ 0x5DEECE66DL);
        }

        private int roll(String id, int kind) {
            long h = salt * 31 + kind;
            h = h * 1_000_003L + (id == null ? 0 : id.hashCode());
            h ^= (h >>> 33);
            h *= 0xff51afd7ed558ccdL;
            h ^= (h >>> 33);
            return (int) Math.floorMod(h, 100L);
        }

        private void maybeThrow(String what) {
            if (callRng.nextDouble() < throwChance) {
                thrown++;
                throw new IllegalStateException("fuzz: " + what + " exploded");
            }
        }

        /** True if the fuzz host reports the id as present (independent of the random throws). */
        boolean present(String id) {
            return id != null && base.itemExists(id) && roll(id, 0) >= missingPercent;
        }

        private TriState tri(String id, int kind) {
            int r = roll(id, kind);
            return r < 40 ? TriState.YES : r < 60 ? TriState.NO : r < 95 ? TriState.UNKNOWN : null;
        }

        @Override
        public String loaderName() {
            maybeThrow("loaderName");
            return callRng.nextBoolean() ? "neoforge" : null;
        }

        @Override
        public String minecraftVersion() {
            maybeThrow("minecraftVersion");
            int r = callRng.nextInt(4);
            return r == 0 ? "1.21.1" : r == 1 ? null : r == 2 ? "garbage-version" : "";
        }

        @Override
        public boolean isModLoaded(String modId) {
            maybeThrow("isModLoaded");
            return base.isModLoaded(modId);
        }

        @Override
        public boolean itemExists(String id) {
            maybeThrow("itemExists");
            return present(id);
        }

        @Override
        public boolean blockExists(String id) {
            maybeThrow("blockExists");
            return present(id);
        }

        @Override
        public boolean entityTypeExists(String id) {
            maybeThrow("entityTypeExists");
            return present(id);
        }

        @Override
        public boolean dimensionExists(String id) {
            maybeThrow("dimensionExists");
            return base.dimensionExists(id);
        }

        @Override
        public Set<String> itemTagMembers(String tagId) {
            maybeThrow("itemTagMembers");
            return callRng.nextInt(5) == 0 ? null : base.itemTagMembers(tagId);
        }

        @Override
        public Set<String> blockTagMembers(String tagId) {
            maybeThrow("blockTagMembers");
            return null;
        }

        @Override
        public Set<String> entityTypeTagMembers(String tagId) {
            maybeThrow("entityTypeTagMembers");
            return Set.of();
        }

        @Override
        public TriState hasCraftingRecipe(String itemId) {
            maybeThrow("hasCraftingRecipe");
            return tri(itemId, 1);
        }

        @Override
        public TriState hasSmeltingRecipe(String outputItemId) {
            maybeThrow("hasSmeltingRecipe");
            return tri(outputItemId, 2);
        }

        @Override
        public int maxStackSize(String itemId) {
            maybeThrow("maxStackSize");
            int r = roll(itemId, 3);
            return r < 10 ? 0 : r < 20 ? -7 : r < 30 ? 1 : r < 40 ? 16 : r < 50 ? Integer.MAX_VALUE : 64;
        }

        @Override
        public TriState isBreedableAnimal(String entityTypeId) {
            maybeThrow("isBreedableAnimal");
            return tri(entityTypeId, 4);
        }

        @Override
        public TriState isTamableAnimal(String entityTypeId) {
            maybeThrow("isTamableAnimal");
            return tri(entityTypeId, 5);
        }

        @Override
        public TriState isConsumable(String itemId) {
            maybeThrow("isConsumable");
            return tri(itemId, 6);
        }

        @Override
        public String englishName(ContentKind kind, String id) {
            maybeThrow("englishName");
            switch (roll(id, 7) % 8) {
                case 0:
                    return null;
                case 1:
                    return "";
                case 2:
                    return "§6Shiny §lThing§r";
                case 3:
                    return "item." + id.replace(':', '.');
                case 4:
                    return "An Extraordinarily Long Display Name That Never Fits In Any Title";
                case 5:
                    return "Weird {Name} [Thing]\n<b>";
                default:
                    return base.englishName(kind, id);
            }
        }
    }

    /** Host that delegates to a FakeHost but swaps in the fuzz content and can break other parts. */
    static final class FuzzHost implements GeneratorHost {
        final FakeHost base;
        final FuzzContent content;
        boolean worldThrows;
        boolean capabilitiesThrow;
        boolean validatorReturnsNull;

        FuzzHost(FakeHost base, FuzzContent content) {
            this.base = base;
            this.content = content;
        }

        @Override
        public ContentView content() {
            return content;
        }

        @Override
        public WorldContext world() {
            if (worldThrows) {
                throw new IllegalStateException("fuzz: world unavailable");
            }
            return base.world();
        }

        @Override
        public StateStore store() {
            return base.store();
        }

        @Override
        public QuestValidator validator() {
            if (validatorReturnsNull) {
                return (id, json) -> null;
            }
            return base.validator();
        }

        @Override
        public HostCapabilities capabilities() {
            if (capabilitiesThrow) {
                throw new IllegalStateException("fuzz: capabilities unavailable");
            }
            return base.capabilities();
        }

        @Override
        public GenLog log() {
            return base.log();
        }

        @Override
        public long currentTimeMillis() {
            return base.currentTimeMillis();
        }
    }

    private static void assertServedSane(FuzzHost h, QuestGeneratorV2 g, String ctx) {
        Map<String, JsonObject> served = assertDoesNotThrow(g::servedQuests, ctx);
        for (Map.Entry<String, JsonObject> e : served.entrySet()) {
            JsonObject q = e.getValue();
            List<String> problems = StrictQuestCheck.check(q, h.base.caps);
            assertTrue(problems.isEmpty(), ctx + " " + e.getKey() + ": " + problems + "\n" + q);
            String title = Json.str(q, "title", "");
            assertFalse(title.contains("§") || title.contains("{") || title.contains("\n"), ctx + " title " + title);
            String description = Json.str(q, "description", "");
            assertFalse(description.contains("§") || description.contains("{"), ctx + " description " + description);
            for (JsonElement o : q.getAsJsonArray("objectives")) {
                JsonObject obj = o.getAsJsonObject();
                if (obj.has("tag") || !obj.has("item") && !obj.has("block") && !obj.has("entity")) {
                    continue;
                }
                String target = TestSupport.objectiveTarget(obj);
                if (target.startsWith("#")) {
                    continue; // tag objectives: membership came from the host's tag view
                }
                assertTrue(h.content.present(target), ctx + " served a quest for an id the host denied: " + target);
            }
            for (JsonElement r : q.getAsJsonArray("rewards")) {
                JsonObject rew = r.getAsJsonObject();
                if (rew.has("item")) {
                    String item = rew.get("item").getAsString();
                    assertTrue(h.content.present(item), ctx + " reward item the host denied: " + item);
                    int count = rew.has("count") ? rew.get("count").getAsInt() : 1;
                    assertTrue(count >= 1 && count <= 64 * 9, ctx + " reward count " + count);
                }
            }
        }
    }

    @Test
    void randomAnswersAndExceptionsNeverEscape() {
        for (int round = 0; round < 24; round++) {
            FakeHost base = new FakeHost().withMods(round % 3 == 0 ? new String[]{"farmersdelight", "botania"}
                : round % 3 == 1 ? new String[]{"create", "farmersdelight", "mekanism", "twilightforest", "botania"}
                : new String[0]);
            base.content.dimensions.add("twilightforest:twilight_forest");
            FuzzContent content = new FuzzContent(base.content, 1000L + round, 5 + round % 4 * 10,
                round % 2 == 0 ? 0.02 : 0.10);
            FuzzHost h = new FuzzHost(base, content);
            Difficulty d = Difficulty.values()[round % 3];
            GeneratorConfig cfg = TestSupport.config(d, 5 + round % 16);
            String ctx = "round " + round + " (" + d + ")";
            QuestGeneratorV2 g = assertDoesNotThrow(() -> new QuestGeneratorV2(h, cfg), ctx);
            assertDoesNotThrow(() -> g.start(Map.of()), ctx);
            assertServedSane(h, g, ctx);
            SplittableRandom rng = new SplittableRandom(round);
            List<UUID> players = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            for (int step = 0; step < 40; step++) {
                base.now += (5 + rng.nextInt(240)) * MIN;
                List<String> ids = new ArrayList<>(g.servedQuests().keySet());
                UUID p = players.get(rng.nextInt(players.size()));
                String q = ids.isEmpty() ? "justquests:gen/1_1" : ids.get(rng.nextInt(ids.size()));
                switch (rng.nextInt(8)) {
                    case 0 -> assertDoesNotThrow(g::tick, ctx);
                    case 1 -> assertDoesNotThrow(g::reroll, ctx);
                    case 2 -> assertDoesNotThrow(() -> g.tryClaim(q, p), ctx);
                    case 3 -> assertDoesNotThrow(() -> g.onComplete(q, p), ctx);
                    case 4 -> assertDoesNotThrow(() -> g.onAbandon(q, p), ctx);
                    case 5 -> assertDoesNotThrow(() -> g.preview(3), ctx);
                    case 6 -> assertDoesNotThrow(() -> g.explain(q), ctx);
                    default -> assertDoesNotThrow(g::status, ctx);
                }
                assertServedSane(h, g, ctx + " step " + step);
            }
            assertTrue(g.selfTest().isEmpty(), ctx + " " + g.selfTest());
            assertDoesNotThrow(g::stop, ctx);
        }
    }

    @Test
    void brokenWorldCapabilitiesAndValidatorAreSurvived() {
        FakeHost base = new FakeHost();
        FuzzContent content = new FuzzContent(base.content, 7L, 0, 0.0);
        FuzzHost h = new FuzzHost(base, content);
        h.worldThrows = true;
        h.capabilitiesThrow = true;
        h.validatorReturnsNull = true;
        QuestGeneratorV2 g = new QuestGeneratorV2(h, TestSupport.config(Difficulty.NORMAL, 10));
        assertDoesNotThrow(() -> g.start(Map.of()));
        assertDoesNotThrow(g::tick);
        assertDoesNotThrow(g::reroll);
        assertDoesNotThrow(g::status);
        assertDoesNotThrow(g::stats);
        assertDoesNotThrow(() -> g.preview(5));
        // with no capabilities and a validator that answers null nothing may be served
        assertTrue(g.servedQuests().isEmpty(), "nothing can be validated, so nothing is served");
        // once the host recovers, the next rotation fills the set
        h.worldThrows = false;
        h.capabilitiesThrow = false;
        h.validatorReturnsNull = false;
        assertDoesNotThrow(g::reroll);
        assertEquals(10, g.servedQuests().size(), base.log.lines.toString());
        assertServedSane(h, g, "recovered");
    }

    @Test
    void everyContentCallThrowingYieldsAnEmptySetNotACrash() {
        FakeHost base = new FakeHost();
        FuzzContent content = new FuzzContent(base.content, 9L, 0, 1.0);
        FuzzHost h = new FuzzHost(base, content);
        QuestGeneratorV2 g = new QuestGeneratorV2(h, TestSupport.config(Difficulty.EASY, 8));
        assertDoesNotThrow(() -> g.start(Map.of()));
        assertDoesNotThrow(g::tick);
        assertTrue(g.servedQuests().isEmpty());
        assertTrue(content.thrown > 0);
        assertTrue(base.log.count("WARN") > 0, "host failures are logged");
    }

    @Test
    void hostNamesAreCleaned() {
        assertEquals("Shiny Thing", English.cleanHostName("§6Shiny §lThing§r"));
        assertEquals("Weird Name Thing", English.cleanHostName("Weird {Name} [Thing]\n"));
        assertNull(English.cleanHostName("item.farmersdelight.cabbage"));
        assertNull(English.cleanHostName("block.create.mechanical_press"));
        assertNull(English.cleanHostName("   "));
        assertNull(English.cleanHostName(null));
        assertNull(English.cleanHostName("x".repeat(English.MAX_HOST_NAME + 1)));
        assertEquals("Mr. Smith's Pie", English.cleanHostName("Mr. Smith's Pie"));
        assertEquals("Cabbage", English.cleanHostName("  Cabbage  "));
    }
}
