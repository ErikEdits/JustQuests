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
                loaded.put(id, quest);
            }
        });
        this.datapackQuests = loaded;
        JustQuests.LOG.info("Loaded {} quests", loaded.size());
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
        if (com.erikedits.justquests.storage.WorldSettings.mainQuests()) {
            merged.putAll(datapackQuests);
        }
        merged.putAll(customQuests);   // custom overrides datapack on a shared id
        merged.putAll(generatedQuests);
        return Collections.unmodifiableMap(merged);
    }

    public Quest get(Identifier id) {
        Quest custom = customQuests.get(id);
        if (custom != null) return custom;
        if (com.erikedits.justquests.storage.WorldSettings.mainQuests()) {
            Quest datapack = datapackQuests.get(id);
            if (datapack != null) return datapack;
        }
        return generatedQuests.get(id);
    }
}
