package com.erikedits.justquests.data;

import com.erikedits.justquests.JustQuests;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 1.21.2 variant: {@link SimpleJsonResourceReloadListener} is now codec-based
 * and decodes each file itself (logging and skipping bad ones), so this class
 * receives already-parsed {@link Quest} objects and only applies the
 * empty-objective / non-positive-count guards.
 */
public class QuestManager extends SimpleJsonResourceReloadListener<Quest> {
    private static final String DIRECTORY = "justquests/quests";
    // Must come after the codec constant in declaration order.
    public static final QuestManager INSTANCE = new QuestManager();

    /** Quests from datapacks (reloaded on /reload). */
    private Map<Identifier, Quest> datapackQuests = new HashMap<>();
    /** Quests from the per-world custom file. Override datapack on id clash (Q54). */
    private Map<Identifier, Quest> customQuests = new HashMap<>();
    /** Procedurally generated quests (Phase 6), rotated per world; own id namespace. */
    private Map<Identifier, Quest> generatedQuests = new HashMap<>();

    private QuestManager() {
        super(Quest.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, Quest> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, Quest> loaded = new HashMap<>();
        map.forEach((id, quest) -> {
            if (quest.objectives().isEmpty()) {
                JustQuests.LOG.warn("Quest {} has no objectives - skipping", id);
            } else if (quest.objectives().stream().anyMatch(o -> o.requiredCount() <= 0)) {
                JustQuests.LOG.warn("Quest {} has an objective with count <= 0 - skipping", id);
            } else {
                loaded.put(id, quest.withFolderCategory(id.getPath()));
            }
        });
        this.datapackQuests = loaded;
        JustQuests.LOG.info("Loaded {} quests", loaded.size());
    }

    /** The quests bundled with the mod (data/justquests/justquests/quests/): the ones mainQuests hides. */
    private static final java.util.Set<String> BUILT_IN = java.util.Set.of(
        "achiever", "animal_friend", "cattle_rancher", "craftsman", "daily_bread", "deep_down",
        "ender_seeker", "first_steps", "fuel_for_the_fire", "green_thumb", "hearty_meal",
        "home_builder", "hot_stuff", "into_the_nether", "iron_harvest", "level_up", "lumberjack",
        "master_miner", "monster_hunter", "prospector", "seasoned_miner", "shine_bright", "slayer",
        "stone_age", "the_smelter");

    private static boolean builtIn(Identifier id) {
        return "justquests".equals(id.getNamespace()) && BUILT_IN.contains(id.getPath());
    }

    /** Replaces the custom (world-file) quests. Precedence: custom > datapack. */
    public void setCustomQuests(Map<Identifier, Quest> custom) {
        this.customQuests = custom;
    }

    /** Replaces the generated (rotating) quests. They use their own id namespace. */
    public void setGeneratedQuests(Map<Identifier, Quest> generated) {
        this.generatedQuests = generated;
    }

    /** All quests, custom overriding datapack on a shared id. */
    public Map<Identifier, Quest> getQuests() {
        Map<Identifier, Quest> merged = new HashMap<>();
        boolean main = com.erikedits.justquests.storage.WorldSettings.mainQuests();
        datapackQuests.forEach((id, q) -> { if (main || !builtIn(id)) merged.put(id, q); });
        merged.putAll(customQuests);   // custom overrides datapack on a shared id
        merged.putAll(generatedQuests);
        return Collections.unmodifiableMap(merged);
    }

    public Quest get(Identifier id) {
        Quest custom = customQuests.get(id);
        if (custom != null) return custom;
        if (com.erikedits.justquests.storage.WorldSettings.mainQuests() || !builtIn(id)) {
            Quest datapack = datapackQuests.get(id);
            if (datapack != null) return datapack;
        }
        return generatedQuests.get(id);
    }
}
