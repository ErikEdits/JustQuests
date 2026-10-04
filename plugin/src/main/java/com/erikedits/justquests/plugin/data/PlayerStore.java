package com.erikedits.justquests.plugin.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Quest progress, one human-readable file per player: plugins/JustQuests/players/&lt;uuid&gt;.json.
 * Everything is kept in memory (for the leaderboard); changes mark the player dirty and are written
 * every minute, on quit and on shutdown, each file through a temp file so a crash never corrupts it.
 *
 * <p>On the first start the progress of a world that was played with the mod
 * (&lt;world&gt;/justquests/progress.json) is imported, so a singleplayer world can move to the server.
 */
public final class PlayerStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path dir;
    private final Logger log;
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final Set<UUID> dirty = new HashSet<>();

    public PlayerStore(Path dir, Logger log) {
        this.dir = dir;
        this.log = log;
    }

    public void load(Path modProgress) {
        players.clear();
        dirty.clear();
        boolean fresh = !Files.isDirectory(dir);
        try {
            Files.createDirectories(dir);
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
                for (Path f : files) {
                    String name = f.getFileName().toString();
                    try {
                        UUID id = UUID.fromString(name.substring(0, name.length() - 5));
                        JsonElement e = JsonParser.parseString(Files.readString(f));
                        if (e.isJsonObject()) players.put(id, PlayerData.fromJson(e.getAsJsonObject()));
                    } catch (Exception e) {
                        log.warning("Skipped player file " + name + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.severe("Could not read the player files: " + e.getMessage());
        }
        if (fresh && modProgress != null && Files.exists(modProgress)) importMod(modProgress);
        log.info("Loaded quest progress for " + players.size() + " player(s)");
    }

    private void importMod(Path file) {
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file));
            if (!root.isJsonObject()) return;
            int n = 0;
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet()) {
                try {
                    UUID id = UUID.fromString(e.getKey());
                    players.put(id, PlayerData.fromJson(e.getValue().getAsJsonObject()));
                    dirty.add(id);
                    n++;
                } catch (Exception ignored) {
                    // not a player entry
                }
            }
            saveDirty();
            log.info("Imported the quest progress of " + n + " player(s) from the mod's " + file);
        } catch (Exception e) {
            log.warning("Could not import " + file + ": " + e.getMessage());
        }
    }

    /** The player's data, created if absent. */
    public PlayerData get(UUID id) {
        return players.computeIfAbsent(id, k -> new PlayerData());
    }

    /** The player's data, or null (nothing is created). */
    public PlayerData peek(UUID id) {
        return players.get(id);
    }

    public Map<UUID, PlayerData> all() {
        return Collections.unmodifiableMap(players);
    }

    public void markDirty(UUID id) {
        dirty.add(id);
    }

    public void saveDirty() {
        for (UUID id : dirty) save(id);
        dirty.clear();
    }

    public void save(UUID id) {
        PlayerData d = players.get(id);
        Path file = dir.resolve(id + ".json");
        try {
            Files.createDirectories(dir);
            if (d == null || d.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            Path tmp = dir.resolve(id + ".json.tmp");
            Files.writeString(tmp, GSON.toJson(d.toJson()));
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.severe("Could not save quest progress of " + id + ": " + e.getMessage());
        }
    }
}
