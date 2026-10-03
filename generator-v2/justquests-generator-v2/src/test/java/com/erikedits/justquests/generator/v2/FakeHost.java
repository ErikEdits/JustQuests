package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.ContentKind;
import com.erikedits.justquests.generator.v2.api.ContentView;
import com.erikedits.justquests.generator.v2.api.GenLog;
import com.erikedits.justquests.generator.v2.api.GeneratorHost;
import com.erikedits.justquests.generator.v2.api.HostCapabilities;
import com.erikedits.justquests.generator.v2.api.QuestValidator;
import com.erikedits.justquests.generator.v2.api.StateStore;
import com.erikedits.justquests.generator.v2.api.TriState;
import com.erikedits.justquests.generator.v2.api.ValidationResult;
import com.erikedits.justquests.generator.v2.api.WorldContext;
import com.erikedits.justquests.generator.v2.internal.gen.SchemaCheck;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Configurable fake host for tests: an in-memory world with a fixed clock. By default every
 * {@code minecraft:} id exists, ids of "loaded" mods exist, all TriState answers are UNKNOWN and
 * English names are null (the hardest case for the core).
 */
public final class FakeHost implements GeneratorHost {
    public final Content content = new Content();
    public final World world = new World();
    public final Store store = new Store();
    public final Validator validator = new Validator();
    public final Caps caps = new Caps();
    public final Log log = new Log();
    public long now = 1_790_000_000_000L; // 2026-09-21

    @Override
    public ContentView content() {
        return content;
    }

    @Override
    public WorldContext world() {
        return world;
    }

    @Override
    public StateStore store() {
        return store;
    }

    @Override
    public QuestValidator validator() {
        return validator;
    }

    @Override
    public HostCapabilities capabilities() {
        return caps;
    }

    @Override
    public GenLog log() {
        return log;
    }

    @Override
    public long currentTimeMillis() {
        return now;
    }

    public FakeHost withMods(String... mods) {
        content.loadedMods.addAll(List.of(mods));
        return this;
    }

    public static final class Content implements ContentView {
        public String loader = "neoforge";
        public String version = "1.21.1";
        public final Set<String> loadedMods = new HashSet<>();
        public final Set<String> missing = new HashSet<>();
        public final Set<String> dimensions = new HashSet<>(Set.of("minecraft:overworld", "minecraft:the_nether",
            "minecraft:the_end"));
        public final Map<String, TriState> craftable = new HashMap<>();
        public final Map<String, TriState> smeltable = new HashMap<>();
        public final Map<String, TriState> breedable = new HashMap<>();
        public final Map<String, TriState> tamable = new HashMap<>();
        public final Map<String, TriState> consumable = new HashMap<>();
        public TriState defaultAnswer = TriState.UNKNOWN;
        public final Map<String, Integer> stackSizes = new HashMap<>();
        public final Map<String, String> names = new HashMap<>();
        public final Map<String, Set<String>> itemTags = new HashMap<>();
        public int existsCalls;

        public Content() {
            itemTags.put("minecraft:logs", Set.of("minecraft:oak_log", "minecraft:birch_log", "minecraft:spruce_log"));
            itemTags.put("minecraft:wool", Set.of("minecraft:white_wool", "minecraft:black_wool"));
            itemTags.put("minecraft:beds", Set.of("minecraft:white_bed", "minecraft:red_bed"));
            itemTags.put("minecraft:small_flowers", Set.of("minecraft:dandelion", "minecraft:poppy"));
            itemTags.put("c:ingots/iron", Set.of("minecraft:iron_ingot"));
        }

        private boolean exists(String id) {
            existsCalls++;
            if (id == null || missing.contains(id)) {
                return false;
            }
            int i = id.indexOf(':');
            String ns = i < 0 ? "minecraft" : id.substring(0, i);
            return "minecraft".equals(ns) || loadedMods.contains(ns);
        }

        @Override
        public String loaderName() {
            return loader;
        }

        @Override
        public String minecraftVersion() {
            return version;
        }

        @Override
        public boolean isModLoaded(String modId) {
            return loadedMods.contains(modId);
        }

        @Override
        public boolean itemExists(String id) {
            return exists(id);
        }

        @Override
        public boolean blockExists(String id) {
            return exists(id);
        }

        @Override
        public boolean entityTypeExists(String id) {
            return exists(id);
        }

        @Override
        public boolean dimensionExists(String id) {
            return dimensions.contains(id) && !missing.contains(id);
        }

        @Override
        public Set<String> itemTagMembers(String tagId) {
            if (tagId.equals("c:ingots/zinc") && loadedMods.contains("create")) {
                return Set.of("create:zinc_ingot");
            }
            return itemTags.getOrDefault(tagId, Set.of());
        }

        @Override
        public Set<String> blockTagMembers(String tagId) {
            return Set.of();
        }

        @Override
        public Set<String> entityTypeTagMembers(String tagId) {
            return Set.of();
        }

        @Override
        public TriState hasCraftingRecipe(String itemId) {
            return craftable.getOrDefault(itemId, defaultAnswer);
        }

        @Override
        public TriState hasSmeltingRecipe(String outputItemId) {
            return smeltable.getOrDefault(outputItemId, defaultAnswer);
        }

        @Override
        public int maxStackSize(String itemId) {
            return stackSizes.getOrDefault(itemId, -1);
        }

        @Override
        public TriState isBreedableAnimal(String entityTypeId) {
            return breedable.getOrDefault(entityTypeId, defaultAnswer);
        }

        @Override
        public TriState isTamableAnimal(String entityTypeId) {
            return tamable.getOrDefault(entityTypeId, defaultAnswer);
        }

        @Override
        public TriState isConsumable(String itemId) {
            return consumable.getOrDefault(itemId, defaultAnswer);
        }

        @Override
        public String englishName(ContentKind kind, String id) {
            return names.get(id);
        }
    }

    public static final class World implements WorldContext {
        public long seed = 12345L;
        public long day = 20;
        public int online = 1;
        public int known = 3;
        public final Map<String, Double> shares = new HashMap<>();
        public boolean singleplayer = false;

        @Override
        public long worldSeed() {
            return seed;
        }

        @Override
        public long gameDay() {
            return day;
        }

        @Override
        public int onlinePlayerCount() {
            return online;
        }

        @Override
        public int knownPlayerCount() {
            return known;
        }

        @Override
        public double onlineShareWithAdvancement(String advancementId) {
            if (online <= 0) {
                return -1;
            }
            return shares.getOrDefault(advancementId, 0.0);
        }

        @Override
        public boolean isSingleplayer() {
            return singleplayer;
        }
    }

    public static final class Store implements StateStore {
        public final Map<String, String> files = new TreeMap<>();
        public int writes;

        @Override
        public Optional<String> read(String fileName) {
            return Optional.ofNullable(files.get(fileName));
        }

        @Override
        public void write(String fileName, String content) {
            writes++;
            files.put(fileName, content);
        }

        @Override
        public List<String> list(String directory) {
            List<String> out = new ArrayList<>();
            String prefix = directory.endsWith("/") ? directory : directory + "/";
            for (String f : files.keySet()) {
                if (f.startsWith(prefix) && !f.substring(prefix.length()).contains("/")) {
                    out.add(f.substring(prefix.length()));
                }
            }
            return out;
        }
    }

    /** Runs the strict schema check (like the mod's codec) and an optional extra rejection rule. */
    public final class Validator implements QuestValidator {
        public Predicate<JsonObject> reject = q -> false;
        public int calls;

        @Override
        public ValidationResult validate(String questId, JsonObject questJson) {
            calls++;
            List<String> problems = SchemaCheck.check(questJson, caps);
            if (!problems.isEmpty()) {
                return ValidationResult.fail(String.join("; ", problems));
            }
            if (reject.test(questJson)) {
                return ValidationResult.fail("rejected by test rule");
            }
            return ValidationResult.OK;
        }
    }

    public static final class Caps implements HostCapabilities {
        public final Set<String> objectiveTypes = new LinkedHashSet<>(List.of("justquests:collect_item",
            "justquests:mine_block", "justquests:craft_item", "justquests:smelt_item", "justquests:kill_mob",
            "justquests:breed_animal", "justquests:tame_animal", "justquests:consume_item", "justquests:place_block",
            "justquests:visit_dimension", "justquests:gain_advancement", "justquests:reach_level",
            "justquests:reach_location", "justquests:enchant_item", "justquests:use_item"));
        public final Set<String> rewardTypes = new LinkedHashSet<>(List.of("justquests:give_item", "justquests:xp",
            "justquests:effect", "justquests:loot_table", "justquests:message", "justquests:command"));
        public final Set<String> tagTypes = new HashSet<>(Set.of("justquests:collect_item", "justquests:craft_item",
            "justquests:smelt_item", "justquests:consume_item", "justquests:enchant_item", "justquests:use_item",
            "justquests:mine_block", "justquests:place_block", "justquests:kill_mob", "justquests:breed_animal",
            "justquests:tame_animal"));

        @Override
        public boolean supportsTag(String objectiveType) {
            return tagTypes.contains(objectiveType);
        }

        @Override
        public Set<String> objectiveTypes() {
            return objectiveTypes;
        }

        @Override
        public Set<String> rewardTypes() {
            return rewardTypes;
        }
    }

    public static final class Log implements GenLog {
        public final List<String> lines = new ArrayList<>();
        public boolean echo = false;

        @Override
        public void info(String msg) {
            add("INFO " + msg);
        }

        @Override
        public void warn(String msg) {
            add("WARN " + msg);
        }

        @Override
        public void error(String msg, Throwable t) {
            add("ERROR " + msg + (t == null ? "" : " " + t));
        }

        private void add(String s) {
            lines.add(s);
            if (echo) {
                System.out.println(s);
            }
        }

        public long count(String prefix) {
            return lines.stream().filter(l -> l.startsWith(prefix)).count();
        }
    }
}
