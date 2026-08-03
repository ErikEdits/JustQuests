package com.erikedits.justquests.network;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side cache of the last {@link QuestSyncPayload}. The quest book reads
 * from here instead of the server-only stores, so it works on dedicated servers
 * (and in singleplayer, where the sync also fires over the loopback connection).
 * Plain data holder — no client-only imports, safe to load anywhere.
 */
public final class ClientQuestData {
    private static Map<ResourceLocation, Quest> quests = Collections.emptyMap();
    private static PlayerQuestData progress = new PlayerQuestData();

    private ClientQuestData() {}

    /** Called on the client when a sync packet arrives. */
    public static void accept(String json) {
        Map<ResourceLocation, Quest> q = new LinkedHashMap<>();
        PlayerQuestData p = new PlayerQuestData();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("quests")) {
                for (Map.Entry<String, com.google.gson.JsonElement> e : root.getAsJsonObject("quests").entrySet()) {
                    ResourceLocation id = ResourceLocation.parse(e.getKey());
                    Quest.CODEC.parse(JsonOps.INSTANCE, e.getValue()).result().ifPresent(quest -> q.put(id, quest));
                }
            }
            if (root.has("progress")) {
                p = PlayerQuestData.CODEC.parse(JsonOps.INSTANCE, root.get("progress")).result().orElseGet(PlayerQuestData::new);
            }
        } catch (Exception ex) {
            JustQuests.LOG.error("Failed to parse quest sync", ex);
        }
        quests = q;
        progress = p;
    }

    public static Map<ResourceLocation, Quest> getQuests() { return quests; }

    public static Quest get(ResourceLocation id) { return quests.get(id); }

    public static PlayerQuestData getData() { return progress; }
}
