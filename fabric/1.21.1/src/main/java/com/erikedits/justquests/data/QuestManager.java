package com.erikedits.justquests.data;

import com.erikedits.justquests.JustQuests;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// Fabric requires reload listeners to carry an id (IdentifiableResourceReloadListener);
// the reload(...) impl is inherited from SimpleJsonResourceReloadListener.
public class QuestManager extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "justquests/quests";
    // Must come after GSON: static initializers run in declaration order,
    // and the constructor passes GSON to super().
    public static final QuestManager INSTANCE = new QuestManager();

    /** Quests from datapacks (reloaded on /reload). */
    private Map<ResourceLocation, Quest> datapackQuests = new HashMap<>();
    /** Quests from the per-world custom file. Override datapack on id clash (Q54). */
    private Map<ResourceLocation, Quest> customQuests = new HashMap<>();

    private QuestManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(JustQuests.MOD_ID, "quests");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, Quest> loaded = new HashMap<>();
        map.forEach((id, json) -> {
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
