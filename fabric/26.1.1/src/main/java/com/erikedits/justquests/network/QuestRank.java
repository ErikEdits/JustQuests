package com.erikedits.justquests.network;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.UUID;

/** The player's place on the server leaderboard (by completed quests), for the quest book's stats page. */
public final class QuestRank {
    private QuestRank() {}

    /** {"pos": 2, "of": 6}, or null while nobody else has quest data (singleplayer). */
    public static JsonObject json(UUID player) {
        WorldQuestStore store = WorldQuestStore.get();
        if (store == null) return null;
        Map<UUID, PlayerQuestData> all = store.allPlayers();
        PlayerQuestData me = all.get(player);
        int others = all.size() - (me != null ? 1 : 0);
        if (others < 1) return null;
        int mine = me != null ? me.completed.size() : 0;
        int pos = 1;
        for (Map.Entry<UUID, PlayerQuestData> e : all.entrySet()) {
            if (!e.getKey().equals(player) && e.getValue().completed.size() > mine) pos++;
        }
        JsonObject o = new JsonObject();
        o.addProperty("pos", pos);
        o.addProperty("of", others + 1);
        return o;
    }
}
