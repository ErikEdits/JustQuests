package com.erikedits.justquests.data;

import com.erikedits.justquests.JustQuests;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QuestManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "justquests/quests";
    // Must come after GSON: static initializers run in declaration order,
    // and the constructor passes GSON to super().
    public static final QuestManager INSTANCE = new QuestManager();

    private Map<ResourceLocation, Quest> quests = new HashMap<>();

    private QuestManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, Quest> loaded = new HashMap<>();
        map.forEach((id, json) -> {
            // Per-quest try/catch so one broken quest (e.g. an unknown
            // objective/reward type, which the dispatch codec throws on)
            // never aborts loading of all the others.
            try {
                Quest.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> JustQuests.LOG.error("Failed to parse quest {}: {}", id, error))
                    .ifPresent(quest -> {
                        if (quest.objectives().isEmpty()) {
                            JustQuests.LOG.warn("Quest {} has no objectives - skipping", id);
                        } else if (quest.objectives().stream().anyMatch(o -> o.requiredCount() <= 0)) {
                            JustQuests.LOG.warn("Quest {} has an objective with count <= 0 - skipping", id);
                        } else {
                            loaded.put(id, quest);
                        }
                    });
            } catch (Exception e) {
                JustQuests.LOG.error("Failed to load quest {} (invalid type or data): {}", id, e.getMessage());
            }
        });
        this.quests = loaded;
        JustQuests.LOG.info("Loaded {} quests", loaded.size());
    }

    public Map<ResourceLocation, Quest> getQuests() {
        return Collections.unmodifiableMap(quests);
    }

    public Quest get(ResourceLocation id) {
        return quests.get(id);
    }
}
