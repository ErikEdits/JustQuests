package com.erikedits.justquests.plugin.gen;

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
import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.compat.Compat;
import com.erikedits.justquests.plugin.quest.Quest;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * The generator core's view of a Spigot server: blocks, items, mobs and tags from the registries,
 * the main world's seed and day, the players, and files in plugins/JustQuests/generator/. Every
 * method runs on the server thread and never throws. Spigot has no mods, so only the vanilla
 * catalog is active.
 */
public final class BukkitHost implements GeneratorHost {
    private final JustQuestsPlugin plugin;
    private final Path dir;
    private final Content content = new Content();
    private final ServerWorld world = new ServerWorld();
    private final Store store = new Store();
    private final Validator validator = new Validator();
    private final Caps caps = new Caps();
    private final Log log = new Log();

    public BukkitHost(JustQuestsPlugin plugin, Path dir) {
        this.plugin = plugin;
        this.dir = dir;
    }

    @Override public ContentView content() { return content; }
    @Override public WorldContext world() { return world; }
    @Override public StateStore store() { return store; }
    @Override public QuestValidator validator() { return validator; }
    @Override public HostCapabilities capabilities() { return caps; }
    @Override public GenLog log() { return log; }
    @Override public long currentTimeMillis() { return System.currentTimeMillis(); }

    static World mainWorld() {
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    private static NamespacedKey key(String id) {
        try {
            return id == null ? null : NamespacedKey.fromString(id.toLowerCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Registered and enabled for the main world (experimental content stays out while its feature is off). */
    private static boolean enabled(Material m) {
        World w = mainWorld();
        try {
            return w == null || Compat.enabled(m, w);
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    private static boolean enabled(EntityType t) {
        World w = mainWorld();
        try {
            return w == null || Compat.enabled(t, w);
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    private static <T extends Keyed> Set<String> members(String registry, String tagId, Class<T> type) {
        Set<String> out = new LinkedHashSet<>();
        NamespacedKey k = key(tagId);
        if (k == null) return out;
        try {
            Tag<T> tag = Bukkit.getTag(registry, k, type);
            if (tag != null) {
                for (T t : tag.getValues()) out.add(Compat.key(t).toString());
            }
        } catch (RuntimeException e) {
            // an unknown tag has no members
        }
        return out;
    }

    // ------------------------------------------------------------------ ContentView

    private final class Content implements ContentView {
        @Override public String loaderName() { return "bukkit"; }

        @Override
        public String minecraftVersion() {
            String v = Bukkit.getBukkitVersion();
            return v.contains("-") ? v.substring(0, v.indexOf('-')) : v;
        }

        @Override public boolean isModLoaded(String modId) { return false; }

        @Override
        public boolean itemExists(String id) {
            NamespacedKey k = key(id);
            Material m = k == null ? null : Registry.MATERIAL.get(k);
            return m != null && m.isItem() && enabled(m);
        }

        @Override
        public boolean blockExists(String id) {
            NamespacedKey k = key(id);
            Material m = k == null ? null : Registry.MATERIAL.get(k);
            return m != null && m.isBlock() && enabled(m);
        }

        @Override
        public boolean entityTypeExists(String id) {
            NamespacedKey k = key(id);
            EntityType t = k == null ? null : Registry.ENTITY_TYPE.get(k);
            return t != null && enabled(t);
        }

        @Override
        public boolean dimensionExists(String id) {
            for (World w : Bukkit.getWorlds()) {
                String kind = switch (w.getEnvironment()) {
                    case NETHER -> "minecraft:the_nether";
                    case THE_END -> "minecraft:the_end";
                    case NORMAL -> "minecraft:overworld";
                    default -> "";
                };
                if (kind.equals(id) || w.getKey().toString().equals(id)) return true;
            }
            return false;
        }

        @Override public Set<String> itemTagMembers(String tagId) { return members(Tag.REGISTRY_ITEMS, tagId, Material.class); }

        @Override public Set<String> blockTagMembers(String tagId) { return members(Tag.REGISTRY_BLOCKS, tagId, Material.class); }

        @Override public Set<String> entityTypeTagMembers(String tagId) { return members(Tag.REGISTRY_ENTITY_TYPES, tagId, EntityType.class); }

        @Override public TriState hasCraftingRecipe(String itemId) { return TriState.UNKNOWN; }

        @Override public TriState hasSmeltingRecipe(String outputItemId) { return TriState.UNKNOWN; }

        @Override
        public int maxStackSize(String itemId) {
            NamespacedKey k = key(itemId);
            Material m = k == null ? null : Registry.MATERIAL.get(k);
            return m == null || !m.isItem() ? -1 : m.getMaxStackSize();
        }

        @Override public TriState isBreedableAnimal(String entityTypeId) { return TriState.UNKNOWN; }

        @Override public TriState isTamableAnimal(String entityTypeId) { return TriState.UNKNOWN; }

        @Override public TriState isConsumable(String itemId) { return TriState.UNKNOWN; }

        /** The catalogs carry the English names. */
        @Override public String englishName(ContentKind kind, String id) { return null; }
    }

    // ------------------------------------------------------------------ WorldContext

    /** Share of the given players that have the advancement; -1 if none or the advancement is unknown. */
    static double share(Collection<? extends Player> players, String advancementId) {
        NamespacedKey k = key(advancementId);
        Advancement adv = k == null ? null : Bukkit.getAdvancement(k);
        if (players.isEmpty() || adv == null) return -1;
        long done = 0;
        for (Player p : players) {
            if (p.getAdvancementProgress(adv).isDone()) done++;
        }
        return (double) done / players.size();
    }

    static long gameDay() {
        World w = mainWorld();
        return w == null ? 0 : Math.max(0, w.getFullTime() / 24000L);
    }

    private final class ServerWorld implements WorldContext {
        @Override
        public long worldSeed() {
            World w = mainWorld();
            return w == null ? 0 : w.getSeed();
        }

        @Override public long gameDay() { return BukkitHost.gameDay(); }

        @Override public int onlinePlayerCount() { return Bukkit.getOnlinePlayers().size(); }

        @Override public int knownPlayerCount() { return Math.max(onlinePlayerCount(), plugin.store().all().size()); }

        @Override
        public double onlineShareWithAdvancement(String advancementId) {
            try {
                return share(Bukkit.getOnlinePlayers(), advancementId);
            } catch (RuntimeException e) {
                return -1;
            }
        }

        @Override public boolean isSingleplayer() { return false; }
    }

    // ------------------------------------------------------------------ StateStore

    private final class Store implements StateStore {
        private Path resolve(String name) {
            if (name == null || name.contains("..") || name.startsWith("/") || name.startsWith("\\") || name.contains(":")) {
                throw new IllegalArgumentException("bad file name " + name);
            }
            return dir.resolve(name);
        }

        @Override
        public Optional<String> read(String fileName) {
            try {
                Path p = resolve(fileName);
                return Files.isRegularFile(p) ? Optional.of(Files.readString(p, StandardCharsets.UTF_8)) : Optional.empty();
            } catch (IOException | RuntimeException e) {
                plugin.getLogger().warning("[Generator] could not read " + fileName + ": " + e);
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
                plugin.getLogger().log(Level.SEVERE, "[Generator] could not write " + fileName, e);
            }
        }

        @Override
        public List<String> list(String directory) {
            List<String> out = new ArrayList<>();
            try {
                Path d = resolve(directory);
                if (Files.isDirectory(d)) {
                    try (Stream<Path> s = Files.list(d)) {
                        s.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).sorted().forEach(out::add);
                    }
                }
            } catch (IOException | RuntimeException e) {
                plugin.getLogger().warning("[Generator] could not list " + directory + ": " + e);
            }
            return out;
        }
    }

    // ------------------------------------------------------------------ QuestValidator

    /** Runs the plugin's own quest reader, so every generated quest is one the plugin can play. */
    private static final class Validator implements QuestValidator {
        @Override
        public ValidationResult validate(String questId, JsonObject questJson) {
            try {
                Quest.parse(questId, questJson);
                return ValidationResult.OK;
            } catch (RuntimeException e) {
                return ValidationResult.fail(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ HostCapabilities

    private static final class Caps implements HostCapabilities {
        private static final Set<String> TAGGABLE = Set.of("justquests:collect_item", "justquests:craft_item",
            "justquests:smelt_item", "justquests:consume_item", "justquests:enchant_item", "justquests:use_item",
            "justquests:mine_block", "justquests:place_block", "justquests:kill_mob", "justquests:tame_animal",
            "justquests:breed_animal");
        private static final Set<String> OBJECTIVES = Set.of("justquests:collect_item", "justquests:kill_mob",
            "justquests:place_block", "justquests:craft_item", "justquests:tame_animal", "justquests:gain_advancement",
            "justquests:visit_dimension", "justquests:reach_level", "justquests:reach_location", "justquests:mine_block",
            "justquests:breed_animal", "justquests:consume_item", "justquests:smelt_item", "justquests:enchant_item",
            "justquests:use_item");
        private static final Set<String> REWARDS = Set.of("justquests:give_item", "justquests:xp", "justquests:effect",
            "justquests:loot_table", "justquests:message", "justquests:command", "justquests:choice");

        @Override public boolean supportsTag(String objectiveType) { return TAGGABLE.contains(objectiveType); }

        @Override public Set<String> objectiveTypes() { return OBJECTIVES; }

        @Override public Set<String> rewardTypes() { return REWARDS; }
    }

    // ------------------------------------------------------------------ GenLog

    private final class Log implements GenLog {
        @Override public void info(String msg) { plugin.getLogger().info(msg); }

        @Override public void warn(String msg) { plugin.getLogger().warning(msg); }

        @Override
        public void error(String msg, Throwable t) {
            if (t == null) plugin.getLogger().severe(msg);
            else plugin.getLogger().log(Level.SEVERE, msg, t);
        }
    }
}
