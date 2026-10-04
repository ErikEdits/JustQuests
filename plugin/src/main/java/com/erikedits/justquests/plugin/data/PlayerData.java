package com.erikedits.justquests.plugin.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One player's quests, in the same JSON shape as an entry of the mod's progress.json
 * (active / pendingClaim / completed), plus the plugin's boss bar settings.
 */
public final class PlayerData {
    public String teamId;
    /** quest id -> objective index -> progress */
    public final Map<String, Map<Integer, Integer>> active = new LinkedHashMap<>();
    /** finished quests whose rewards wait to be claimed: quest id -> completion time */
    public final Map<String, Long> pendingClaim = new LinkedHashMap<>();
    /** quest id -> last completion time (epoch millis) */
    public final Map<String, Long> completed = new LinkedHashMap<>();
    /** quest shown in the boss bar, or null */
    public String pinned;
    public boolean bossbar = true;

    public boolean isActive(String id) {
        return active.containsKey(id);
    }

    public boolean isCompleted(String id) {
        return completed.containsKey(id);
    }

    public boolean isClaimable(String id) {
        return pendingClaim.containsKey(id);
    }

    public int progress(String id, int objective) {
        Map<Integer, Integer> p = active.get(id);
        return p == null ? 0 : p.getOrDefault(objective, 0);
    }

    public void accept(String id) {
        active.putIfAbsent(id, new HashMap<>());
    }

    public void abandon(String id) {
        active.remove(id);
    }

    /** Marks a quest finished: removes it from active and records the time. */
    public void complete(String id) {
        active.remove(id);
        completed.put(id, System.currentTimeMillis());
    }

    public boolean isEmpty() {
        return active.isEmpty() && pendingClaim.isEmpty() && completed.isEmpty() && pinned == null && bossbar;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        if (teamId != null) o.addProperty("teamId", teamId);
        JsonObject act = new JsonObject();
        active.forEach((id, prog) -> {
            JsonObject p = new JsonObject();
            prog.forEach((i, n) -> p.addProperty(String.valueOf(i), n));
            act.add(id, p);
        });
        o.add("active", act);
        JsonObject pend = new JsonObject();
        pendingClaim.forEach(pend::addProperty);
        o.add("pendingClaim", pend);
        JsonObject done = new JsonObject();
        completed.forEach(done::addProperty);
        o.add("completed", done);
        if (pinned != null) o.addProperty("pinned", pinned);
        if (!bossbar) o.addProperty("bossbar", false);
        return o;
    }

    public static PlayerData fromJson(JsonObject o) {
        PlayerData d = new PlayerData();
        if (o.has("teamId")) d.teamId = o.get("teamId").getAsString();
        if (o.has("active")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("active").entrySet()) {
                Map<Integer, Integer> prog = new HashMap<>();
                if (e.getValue().isJsonObject()) {
                    for (Map.Entry<String, JsonElement> p : e.getValue().getAsJsonObject().entrySet()) {
                        try {
                            prog.put(Integer.parseInt(p.getKey()), p.getValue().getAsInt());
                        } catch (NumberFormatException ignored) {
                            // skip invalid keys
                        }
                    }
                }
                d.active.put(e.getKey(), prog);
            }
        }
        if (o.has("pendingClaim")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("pendingClaim").entrySet()) d.pendingClaim.put(e.getKey(), e.getValue().getAsLong());
        }
        if (o.has("completed")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("completed").entrySet()) d.completed.put(e.getKey(), e.getValue().getAsLong());
        }
        if (o.has("pinned")) d.pinned = o.get("pinned").getAsString();
        if (o.has("bossbar")) d.bossbar = o.get("bossbar").getAsBoolean();
        return d;
    }
}
