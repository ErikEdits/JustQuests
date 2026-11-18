package com.erikedits.justquests.storage;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads player/server-authored quests from a per-world file
 * (`<world>/justquests/custom-quests.json`, Q31) and, from 0.6.0, from every .json in
 * `<world>/justquests/quests/` (sub-folders too, any names): a file with "objectives" at the top is
 * one quest whose id is its path there (quests/mining/iron.json is justquests:mining/iron), any other
 * file is read like custom-quests.json. Custom quests override datapack quests on a shared id (Q54).
 * The files are watched and reloaded automatically when they change (Q32). A template is written on
 * first run.
 */
public final class CustomQuestLoader {
    private static Path file;
    /** <world>/justquests/quests/ (0.6.0): more quest files, any names, in sub-folders too. */
    private static Path folder;
    private static long lastModified = -1L;
    private static MinecraftServer server;

    private CustomQuestLoader() {}

    public static void init(MinecraftServer server) {
        CustomQuestLoader.server = server;
        file = server.getWorldPath(LevelResource.ROOT).resolve("justquests").resolve("custom-quests.json");
        folder = file.getParent().resolve("quests");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, TEMPLATE);
            }
            // the folder for more quest files, so it is easy to find
            Files.createDirectories(folder);
        } catch (Exception e) {
            JustQuests.LOG.error("Could not create custom-quests.json template", e);
        }
        load();
    }

    /** Called periodically; reloads only when the file changed (auto-reload). */
    public static void tickCheck() {
        if (file == null) return;
        try {
            long m = stamp();
            if (m != lastModified) {
                load();
                // the quest book only knows the quests it was sent: send the changed list
                if (server != null) com.erikedits.justquests.network.QuestNetwork.syncAll(server);
            }
        } catch (Exception ignored) {
            // transient FS error; try again next tick
        }
    }

    /** One number for custom-quests.json and every file of the quests folder: it changes when any of them does. */
    private static long stamp() throws java.io.IOException {
        long s = Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : -1L;
        if (folder != null && Files.isDirectory(folder)) {
            try (java.util.stream.Stream<Path> files = Files.walk(folder)) {
                for (Path p : (Iterable<Path>) files::iterator) {
                    if (p.toString().endsWith(".json")) s = 31 * s + Files.getLastModifiedTime(p).toMillis() + p.hashCode();
                }
            }
        }
        return s;
    }

    /** Parses the custom files and pushes the result into the QuestManager. */
    public static void load() {
        Map<Identifier, Quest> custom = new HashMap<>();
        Map<Identifier, com.erikedits.justquests.data.Badge> badges = new HashMap<>();
        JsonObject pack = null;
        try {
            lastModified = file == null ? -1L : stamp();
        } catch (Exception e) {
            lastModified = -1L;
        }
        if (file != null && Files.exists(file)) {
            try {
                JsonElement root = JsonParser.parseString(Files.readString(file));
                if (root.isJsonObject()) pack = readFile(root.getAsJsonObject(), "custom-quests.json", custom, badges);
            } catch (Exception e) {
                JustQuests.LOG.error("Could not read custom-quests.json", e);
            }
        }
        // 0.6.0: the quests folder, sorted so the same files always give the same result
        if (folder != null && Files.isDirectory(folder)) {
            java.util.List<Path> files = new java.util.ArrayList<>();
            try (java.util.stream.Stream<Path> walk = Files.walk(folder)) {
                walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json")).sorted().forEach(files::add);
            } catch (Exception e) {
                JustQuests.LOG.error("Could not read the quests folder", e);
            }
            for (Path p : files) {
                String rel = folder.relativize(p).toString().replace('\\', '/');
                try {
                    JsonElement root = JsonParser.parseString(Files.readString(p));
                    if (!root.isJsonObject()) continue;
                    JsonObject obj = root.getAsJsonObject();
                    if (obj.has("objectives")) {
                        // one quest per file: its path is its id, as in a datapack (any file name works)
                        String key = fileId(rel.substring(0, rel.length() - 5));
                        if (!key.equals(rel.substring(0, rel.length() - 5))) JustQuests.LOG.info("quests/{} has the quest id justquests:{}", rel, key);
                        readQuest(key, obj, "quests/" + rel, custom);
                    } else {
                        JsonObject own = readFile(obj, "quests/" + rel, custom, badges);
                        if (own != null) pack = own;
                    }
                } catch (Exception e) {
                    JustQuests.LOG.error("Could not read quests/{}: {}", rel, e.getMessage());
                }
            }
        }
        QuestManager.INSTANCE.setCustomQuests(custom);
        com.erikedits.justquests.badge.Badges.setCustom(badges, pack);
        JustQuests.LOG.info("Loaded {} custom quest(s)", custom.size());
    }

    /**
     * A file's path as a quest id: lower case, umlauts spelled out, and anything else an id can't hold
     * becomes "_", so any file name works ("Meine Quest.json" is justquests:meine_quest).
     */
    static String fileId(String path) {
        StringBuilder b = new StringBuilder();
        for (char c : path.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            switch (c) {
                case '\u00e4' -> b.append("ae");
                case '\u00f6' -> b.append("oe");
                case '\u00fc' -> b.append("ue");
                case '\u00df' -> b.append("ss");
                default -> b.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '-' ? c : '_');
            }
        }
        return b.toString();
    }

    /** A file like custom-quests.json: quest ids as keys, "badges" and "pack"; returns its "pack" or null. */
    private static JsonObject readFile(JsonObject root, String where, Map<Identifier, Quest> custom,
                                       Map<Identifier, com.erikedits.justquests.data.Badge> badges) {
        JsonObject pack = null;
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            String key = e.getKey();
            if (key.startsWith("_")) continue;          // _help etc.
            // badges (0.6.0): "badges": { "mypack:chapter_1": { "name": ..., ... } }
            if (key.equals("badges") && e.getValue().isJsonObject()) {
                readBadges(e.getValue().getAsJsonObject(), badges);
                continue;
            }
            // what the JustQuests Discord needs to know to make the badges roles (pack.json in a datapack)
            if (key.equals("pack") && e.getValue().isJsonObject()) {
                pack = e.getValue().getAsJsonObject();
                continue;
            }
            if (!e.getValue().isJsonObject()) continue;
            readQuest(key, e.getValue().getAsJsonObject(), where, custom);
        }
        return pack;
    }

    /** One quest; blank template slots (no objectives) are skipped silently. A later file wins on the same id. */
    private static void readQuest(String key, JsonObject obj, String where, Map<Identifier, Quest> custom) {
        if (!obj.has("objectives") || obj.getAsJsonArray("objectives").isEmpty()) return;
        try {
            Identifier id = key.contains(":")
                ? Identifier.parse(key)
                : Identifier.fromNamespaceAndPath("justquests", key);
            Quest.CODEC.parse(JsonOps.INSTANCE, obj)
                .resultOrPartial(err -> JustQuests.LOG.error("Custom quest {} ({}) invalid: {}", key, where, err))
                .ifPresent(q -> {
                    String problem = com.erikedits.justquests.data.QuestManager.problem(q, obj);
                    if (problem != null) {
                        JustQuests.LOG.warn("Custom quest {} ({}) {} - skipping", key, where, problem);
                        return;
                    }
                    if (custom.containsKey(id)) JustQuests.LOG.warn("Custom quest {} is in more than one file, {} wins", id, where);
                    custom.put(id, q.withFolderCategory(id.getPath()));
                });
        } catch (Exception ex) {
            JustQuests.LOG.error("Custom quest {} ({}) failed: {}", key, where, ex.getMessage());
        }
    }

    private static void readBadges(JsonObject section, Map<Identifier, com.erikedits.justquests.data.Badge> out) {
        for (Map.Entry<String, JsonElement> b : section.entrySet()) {
            try {
                Identifier id = Identifier.tryParse(b.getKey().contains(":") ? b.getKey() : "justquests:" + b.getKey());
                if (id == null) throw new IllegalArgumentException("not a valid id");
                com.erikedits.justquests.data.Badge.CODEC.parse(JsonOps.INSTANCE, b.getValue())
                    .resultOrPartial(err -> JustQuests.LOG.error("Custom badge {} invalid: {}", b.getKey(), err))
                    .ifPresent(badge -> out.put(id, badge));
            } catch (Exception ex) {
                JustQuests.LOG.error("Custom badge {} failed: {}", b.getKey(), ex.getMessage());
            }
        }
    }

    public static void clear() {
        file = null;
        folder = null;
        server = null;
        lastModified = -1L;
        QuestManager.INSTANCE.setCustomQuests(new HashMap<>());
    }

    private static final String TEMPLATE = """
        {
          "_help": "Add your own quests here. Each key is the quest id (a bare name becomes justquests:<name>, or use a full namespace:path). More quest files can go into the quests folder next to this file (sub-folders too, any names): a file in this form is read like this one, a file that is a single quest (with objectives at the top) gets its path as id. Keys starting with _ are ignored: the two _example quests below only show how a quest looks - copy one and take the _ away to use it. Slots with no objectives are skipped, so you can leave blank slots. Custom quests override datapack quests with the same id. The file reloads automatically when you save it. Objective types: collect_item, kill_mob, place_block, mine_block, craft_item, smelt_item, consume_item, enchant_item, use_item, have_item (the player holds that many in the inventory, wherever they came from), anvil_item (results taken out of an anvil: item checks the result, with the item in the right slot), grindstone_item (results taken out of a grindstone; disenchant: true counts only when enchantments come off), tame_animal, breed_animal, gain_advancement, visit_dimension, reach_level, reach_location, submit_item (hand items in: the Hand in link in the quest book or /quest submit takes them), fish_item (item is optional), trade_villager (item and profession are optional, minecraft:wandering_trader works too), brew_potion (potion like minecraft:swiftness, its long and strong forms count too, and item are optional), stat (a statistic of the statistics screen like minecraft:jump; distances count in blocks, times in minutes; only what happens after taking the quest counts), interact_entity (entity, item held is optional), use_block (block, item held is optional), find_structure (structure: an id or #tag) and find_biome (biome: an id or #tag; for both a count above 1 means that many different ones). With Farmer's Delight also cut_item (on a cutting board; item and tool are optional), cook_item (item and via: cooking_pot, skillet, stove or feast are optional; pot meals and cutting board results count for craft_item too) and rich_soil (organic compost the player put down turns into rich soil). An objective may also set group (objectives with the same group are alternatives, one of them is enough) and label (the goal text to show instead of the generated one, a string or a per-language map). Reward types: give_item, command, loot_table, xp, effect, message, title, choice (the player picks one of its options when claiming). A quest may also set requires (a list of quest ids that must be completed first), repeatable (true/false), cooldown_hours (wait time before a repeatable quest can be taken again), icon (an item id shown in the quest book; picked from the first goal when left out; it may carry properties the way /give writes them, such as minecraft:potion[minecraft:potion_contents={potion:'minecraft:swiftness'}]), permission (a permission node: only players who have it see the quest), team (true: one quest for the player's whole team, see /quest team), requires_min (only this many of requires must be done first, e.g. any 12 of 16) reset_on_death (true: the progress goes back to zero when the player dies) and auto_accept (false: the quest is never accepted by itself, only with its Accept button). Item fields accept an id, a #tag, or an object like {\\"id\\": \\"minecraft:diamond_sword\\", \\"enchantments\\": {\\"minecraft:sharpness\\": 2}} (also \\"potion\\" and \\"name\\"). Block and mob fields accept an id or a #tag. craft_item also counts the stonecutter, smithing table, loom and cartography table. enchant_item may set via: table (the default), anvil or any. An objective with \\"distinct\\": true counts every kind of item, block or mob only once (eat 10 different foods). title and description can be a plain string OR a per-language map like {\\"en_us\\": \\"...\\", \\"de_de\\": \\"...\\"} - each player sees their own language, with English as fallback. Item, mob and block names in the goal line are translated automatically. See https://github.com/ErikEdits/JustQuests",
          "_example_quest": {
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
          "_example_multilang": {
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
