package com.erikedits.justquests.storage;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.PlayerQuestData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-world quest progress, stored as a human-readable JSON file in the
 * world folder: <world>/justquests/progress.json.
 *
 * This file is the single source of truth for player progress and is the
 * compatibility bridge to the future Paper/Bukkit plugin and the
 * singleplayer -> server migration (it travels with the world). The same
 * codecs can serialize to NBT too if ever needed (Q15).
 *
 * Saving is "loose": changes set a dirty flag and are flushed periodically
 * (and on server stop), not every tick, to spare hardware (Q47).
 */
public class WorldQuestStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Codec<Map<UUID, PlayerQuestData>> PLAYERS_CODEC =
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerQuestData.CODEC);

    private static WorldQuestStore instance;

    private final Map<UUID, PlayerQuestData> players = new HashMap<>();
    private Path file;
    private boolean dirty = false;

    public static WorldQuestStore get() {
        return instance;
    }

    /** Loads the store from the world folder; called on server start. */
    public static void load(MinecraftServer server) {
        WorldQuestStore store = new WorldQuestStore();
        store.file = server.getWorldPath(LevelResource.ROOT).resolve("justquests").resolve("progress.json");
        try {
            if (Files.exists(store.file)) {
                JsonElement json = JsonParser.parseString(Files.readString(store.file));
                PLAYERS_CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> JustQuests.LOG.error("Failed to parse progress.json: {}", err))
                    .ifPresent(store.players::putAll);
            }
        } catch (Exception e) {
            JustQuests.LOG.error("Could not read progress.json", e);
        }
        instance = store;
        JustQuests.LOG.info("Loaded quest progress for {} player(s)", store.players.size());
    }

    /** Flushes to disk if there are unsaved changes. */
    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            JsonElement json = PLAYERS_CODEC.encodeStart(JsonOps.INSTANCE, players).getOrThrow();
            Files.writeString(file, GSON.toJson(json));
            dirty = false;
        } catch (Exception e) {
            JustQuests.LOG.error("Could not write progress.json", e);
        }
    }

    /** Saves and clears the instance; called on server stop. */
    public static void unload() {
        if (instance != null) {
            instance.save();
            instance = null;
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean has(UUID id) {
        return players.containsKey(id);
    }

    /** Returns the player's data, creating an empty record if absent. */
    public PlayerQuestData get(UUID id) {
        return players.computeIfAbsent(id, k -> new PlayerQuestData());
    }
}
