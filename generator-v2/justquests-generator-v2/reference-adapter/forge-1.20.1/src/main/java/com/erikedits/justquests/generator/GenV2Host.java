// REFERENCE ONLY — NOT COMPILED
// Forge 1.20.1 host adapter for the JustQuests generator v2 core (see INTEGRATION.md §2).
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
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** One per server. Every method runs on the server thread and never throws. */
public final class GenV2Host implements GeneratorHost {
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

    /** Call after datapack reloads (/reload) so recipe caches are rebuilt. */
    public void invalidateCaches() {
        content.craftOutputs = null;
        content.smeltOutputs = null;
    }

    private static ResourceLocation rl(String id) {
        return id == null ? null : ResourceLocation.tryParse(id);
    }

    // ------------------------------------------------------------------ ContentView

    private final class Content implements ContentView {
        Set<String> craftOutputs;
        Set<String> smeltOutputs;

        @Override public String loaderName() { return "forge"; }

        @Override public String minecraftVersion() { return SharedConstants.getCurrentVersion().getName(); }

        @Override public boolean isModLoaded(String modId) { return ModList.get().isLoaded(modId); }

        // registered AND enabled by the world's feature flags (experimental content is registered too)
        @Override public boolean itemExists(String id) { return enabled(BuiltInRegistries.ITEM, id); }

        @Override public boolean blockExists(String id) { return enabled(BuiltInRegistries.BLOCK, id); }

        @Override public boolean entityTypeExists(String id) { return enabled(BuiltInRegistries.ENTITY_TYPE, id); }

        private <T extends FeatureElement> boolean enabled(Registry<T> reg, String id) {
            ResourceLocation r = rl(id);
            if (r == null || !reg.containsKey(r)) return false;
            return reg.get(r).isEnabled(server.getWorldData().enabledFeatures());
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
            if (r == null) return Set.of();
            Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, r));
            return tag.map(GenV2Host::keys).orElse(Set.of());
        }

        @Override
        public Set<String> blockTagMembers(String tagId) {
            ResourceLocation r = rl(tagId);
            if (r == null) return Set.of();
            Optional<HolderSet.Named<Block>> tag = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, r));
            return tag.map(GenV2Host::keys).orElse(Set.of());
        }

        @Override
        public Set<String> entityTypeTagMembers(String tagId) {
            ResourceLocation r = rl(tagId);
            if (r == null) return Set.of();
            Optional<HolderSet.Named<EntityType<?>>> tag =
                BuiltInRegistries.ENTITY_TYPE.getTag(TagKey.create(Registries.ENTITY_TYPE, r));
            return tag.map(GenV2Host::keys).orElse(Set.of());
        }

        @Override
        public TriState hasCraftingRecipe(String itemId) {
            try {
                if (craftOutputs == null) {
                    craftOutputs = outputs(List.of(RecipeType.CRAFTING));
                }
                return TriState.of(craftOutputs.contains(itemId));
            } catch (RuntimeException e) {
                return TriState.UNKNOWN;
            }
        }

        @Override
        public TriState hasSmeltingRecipe(String outputItemId) {
            try {
                if (smeltOutputs == null) {
                    smeltOutputs = outputs(List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING));
                }
                return TriState.of(smeltOutputs.contains(outputItemId));
            } catch (RuntimeException e) {
                return TriState.UNKNOWN;
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Set<String> outputs(List<RecipeType<?>> types) {
            Set<String> out = new HashSet<>();
            for (RecipeType type : types) {
                for (Object o : server.getRecipeManager().getAllRecipesFor(type)) {
                    Recipe<?> recipe = (Recipe<?>) o;   // 1.20.1: no RecipeHolder yet
                    ItemStack result = recipe.getResultItem(server.registryAccess());
                    if (!result.isEmpty()) {
                        out.add(BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
                    }
                }
            }
            return out;
        }

        @Override
        public int maxStackSize(String itemId) {
            ResourceLocation r = rl(itemId);
            if (r == null || !BuiltInRegistries.ITEM.containsKey(r)) return -1;
            return BuiltInRegistries.ITEM.get(r).getMaxStackSize();   // getDefaultMaxStackSize() from 1.20.5
        }

        /** Class checks need an instance; the catalog is authoritative, so answer UNKNOWN. */
        @Override public TriState isBreedableAnimal(String entityTypeId) { return TriState.UNKNOWN; }

        @Override public TriState isTamableAnimal(String entityTypeId) { return TriState.UNKNOWN; }

        @Override
        public TriState isConsumable(String itemId) {
            ResourceLocation r = rl(itemId);
            if (r == null || !BuiltInRegistries.ITEM.containsKey(r)) return TriState.UNKNOWN;
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(r));
            UseAnim anim = stack.getUseAnimation();
            return anim == UseAnim.EAT || anim == UseAnim.DRINK ? TriState.YES : TriState.NO;
        }

        @Override
        public String englishName(ContentKind kind, String id) {
            ResourceLocation r = rl(id);
            if (r == null) return null;
            String key;
            switch (kind) {
                case ITEM: key = BuiltInRegistries.ITEM.containsKey(r) ? BuiltInRegistries.ITEM.get(r).getDescriptionId() : null; break;
                case BLOCK: key = BuiltInRegistries.BLOCK.containsKey(r) ? BuiltInRegistries.BLOCK.get(r).getDescriptionId() : null; break;
                case ENTITY: key = BuiltInRegistries.ENTITY_TYPE.containsKey(r) ? BuiltInRegistries.ENTITY_TYPE.get(r).getDescriptionId() : null; break;
                default: key = null;
            }
            Language lang = Language.getInstance();
            return key != null && lang.has(key) ? lang.getOrDefault(key) : null;   // server side: vanilla en_us only
        }
    }

    private static <T> Set<String> keys(HolderSet.Named<T> set) {
        Set<String> out = new LinkedHashSet<>();
        set.stream().forEach(h -> h.unwrapKey().ifPresent(k -> out.add(k.location().toString())));
        return out;
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
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            ResourceLocation r = rl(advancementId);
            if (players.isEmpty() || r == null) return -1;
            Advancement adv = server.getAdvancements().getAdvancement(r);
            if (adv == null) return -1;
            long done = players.stream().filter(p -> p.getAdvancements().getOrStartProgress(adv).isDone()).count();
            return (double) done / players.size();
        }

        @Override public boolean isSingleplayer() { return server.isSingleplayer(); }
    }

    // ------------------------------------------------------------------ StateStore

    private final class Store implements StateStore {
        private Path root() {
            return server.getWorldPath(LevelResource.ROOT).resolve("justquests");
        }

        private Path resolve(String name) {
            if (name == null || name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
                throw new IllegalArgumentException("bad file name " + name);
            }
            return root().resolve(name);
        }

        @Override
        public Optional<String> read(String fileName) {
            try {
                Path p = resolve(fileName);
                return Files.isRegularFile(p) ? Optional.of(Files.readString(p, StandardCharsets.UTF_8)) : Optional.empty();
            } catch (IOException | RuntimeException e) {
                JustQuests.LOG.warn("[GenV2] could not read {}: {}", fileName, e.toString());
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
                JustQuests.LOG.error("[GenV2] could not write {}", fileName, e);
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
                JustQuests.LOG.warn("[GenV2] could not list {}: {}", directory, e.toString());
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
            } catch (RuntimeException e) {   // e.g. unknown objective type throws in the dispatch codec
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
        @Override public void error(String msg, Throwable t) {
            if (t == null) JustQuests.LOG.error(msg); else JustQuests.LOG.error(msg, t);
        }
    }
}
