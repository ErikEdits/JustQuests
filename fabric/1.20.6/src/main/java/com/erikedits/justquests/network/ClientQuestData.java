package com.erikedits.justquests.network;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
    /** Bumped whenever the quest list changes, so an open quest book can rebuild it. */
    private static int version = 0;
    /** Generated quests held under exclusive claims (from GenV2.claimsJson); free ones are absent. */
    private static Map<ResourceLocation, Claim> claims = Collections.emptyMap();
    /** Counts every sync (and clear), so the open book notices progress changes. */
    private static int syncs = 0;
    /** This player's place on the server leaderboard; null in singleplayer. */
    private static Rank rank;
    /** Active quests, most recently accepted first (tracked here on the client, for the HUD). */
    private static final List<ResourceLocation> activeOrder = new ArrayList<>();

    /** A claim: the holder's name ("" if unknown), whether it is this player, whether it is done. */
    public record Claim(String by, boolean mine, boolean completed) {}

    /** Leaderboard place: pos of `of` players (ties share the better place). */
    public record Rank(int pos, int of) {}

    private ClientQuestData() {}

    /** Called on the client when a sync packet arrives. */
    public static void accept(String json) {
        Map<ResourceLocation, Quest> q = new LinkedHashMap<>();
        PlayerQuestData p = new PlayerQuestData();
        Map<ResourceLocation, Claim> c = new LinkedHashMap<>();
        boolean hasQuests = false;
        Rank r = null;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("quests")) {
                hasQuests = true;
                for (Map.Entry<String, com.google.gson.JsonElement> e : root.getAsJsonObject("quests").entrySet()) {
                    ResourceLocation id = new ResourceLocation(e.getKey());
                    Quest.CODEC.parse(JsonOps.INSTANCE, e.getValue()).result().ifPresent(quest -> q.put(id, quest));
                }
            }
            if (root.has("progress")) {
                p = PlayerQuestData.CODEC.parse(JsonOps.INSTANCE, root.get("progress")).result().orElseGet(PlayerQuestData::new);
            }
            if (root.has("claims")) {
                for (Map.Entry<String, com.google.gson.JsonElement> e : root.getAsJsonObject("claims").entrySet()) {
                    JsonObject o = e.getValue().getAsJsonObject();
                    c.put(new ResourceLocation(e.getKey()), new Claim(o.has("by") ? o.get("by").getAsString() : "",
                        o.has("mine") && o.get("mine").getAsBoolean(),
                        o.has("state") && "completed".equals(o.get("state").getAsString())));
                }
            }
            if (root.has("rank")) {
                JsonObject o = root.getAsJsonObject("rank");
                r = new Rank(o.get("pos").getAsInt(), o.get("of").getAsInt());
            }
        } catch (Exception ex) {
            JustQuests.LOG.error("Failed to parse quest sync", ex);
        }
        // A progress-only sync carries no "quests": keep the list we already have.
        if (hasQuests) {
            quests = q;
            version++;
        }
        progress = p;
        claims = c;
        rank = r;
        trackActive(p);
        syncs++;
    }

    /** Newly accepted quests go to the front; finished or dropped ones leave the list. */
    private static void trackActive(PlayerQuestData p) {
        activeOrder.removeIf(id -> !p.active.containsKey(id));
        boolean first = activeOrder.isEmpty();
        for (ResourceLocation id : p.active.keySet()) {
            if (activeOrder.contains(id)) continue;
            if (first) activeOrder.add(id);
            else activeOrder.add(0, id);
        }
    }

    /** Forget the last server's data (on disconnect), so the book never shows stale quests. */
    public static void clear() {
        quests = Collections.emptyMap();
        progress = new PlayerQuestData();
        claims = Collections.emptyMap();
        rank = null;
        activeOrder.clear();
        version++;
        syncs++;
    }

    /** Increments whenever the quest list changes, so the open book knows to refresh. */
    public static int version() { return version; }

    /** Increments on every sync, so the open book can regroup quests by status. */
    public static int syncCount() { return syncs; }

    /** Leaderboard place, or null (singleplayer). */
    public static Rank rank() { return rank; }

    /** Active quest ids, most recently accepted first. */
    public static List<ResourceLocation> activeOrder() { return activeOrder; }

    public static Map<ResourceLocation, Quest> getQuests() { return quests; }

    public static Quest get(ResourceLocation id) { return quests.get(id); }

    public static PlayerQuestData getData() { return progress; }

    /** The claim on a generated quest, or null if it is free (or claims are not exclusive). */
    public static Claim claim(ResourceLocation id) { return claims.get(id); }
}
