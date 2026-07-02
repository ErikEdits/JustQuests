package com.erikedits.justquests.storage;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads player/server-authored quests from a per-world file
 * (`<world>/justquests/custom-quests.json`, Q31). Custom quests override
 * datapack quests on a shared id (Q54). The file is watched and reloaded
 * automatically when it changes (Q32). A template is written on first run.
 */
public final class CustomQuestLoader {
    private static Path file;
    private static long lastModified = -1L;

    private CustomQuestLoader() {}

    public static void init(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("justquests").resolve("custom-quests.json");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, TEMPLATE);
            }
        } catch (Exception e) {
            JustQuests.LOG.error("Could not create custom-quests.json template", e);
        }
        load();
    }

    /** Called periodically; reloads only when the file changed (auto-reload). */
    public static void tickCheck() {
        if (file == null) return;
        try {
            long m = Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : -1L;
            if (m != lastModified) {
                load();
            }
        } catch (Exception ignored) {
            // transient FS error; try again next tick
        }
    }

    /** Parses the custom file and pushes the result into the QuestManager. */
    public static void load() {
        Map<ResourceLocation, Quest> custom = new HashMap<>();
        if (file != null && Files.exists(file)) {
            try {
                lastModified = Files.getLastModifiedTime(file).toMillis();
                JsonElement root = JsonParser.parseString(Files.readString(file));
                if (root.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet()) {
                        String key = e.getKey();
                        if (key.startsWith("_")) continue;          // _help etc.
                        if (!e.getValue().isJsonObject()) continue;
                        JsonObject obj = e.getValue().getAsJsonObject();
                        // blank template slots (no objectives) are skipped silently
                        if (!obj.has("objectives") || obj.getAsJsonArray("objectives").isEmpty()) continue;
                        try {
                            ResourceLocation id = key.contains(":")
                                ? new ResourceLocation(key)
                                : new ResourceLocation("justquests", key);
                            Quest.CODEC.parse(JsonOps.INSTANCE, obj)
                                .resultOrPartial(err -> JustQuests.LOG.error("Custom quest {} invalid: {}", key, err))
                                .ifPresent(q -> custom.put(id, q));
                        } catch (Exception ex) {
                            JustQuests.LOG.error("Custom quest {} failed: {}", key, ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                JustQuests.LOG.error("Could not read custom-quests.json", e);
            }
        }
        QuestManager.INSTANCE.setCustomQuests(custom);
        JustQuests.LOG.info("Loaded {} custom quest(s)", custom.size());
    }

    public static void clear() {
        file = null;
        lastModified = -1L;
        QuestManager.INSTANCE.setCustomQuests(new HashMap<>());
    }

    private static final String TEMPLATE = """
        {
          "_help": "Add your own quests here. Each key is the quest id (a bare name becomes justquests:<name>, or use a full namespace:path). Keys starting with _ are ignored. Slots with no objectives are skipped, so you can leave blank slots. Custom quests override datapack quests with the same id. The file reloads automatically when you save it. Objective types: collect_item, kill_mob, place_block, mine_block, craft_item, smelt_item, consume_item, tame_animal, breed_animal, gain_advancement, visit_dimension, reach_level, reach_location. Reward types: give_item, command, loot_table, xp, effect, message. A quest may also set requires (a list of quest ids that must be completed first), repeatable (true/false) and cooldown_hours (wait time before a repeatable quest can be taken again). Item fields accept an id or a #tag. title and description can be a plain string OR a per-language map like {\\"en_us\\": \\"...\\", \\"de_de\\": \\"...\\"} - each player sees their own language, with English as fallback. Item, mob and block names in the goal line are translated automatically. See https://github.com/ErikEdits/JustQuests",
          "example_quest": {
            "title": "My First Custom Quest",
            "description": "Gather 10 dirt, get a diamond.",
            "category": "custom",
            "objectives": [
              { "type": "justquests:collect_item", "item": "minecraft:dirt", "count": 10 }
            ],
            "rewards": [
              { "type": "justquests:give_item", "item": "minecraft:diamond", "count": 1 }
            ]
          },
          "example_multilang": {
            "title": { "en_us": "Mining Trip", "de_de": "Bergbau-Ausflug" },
            "description": { "en_us": "Mine 20 iron ore.", "de_de": "Baue 20 Eisenerz ab." },
            "category": "custom",
            "objectives": [
              { "type": "justquests:collect_item", "item": "minecraft:raw_iron", "count": 20 }
            ],
            "rewards": [
              { "type": "justquests:give_item", "item": "minecraft:iron_block", "count": 2 }
            ]
          },
          "blank_slot_1": { "title": "", "description": "", "category": "custom", "objectives": [], "rewards": [] },
          "blank_slot_2": { "title": "", "description": "", "category": "custom", "objectives": [], "rewards": [] }
        }
        """;
}
