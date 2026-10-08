package com.erikedits.justquests.plugin.team;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Team quests: a player's team is the one made with /quest team, else their scoreboard team
 * (/team). A team keeps its quests in the same shape as a player (active quests with progress,
 * completed quests), in plugins/JustQuests/team-progress.json.
 */
public final class Teams {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** A player's team: the key its quests are kept under, its name, and the party if it is one. */
    public record TeamRef(String key, String name, Party party) {}

    private final JustQuestsPlugin plugin;
    private final Parties parties;
    private final Path file;
    private final Map<String, PlayerData> progress = new HashMap<>();
    private final Set<String> dirty = new HashSet<>();

    public Teams(JustQuestsPlugin plugin, Path dir) {
        this.plugin = plugin;
        this.parties = new Parties(dir.resolve("teams.json"), plugin.getLogger());
        this.file = dir.resolve("team-progress.json");
    }

    public Parties parties() {
        return parties;
    }

    /** The player's team, or null. */
    public TeamRef of(UUID player) {
        if (plugin.settings().teamParties) {
            Party p = parties.of(player);
            if (p != null) return new TeamRef(Parties.key(p), p.name, p);
        }
        if (plugin.settings().teamScoreboard) {
            Team t = scoreboardTeam(player);
            if (t != null) return new TeamRef("team:" + t.getName(), t.getName(), null);
        }
        return null;
    }

    public Team scoreboardTeam(UUID player) {
        String name = name(player);
        return name == null || Bukkit.getScoreboardManager() == null ? null
            : Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(name);
    }

    /** Everyone in the team: the party's members, or the players among the scoreboard team's entries. */
    public Set<UUID> members(TeamRef team) {
        if (team.party() != null) return new LinkedHashSet<>(team.party().members);
        Set<UUID> out = new LinkedHashSet<>();
        Team t = Bukkit.getScoreboardManager() == null ? null : Bukkit.getScoreboardManager().getMainScoreboard().getTeam(team.name());
        if (t == null) return out;
        Set<String> wanted = new HashSet<>();
        for (String entry : t.getEntries()) {
            Player online = Bukkit.getPlayerExact(entry);
            if (online != null) out.add(online.getUniqueId());
            else wanted.add(entry.toLowerCase(Locale.ROOT));
        }
        if (!wanted.isEmpty()) {
            for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                if (p.getName() != null && wanted.contains(p.getName().toLowerCase(Locale.ROOT))) out.add(p.getUniqueId());
            }
        }
        return out;
    }

    public Set<Player> onlineMembers(TeamRef team) {
        Set<Player> out = new LinkedHashSet<>();
        for (UUID m : members(team)) {
            Player p = Bukkit.getPlayer(m);
            if (p != null) out.add(p);
        }
        return out;
    }

    private static String name(UUID player) {
        Player online = Bukkit.getPlayer(player);
        return online != null ? online.getName() : Bukkit.getOfflinePlayer(player).getName();
    }

    /** The team's quests, created if absent. */
    public PlayerData progress(TeamRef team) {
        return progress.computeIfAbsent(team.key(), k -> new PlayerData());
    }

    /** The team's quests, or null (nothing is created). */
    public PlayerData peek(TeamRef team) {
        return team == null ? null : progress.get(team.key());
    }

    public void markDirty(TeamRef team) {
        dirty.add(team.key());
    }

    // --- persistence ---------------------------------------------------------------------------

    /** @param modWorld the main world's justquests folder: a world played with the mod brings its teams along */
    public void load(Path modWorld) {
        if (modWorld != null) {
            for (String name : new String[]{"teams.json", "team-progress.json"}) {
                Path own = name.equals("teams.json") ? parties.file() : file;
                try {
                    if (!Files.exists(own) && Files.exists(modWorld.resolve(name))) {
                        Files.createDirectories(own.getParent());
                        Files.copy(modWorld.resolve(name), own);
                        plugin.getLogger().info("Took over the mod's " + name);
                        if (name.equals("teams.json")) takeOverOwed(modWorld.resolve(name));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not take over the mod's " + name + ": " + e.getMessage());
                }
            }
        }
        parties.load();
        progress.clear();
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                if (e.getValue().isJsonObject()) progress.put(e.getKey(), PlayerData.fromJson(e.getValue().getAsJsonObject()));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not read " + file.getFileName() + ": " + e.getMessage());
        }
    }

    /**
     * The mod keeps the rewards of team quests that scoreboard-team members missed while offline in
     * teams.json ("owed", by uuid or lower-case name); here they wait in the player's own data.
     */
    private void takeOverOwed(Path modTeams) {
        try {
            JsonObject root = JsonParser.parseString(Files.readString(modTeams)).getAsJsonObject();
            if (!root.has("owed") || !root.get("owed").isJsonObject()) return;
            Map<String, UUID> byName = new HashMap<>();
            for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                if (p.getName() != null) byName.put(p.getName().toLowerCase(Locale.ROOT), p.getUniqueId());
            }
            long now = System.currentTimeMillis();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("owed").entrySet()) {
                UUID who;
                try {
                    who = UUID.fromString(e.getKey());
                } catch (IllegalArgumentException notUuid) {
                    who = byName.get(e.getKey().toLowerCase(Locale.ROOT));
                }
                if (who == null || !e.getValue().isJsonArray()) continue;
                PlayerData data = plugin.store().get(who);
                for (JsonElement id : e.getValue().getAsJsonArray()) {
                    data.completed.put(id.getAsString(), now);
                    data.pendingClaim.put(id.getAsString(), now);
                }
                plugin.store().markDirty(who);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not take over the mod's waiting team rewards: " + e.getMessage());
        }
    }

    public void saveDirty() {
        if (dirty.isEmpty()) return;
        dirty.clear();
        JsonObject root = new JsonObject();
        progress.forEach((key, data) -> {
            if (!data.active.isEmpty() || !data.completed.isEmpty()) root.add(key, data.toJson());
        });
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save " + file.getFileName() + ": " + e.getMessage());
        }
    }
}
