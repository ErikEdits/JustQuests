package com.erikedits.justquests.generator.v2.internal.state;

import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Claim bookkeeping of one quest. Holders are players who currently have the quest active;
 * completions are players who finished it; awaiting are finishers whose rewards still wait to be
 * claimed. With exclusive claims there is at most one holder (legacy data may carry two) and one
 * completion.
 */
public final class ClaimRecord {
    public final LinkedHashMap<UUID, Long> holders = new LinkedHashMap<>();
    public final LinkedHashMap<UUID, Long> completions = new LinkedHashMap<>();
    public final LinkedHashMap<UUID, Long> awaiting = new LinkedHashMap<>();

    public boolean isHeldBy(UUID p) {
        return holders.containsKey(p);
    }

    /** Someone still needs the quest: a holder, or a finisher whose rewards wait. Such quests outlive their cycle. */
    public boolean isNeeded() {
        return !holders.isEmpty() || !awaiting.isEmpty();
    }

    public UUID firstHolder() {
        return holders.isEmpty() ? null : holders.keySet().iterator().next();
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonObject h = new JsonObject();
        holders.forEach((k, v) -> h.addProperty(k.toString(), v));
        o.add("holders", h);
        JsonObject c = new JsonObject();
        completions.forEach((k, v) -> c.addProperty(k.toString(), v));
        o.add("completions", c);
        if (!awaiting.isEmpty()) {
            JsonObject a = new JsonObject();
            awaiting.forEach((k, v) -> a.addProperty(k.toString(), v));
            o.add("awaiting", a);
        }
        return o;
    }

    public static ClaimRecord fromJson(JsonObject o) {
        ClaimRecord r = new ClaimRecord();
        read(Json.obj(o, "holders"), r.holders);
        read(Json.obj(o, "completions"), r.completions);
        read(Json.obj(o, "awaiting"), r.awaiting);
        return r;
    }

    private static void read(JsonObject o, Map<UUID, Long> into) {
        if (o == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            try {
                UUID id = UUID.fromString(e.getKey());
                long t = e.getValue().isJsonPrimitive() ? e.getValue().getAsLong() : 0L;
                into.put(id, t);
            } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
                // skip malformed entries
            }
        }
    }
}
