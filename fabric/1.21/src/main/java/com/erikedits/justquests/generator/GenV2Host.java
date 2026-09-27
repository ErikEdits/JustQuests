package com.erikedits.justquests.generator;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.Quest;
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
import com.erikedits.justquests.storage.WorldQuestStore;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Host adapter for the generator v2 core on fabric 1.21 (core INTEGRATION.md section 2).
 * One per server; every method runs on the server thread and never throws.
 *
 * <p>The same choices in every build: recipe, animal-class and consumable checks answer
 * {@code UNKNOWN} (the catalogs carry that knowledge), and {@code englishName} answers
 * {@code null} - on an integrated server the language is the player's client language, which
 * would mix non-English item names into the English quest text.
 */
public final class GenV2Host implements GeneratorHost {
    static final String LOADER = "fabric";
    static final String MINECRAFT = "1.21";

    private final MinecraftServer server;
    private final Content content = new Content();
    private final World world = new World();
    private final Store store = new Store();
    private final Validator validator = new Validator();
    private final Caps caps = new Caps();
    private final Log log = new Log();

    public GenV2Host(MinecraftServer server) {
        this.server = server;
    }

    @Override public ContentView content() { return content; }
    @Override public WorldContext world() { return world; }
    @Override public StateStore store() { return store; }
    @Override public QuestValidator validator() { return validator; }
    @Override public HostCapabilities capabilities() { return caps; }
    @Override public GenLog log() { return log; }
    @Override public long currentTimeMillis() { return System.currentTimeMillis(); }

    private static ResourceLocation rl(String id) {
        try {
            return id == null ? null : ResourceLocation.tryParse(id);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static <T> Set<String> keys(Optional<HolderSet.Named<T>> tag) {
        Set<String> out = new LinkedHashSet<>();
        tag.ifPresent(set -> set.stream().forEach(h -> h.unwrapKey().ifPresent(k -> out.add(k.location().toString()))));
        return out;
    }

    private static boolean safe(BooleanSupplier s) {
        try {
            return s.getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Set<String> safeSet(Supplier<Set<String>> s) {
        try {
            return s.get();
        } catch (RuntimeException e) {
            return Set.of();
        }
    }

    // ------------------------------------------------------------------ registry access
    // BuiltInRegistries, get/getTag, feature flags (1.19.3-1.21.1)

    // registered AND enabled for this world: experimental content is registered even when its
    // feature flag is off (cherry wood on 1.19.4, the breeze on 1.20.4, pale oak on 1.21.2/1.21.3)
    private boolean itemOk(ResourceLocation r) {
        return BuiltInRegistries.ITEM.containsKey(r)
            && BuiltInRegistries.ITEM.get(r).isEnabled(server.getWorldData().enabledFeatures());
    }

    private boolean blockOk(ResourceLocation r) {
        return BuiltInRegistries.BLOCK.containsKey(r)
            && BuiltInRegistries.BLOCK.get(r).isEnabled(server.getWorldData().enabledFeatures());
    }

    private boolean entityOk(ResourceLocation r) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(r)
            && BuiltInRegistries.ENTITY_TYPE.get(r).isEnabled(server.getWorldData().enabledFeatures());
    }

    private Set<String> itemTag(ResourceLocation r) {
        return keys(BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, r)));
    }

    private Set<String> blockTag(ResourceLocation r) {
        return keys(BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, r)));
    }

    private Set<String> entityTag(ResourceLocation r) {
        return keys(BuiltInRegistries.ENTITY_TYPE.getTag(TagKey.create(Registries.ENTITY_TYPE, r)));
    }

    private int stackSize(ResourceLocation r) {
        return BuiltInRegistries.ITEM.containsKey(r) ? new ItemStack(BuiltInRegistries.ITEM.get(r)).getMaxStackSize() : -1;
    }

    // ------------------------------------------------------------------ ContentView

    private final class Content implements ContentView {
        @Override public String loaderName() { return LOADER; }

        @Override public String minecraftVersion() { return MINECRAFT; }

        @Override
        public boolean isModLoaded(String modId) {
            try {
                return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
            } catch (RuntimeException e) {
                return false;
            }
        }

        @Override
        public boolean itemExists(String id) {
            ResourceLocation r = rl(id);
            return r != null && safe(() -> itemOk(r));
        }

        @Override
        public boolean blockExists(String id) {
            ResourceLocation r = rl(id);
            return r != null && safe(() -> blockOk(r));
        }

        @Override
        public boolean entityTypeExists(String id) {
            ResourceLocation r = rl(id);
            return r != null && safe(() -> entityOk(r));
        }

        @Override
        public boolean dimensionExists(String id) {
            ResourceLocation r = rl(id);
            if (r == null) return false;
            for (ResourceKey<Level> key : server.levelKeys()) {
                if (key.location().equals(r)) return true;
            }
            return false;
        }

        @Override
        public Set<String> itemTagMembers(String tagId) {
            ResourceLocation r = rl(tagId);
            return r == null ? Set.of() : safeSet(() -> itemTag(r));
        }

        @Override
        public Set<String> blockTagMembers(String tagId) {
            ResourceLocation r = rl(tagId);
            return r == null ? Set.of() : safeSet(() -> blockTag(r));
        }

        @Override
        public Set<String> entityTypeTagMembers(String tagId) {
            ResourceLocation r = rl(tagId);
            return r == null ? Set.of() : safeSet(() -> entityTag(r));
        }

        @Override public TriState hasCraftingRecipe(String itemId) { return TriState.UNKNOWN; }

        @Override public TriState hasSmeltingRecipe(String outputItemId) { return TriState.UNKNOWN; }

        @Override
        public int maxStackSize(String itemId) {
            ResourceLocation r = rl(itemId);
            if (r == null) return -1;
            try {
                return stackSize(r);
            } catch (RuntimeException e) {
                return -1;
            }
        }

        @Override public TriState isBreedableAnimal(String entityTypeId) { return TriState.UNKNOWN; }

        @Override public TriState isTamableAnimal(String entityTypeId) { return TriState.UNKNOWN; }

        @Override public TriState isConsumable(String itemId) { return TriState.UNKNOWN; }

        @Override public String englishName(ContentKind kind, String id) { return null; }
    }

    // ------------------------------------------------------------------ WorldContext

    private final class World implements WorldContext {
        @Override public long worldSeed() { return server.overworld().getSeed(); }

        @Override public long gameDay() { return server.overworld().getGameTime() / 24000L; }

        @Override public int onlinePlayerCount() { return server.getPlayerList().getPlayerCount(); }

        @Override
        public int knownPlayerCount() {
            WorldQuestStore s = WorldQuestStore.get();
            return Math.max(onlinePlayerCount(), s == null ? 0 : s.allPlayers().size());
        }

        @Override
        public double onlineShareWithAdvancement(String advancementId) {
            try {
                List<ServerPlayer> players = server.getPlayerList().getPlayers();
                ResourceLocation r = rl(advancementId);
                if (players.isEmpty() || r == null) return -1;
                net.minecraft.advancements.AdvancementHolder adv = server.getAdvancements().get(r);
                if (adv == null) return -1;
                long done = 0;
                for (ServerPlayer p : players) {
                    if (p.getAdvancements().getOrStartProgress(adv).isDone()) done++;
                }
                return (double) done / players.size();
            } catch (RuntimeException e) {
                return -1;
            }
        }

        @Override public boolean isSingleplayer() { return server.isSingleplayer(); }
    }

    // ------------------------------------------------------------------ StateStore

    private final class Store implements StateStore {
        private Path resolve(String name) {
            if (name == null || name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
                throw new IllegalArgumentException("bad file name " + name);
            }
            return server.getWorldPath(LevelResource.ROOT).resolve("justquests").resolve(name);
        }

        @Override
        public Optional<String> read(String fileName) {
            try {
                Path p = resolve(fileName);
                return Files.isRegularFile(p) ? Optional.of(Files.readString(p, StandardCharsets.UTF_8)) : Optional.empty();
            } catch (IOException | RuntimeException e) {
                JustQuests.LOG.warn("[GenV2] could not read " + fileName + ": " + e);
                return Optional.empty();
            }
        }

        @Override
        public void write(String fileName, String content) {
            try {
                Path target = resolve(fileName);
                Files.createDirectories(target.getParent());
                Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
                Files.writeString(tmp, content, StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException | RuntimeException e) {
                JustQuests.LOG.error("[GenV2] could not write " + fileName, e);
            }
        }

        @Override
        public List<String> list(String directory) {
            List<String> out = new ArrayList<>();
            try {
                Path dir = resolve(directory);
                if (Files.isDirectory(dir)) {
                    try (Stream<Path> s = Files.list(dir)) {
                        s.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).sorted().forEach(out::add);
                    }
                }
            } catch (IOException | RuntimeException e) {
                JustQuests.LOG.warn("[GenV2] could not list " + directory + ": " + e);
            }
            return out;
        }
    }

    // ------------------------------------------------------------------ QuestValidator

    private static final class Validator implements QuestValidator {
        @Override
        public ValidationResult validate(String questId, JsonObject questJson) {
            try {
                DataResult<Quest> r = Quest.CODEC.parse(JsonOps.INSTANCE, questJson);
                Optional<Quest> q = r.result();
                if (q.isEmpty()) {
                    return ValidationResult.fail(r.error().map(Object::toString).orElse("parse error"));
                }
                if (q.get().objectives().isEmpty()) return ValidationResult.fail("no objectives");
                if (q.get().objectives().stream().anyMatch(o -> o.requiredCount() <= 0)) {
                    return ValidationResult.fail("objective count <= 0");
                }
                return ValidationResult.OK;
            } catch (RuntimeException e) {   // an unknown type throws inside the dispatch codec
                return ValidationResult.fail(e.toString());
            }
        }
    }

    // ------------------------------------------------------------------ HostCapabilities

    private static final class Caps implements HostCapabilities {
        private static final Set<String> TAGGABLE = Set.of("justquests:collect_item", "justquests:craft_item",
            "justquests:smelt_item", "justquests:consume_item");
        private static final Set<String> OBJECTIVES = Set.of("justquests:collect_item", "justquests:kill_mob",
            "justquests:place_block", "justquests:craft_item", "justquests:tame_animal", "justquests:gain_advancement",
            "justquests:visit_dimension", "justquests:reach_level", "justquests:reach_location", "justquests:mine_block",
            "justquests:breed_animal", "justquests:consume_item", "justquests:smelt_item");
        private static final Set<String> REWARDS = Set.of("justquests:give_item", "justquests:xp", "justquests:effect",
            "justquests:loot_table", "justquests:message", "justquests:command");

        @Override public boolean supportsTag(String objectiveType) { return TAGGABLE.contains(objectiveType); }

        @Override public Set<String> objectiveTypes() { return OBJECTIVES; }

        @Override public Set<String> rewardTypes() { return REWARDS; }
    }

    // ------------------------------------------------------------------ GenLog

    private static final class Log implements GenLog {
        @Override public void info(String msg) { JustQuests.LOG.info(msg); }

        @Override public void warn(String msg) { JustQuests.LOG.warn(msg); }

        @Override
        public void error(String msg, Throwable t) {
            if (t == null) JustQuests.LOG.error(msg); else JustQuests.LOG.error(msg, t);
        }
    }
}
