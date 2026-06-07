package com.erikedits.justquests.data;

import com.erikedits.justquests.JustQuests;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class QuestManager extends SimpleJsonResourceReloadListener {
    public static final QuestManager INSTANCE = new QuestManager();
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "justquests/quests";

    private Map<ResourceLocation, Quest> quests = new HashMap<>();

    private QuestManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, Quest> loaded = new HashMap<>();
        map.forEach((id, json) -> Quest.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(error -> JustQuests.LOG.error("Failed to parse quest {}: {}", id, error))
            .ifPresent(quest -> loaded.put(id, quest)));
        this.quests = loaded;
        JustQuests.LOG.info("Loaded {} quests", loaded.size());
    }

    public Map<ResourceLocation, Quest> getQuests() {
        return quests;
    }

    public Quest get(ResourceLocation id) {
        return quests.get(id);
    }
}
