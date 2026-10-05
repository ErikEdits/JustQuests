package com.erikedits.justquests.plugin.gen;

import com.erikedits.justquests.generator.v2.QuestGeneratorV2;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratedQuest;
import com.erikedits.justquests.generator.v2.api.SetRequest;
import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The weekly quests: one set for everyone, new each week, a difficulty step harder and about four
 * times longer than the board's quests, with valuable loot on top. Each player can do each quest
 * once. Unfinished quests end with the week; finished ones whose rewards wait stay until claimed.
 */
final class WeeklyQuests {
    static final String PREFIX = "justquests:weekly/";
    /** How much longer than a board quest of the same difficulty. */
    static final double MINUTES = 4.0;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final JustQuestsPlugin plugin;
    private final Path file;
    private long week = -1;
    private final Map<String, JsonObject> quests = new LinkedHashMap<>();
    /** "week|signature" of the last four weeks. */
    private final List<String> history = new ArrayList<>();

    WeeklyQuests(JustQuestsPlugin plugin, Path file) {
        this.plugin = plugin;
        this.file = file;
    }

    long currentWeek() {
        return GenTime.week(plugin.settings().weeklyDay, plugin.settings().weeklyHour);
    }

    long week() {
        return week;
    }

    /** Renews the set when a new week began; true if it changed. Switched off, nothing is renewed. */
    boolean rollIfDue(QuestGeneratorV2 gen, Difficulty base, PluginGenerator owner) {
        if (!plugin.settings().weekly) return false;
        long now = currentWeek();
        if (now == week) return false;
        Set<String> waiting = waiting();
        List<String> gone = new ArrayList<>();
        for (String id : new ArrayList<>(quests.keySet())) {
            if (!waiting.contains(id) && !id.startsWith(PREFIX + now + "_")) {
                quests.remove(id);
                gone.add(id);
            }
        }
        owner.dropFromPlayers(gone);
        if (now != week) {
            history.removeIf(h -> {
                int bar = h.indexOf('|');
                try {
                    return bar < 0 || Long.parseLong(h.substring(0, bar)) < now - 28;
                } catch (NumberFormatException e) {
                    return true;
                }
            });
            Set<String> avoid = new LinkedHashSet<>();
            for (String h : history) avoid.add(h.substring(h.indexOf('|') + 1));
            Difficulty d = base == Difficulty.EASY ? Difficulty.NORMAL : Difficulty.HARD;
            long seed = GenTime.mix(BukkitHost.mainWorld() == null ? 0 : BukkitHost.mainWorld().getSeed(), now);
            List<GeneratedQuest> made = gen.generateSet(new SetRequest(seed, plugin.settings().weeklyQuests, d, MINUTES,
                1.0, false, true, null, avoid, Set.of()));
            int i = 0;
            for (GeneratedQuest q : made) {
                quests.put(PREFIX + now + "_" + i++, owner.augment(q.json(), q.rewardValue(), "weekly", loot(d), false));
                history.add(now + "|" + q.signature());
            }
            week = now;
            plugin.getLogger().info("[Generator] " + made.size() + " weekly quest(s) for the week of day " + now);
        }
        save();
        return true;
    }

    /** Valuable loot on top of a weekly quest: enchanted books and gear on the harder steps. */
    private static String loot(Difficulty d) {
        return switch (d) {
            case EASY -> "minecraft:chests/simple_dungeon";
            case NORMAL -> "minecraft:chests/buried_treasure";
            case HARD -> "minecraft:chests/stronghold_library";
        };
    }

    /**
     * The week's quests, for the quest registry. Switched off, only finished quests whose rewards
     * still wait stay; switched on again the same week, the rest come back.
     */
    Map<String, JsonObject> all() {
        if (plugin.settings().weekly) return quests;
        Set<String> waiting = waiting();
        Map<String, JsonObject> out = new LinkedHashMap<>();
        quests.forEach((id, json) -> {
            if (waiting.contains(id)) out.put(id, json);
        });
        return out;
    }

    /** Quest ids whose rewards some player still has to claim. */
    private Set<String> waiting() {
        Set<String> waiting = new LinkedHashSet<>();
        for (PlayerData d : plugin.store().all().values()) waiting.addAll(d.pendingClaim.keySet());
        return waiting;
    }

    boolean isWeekly(String id) {
        return id.startsWith(PREFIX);
    }

    void load() {
        quests.clear();
        history.clear();
        try {
            if (!Files.exists(file)) return;
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            week = root.has("week") ? root.get("week").getAsLong() : -1;
            if (root.has("quests")) {
                for (Map.Entry<String, JsonElement> q : root.getAsJsonObject("quests").entrySet()) {
                    quests.put(q.getKey(), q.getValue().getAsJsonObject());
                }
            }
            if (root.has("history")) {
                for (JsonElement h : root.getAsJsonArray("history")) history.add(h.getAsString());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Generator] could not read " + file.getFileName() + ": " + e.getMessage());
        }
    }

    void save() {
        JsonObject root = new JsonObject();
        root.addProperty("week", week);
        JsonObject qs = new JsonObject();
        quests.forEach(qs::add);
        root.add("quests", qs);
        JsonArray h = new JsonArray();
        history.forEach(h::add);
        root.add("history", h);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root));
        } catch (Exception e) {
            plugin.getLogger().warning("[Generator] could not save " + file.getFileName() + ": " + e.getMessage());
        }
    }
}
