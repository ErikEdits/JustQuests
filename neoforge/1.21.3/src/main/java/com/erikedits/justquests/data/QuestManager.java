package com.erikedits.justquests.data;

import com.erikedits.justquests.JustQuests;
import net.minecraft.resources.ResourceLocation;
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
    private Map<ResourceLocation, Quest> datapackQuests = new HashMap<>();
    /** Quests from the per-world custom file. Override datapack on id clash (Q54). */
    private Map<ResourceLocation, Quest> customQuests = new HashMap<>();

    private QuestManager() {
        super(Quest.CODEC, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, Quest> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, Quest> loaded = new HashMap<>();
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
    public void setCustomQuests(Map<ResourceLocation, Quest> custom) {
        this.customQuests = custom;
    }

    /** All quests, custom overriding datapack on a shared id. */
    public Map<ResourceLocation, Quest> getQuests() {
        if (customQuests.isEmpty()) {
            return Collections.unmodifiableMap(datapackQuests);
        }
        Map<ResourceLocation, Quest> merged = new HashMap<>(datapackQuests);
        merged.putAll(customQuests);
        return Collections.unmodifiableMap(merged);
    }

    public Quest get(ResourceLocation id) {
        Quest custom = customQuests.get(id);
        return custom != null ? custom : datapackQuests.get(id);
    }
}
