package com.erikedits.justquests.plugin.gen;

import com.erikedits.justquests.generator.v2.QuestGeneratorV2;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratedQuest;
import com.erikedits.justquests.generator.v2.api.SetRequest;
import com.erikedits.justquests.generator.v2.api.WorldContext;
import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every player's own daily quests: generated when the day turns (or on the player's first visit that
 * day) from a seed of world, player and day, sized to the player's own progress (the Nether only
 * once they have been there), never repeating the last week's. Only their owner sees them. A
 * finished quest whose rewards still wait stays until they are claimed.
 *
 * <p>A streak counts the days in a row with at least one personal quest done; new personal quests
 * pay {@code streakBonus} more per streak day, up to {@code streakMax}.
 */
final class PersonalQuests {
    static final String PREFIX = "justquests:personal/";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** One player's personal quests. */
    static final class Entry {
        long day = -1;
        final Map<String, JsonObject> quests = new LinkedHashMap<>();
        /** "day|signature" of the last week's quests. */
        final List<String> history = new ArrayList<>();
        int streak;
        long lastDoneDay = -1;
    }

    private final JustQuestsPlugin plugin;
    private final Path file;
    private final Map<UUID, Entry> entries = new HashMap<>();
    private final Map<String, UUID> owners = new HashMap<>();
    private boolean dirty;

    PersonalQuests(JustQuestsPlugin plugin, Path file) {
        this.plugin = plugin;
        this.file = file;
    }

    long today() {
        return GenTime.day(plugin.settings().personalResetHour);
    }

    /** Makes sure the player has today's quests; true if they were (re)generated. */
    boolean ensure(Player player, QuestGeneratorV2 gen, Difficulty difficulty, PluginGenerator owner) {
        if (!plugin.settings().personal) return false;
        Entry e = entries.computeIfAbsent(player.getUniqueId(), k -> new Entry());
        long today = today();
        if (e.day == today) return false;
        PlayerData data = plugin.store().get(player.getUniqueId());
        // yesterday's quests go, except finished ones whose rewards still wait
        for (String id : new ArrayList<>(e.quests.keySet())) {
            if (data.isClaimable(id)) continue;
            e.quests.remove(id);
            owners.remove(id);
            if (data.isActive(id)) {
                data.abandon(id);
                if (id.equals(data.pinned)) data.pinned = null;
                plugin.store().markDirty(player.getUniqueId());
            }
        }
        e.history.removeIf(h -> {
            int bar = h.indexOf('|');
            try {
                return bar < 0 || Long.parseLong(h.substring(0, bar)) < today - 7;
            } catch (NumberFormatException ex) {
                return true;
            }
        });
        Set<String> history = new LinkedHashSet<>();
        for (String h : e.history) history.add(h.substring(h.indexOf('|') + 1));
        double bonus = 1.0 + Math.min(plugin.settings().streakMax, streak(player.getUniqueId()) * plugin.settings().streakBonus);
        long seed = GenTime.mix(BukkitHost.mainWorld() == null ? 0 : BukkitHost.mainWorld().getSeed(), player.getUniqueId(), today);
        List<GeneratedQuest> made = gen.generateSet(new SetRequest(seed, plugin.settings().personalQuests, difficulty, 1.0,
            bonus, false, false, new PlayerWorld(player), history, Set.of()));
        String owner8 = player.getUniqueId().toString().replace("-", "").substring(0, 8);
        int i = 0;
        for (GeneratedQuest q : made) {
            String id = PREFIX + owner8 + "/" + today + "_" + i++;
            e.quests.put(id, owner.augment(q.json(), q.rewardValue(), "personal", null, false));
            owners.put(id, player.getUniqueId());
            e.history.add(today + "|" + q.signature());
        }
        e.day = today;
        dirty = true;
        return true;
    }

    /**
     * All personal quests of every player, for the quest registry. Switched off, only finished
     * quests whose rewards still wait stay; switched on again the same day, the rest come back.
     */
    Map<String, JsonObject> all() {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        boolean on = plugin.settings().personal;
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            PlayerData data = on ? null : plugin.store().peek(e.getKey());
            e.getValue().quests.forEach((id, json) -> {
                if (on || (data != null && data.isClaimable(id))) out.put(id, json);
            });
        }
        return out;
    }

    boolean isPersonal(String id) {
        return id.startsWith(PREFIX);
    }

    UUID owner(String id) {
        return owners.get(id);
    }

    /** A personal quest was finished: the streak grows once per day. */
    void completed(UUID player, String id) {
        Entry e = entries.get(player);
        if (e == null || !e.quests.containsKey(id)) return;
        long today = today();
        if (e.lastDoneDay == today) return;
        e.streak = e.lastDoneDay == today - 1 ? e.streak + 1 : 1;
        e.lastDoneDay = today;
        dirty = true;
    }

    /** Days in a row with a personal quest done (0 once a day was missed). */
    int streak(UUID player) {
        Entry e = entries.get(player);
        if (e == null || e.lastDoneDay < today() - 1) return 0;
        return e.streak;
    }

    // --- persistence -------------------------------------------------------------------------

    void load() {
        entries.clear();
        owners.clear();
        try {
            if (!Files.exists(file)) return;
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> p : root.entrySet()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(p.getKey());
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                JsonObject o = p.getValue().getAsJsonObject();
                Entry e = new Entry();
                e.day = o.has("day") ? o.get("day").getAsLong() : -1;
                e.streak = o.has("streak") ? o.get("streak").getAsInt() : 0;
                e.lastDoneDay = o.has("lastDoneDay") ? o.get("lastDoneDay").getAsLong() : -1;
                if (o.has("quests")) {
                    for (Map.Entry<String, JsonElement> q : o.getAsJsonObject("quests").entrySet()) {
                        e.quests.put(q.getKey(), q.getValue().getAsJsonObject());
                        owners.put(q.getKey(), uuid);
                    }
                }
                if (o.has("history")) {
                    for (JsonElement h : o.getAsJsonArray("history")) e.history.add(h.getAsString());
                }
                entries.put(uuid, e);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[Generator] could not read " + file.getFileName() + ": " + ex.getMessage());
        }
    }

    void saveIfDirty() {
        if (!dirty) return;
        JsonObject root = new JsonObject();
        entries.forEach((uuid, e) -> {
            JsonObject o = new JsonObject();
            o.addProperty("day", e.day);
            o.addProperty("streak", e.streak);
            o.addProperty("lastDoneDay", e.lastDoneDay);
            JsonObject qs = new JsonObject();
            e.quests.forEach(qs::add);
            o.add("quests", qs);
            JsonArray h = new JsonArray();
            e.history.forEach(h::add);
            o.add("history", h);
            root.add(uuid.toString(), o);
        });
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root));
            dirty = false;
        } catch (Exception ex) {
            plugin.getLogger().warning("[Generator] could not save " + file.getFileName() + ": " + ex.getMessage());
        }
    }

    /** One player's progress as the generator sees it: only their own advancements unlock the Nether and the End. */
    private static final class PlayerWorld implements WorldContext {
        private final Player player;

        PlayerWorld(Player player) {
            this.player = player;
        }

        @Override
        public long worldSeed() {
            return BukkitHost.mainWorld() == null ? 0 : BukkitHost.mainWorld().getSeed();
        }

        @Override public long gameDay() { return BukkitHost.gameDay(); }

        @Override public int onlinePlayerCount() { return 1; }

        @Override public int knownPlayerCount() { return 1; }

        @Override
        public double onlineShareWithAdvancement(String advancementId) {
            try {
                return BukkitHost.share(List.of(player), advancementId);
            } catch (RuntimeException e) {
                return -1;
            }
        }

        @Override public boolean isSingleplayer() { return false; }
    }
}
