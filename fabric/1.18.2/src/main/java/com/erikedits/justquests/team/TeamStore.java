package com.erikedits.justquests.team;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.PlayerQuestData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The teams of team quests, per world: teams made with /quest team in
 * <world>/justquests/teams.json (with the rewards still owed to scoreboard-team members who were
 * offline), and every team's quests in team-progress.json - the same files as the server plugin.
 */
public final class TeamStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long INVITE_MS = 5 * 60_000L;

    /** A team made with /quest team. */
    public static final class Party {
        final String id;
        final String name;
        UUID leader;
        final Set<UUID> members = new LinkedHashSet<>();
        /** the members' names as last seen, for /quest team info and kick */
        final Map<UUID, String> names = new HashMap<>();

        Party(String id, String name, UUID leader) {
            this.id = id;
            this.name = name;
            this.leader = leader;
            members.add(leader);
        }

        public String name() {
            return name;
        }

        String key() {
            return "party:" + id;
        }
    }

    private record Invite(String party, long until) {}

    private static TeamStore instance;

    private Path dir;
    private final Map<String, Party> parties = new LinkedHashMap<>();
    private final Map<UUID, Party> byMember = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();
    private final Map<String, PlayerQuestData> progress = new HashMap<>();
    /** player uuid or lower-case name -> quest ids whose rewards they still get on their next visit */
    private final Map<String, Set<String>> owed = new LinkedHashMap<>();
    private boolean dirty;

    public static TeamStore get() {
        return instance;
    }

    /** Called with the world's justquests folder when the quest progress loads. */
    public static void load(Path dir) {
        TeamStore s = new TeamStore();
        s.dir = dir;
        try {
            Path teams = dir.resolve("teams.json");
            if (Files.exists(teams)) {
                JsonObject root = JsonParser.parseString(Files.readString(teams)).getAsJsonObject();
                if (root.has("teams")) {
                    for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("teams").entrySet()) {
                        JsonObject o = e.getValue().getAsJsonObject();
                        Party p = new Party(e.getKey(), o.get("name").getAsString(), UUID.fromString(o.get("leader").getAsString()));
                        for (JsonElement m : o.getAsJsonArray("members")) p.members.add(UUID.fromString(m.getAsString()));
                        if (o.has("names")) {
                            for (Map.Entry<String, JsonElement> n : o.getAsJsonObject("names").entrySet()) {
                                p.names.put(UUID.fromString(n.getKey()), n.getValue().getAsString());
                            }
                        }
                        s.parties.put(p.id, p);
                        for (UUID m : p.members) s.byMember.put(m, p);
                    }
                }
                if (root.has("owed")) {
                    for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("owed").entrySet()) {
                        Set<String> ids = new LinkedHashSet<>();
                        for (JsonElement q : e.getValue().getAsJsonArray()) ids.add(q.getAsString());
                        s.owed.put(e.getKey(), ids);
                    }
                }
            }
            Path prog = dir.resolve("team-progress.json");
            if (Files.exists(prog)) {
                JsonObject root = JsonParser.parseString(Files.readString(prog)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                    PlayerQuestData.CODEC.parse(JsonOps.INSTANCE, e.getValue()).result().ifPresent(d -> s.progress.put(e.getKey(), d));
                }
            }
        } catch (Exception e) {
            JustQuests.LOG.error("Could not read the team files", e);
        }
        instance = s;
    }

    public static void unload() {
        if (instance != null) {
            instance.save();
            instance = null;
        }
    }

    public void markDirty() {
        dirty = true;
    }

    public void saveIfDirty() {
        if (dirty) save();
    }

    public void save() {
        if (dir == null) return;
        try {
            Files.createDirectories(dir);
            JsonObject teams = new JsonObject();
            for (Party p : parties.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("name", p.name);
                o.addProperty("leader", p.leader.toString());
                JsonArray members = new JsonArray();
                p.members.forEach(m -> members.add(m.toString()));
                o.add("members", members);
                JsonObject names = new JsonObject();
                p.names.forEach((u, n) -> names.addProperty(u.toString(), n));
                o.add("names", names);
                teams.add(p.id, o);
            }
            JsonObject root = new JsonObject();
            root.add("teams", teams);
            JsonObject owedJson = new JsonObject();
            owed.forEach((who, ids) -> {
                JsonArray a = new JsonArray();
                ids.forEach(a::add);
                owedJson.add(who, a);
            });
            root.add("owed", owedJson);
            Files.writeString(dir.resolve("teams.json"), GSON.toJson(root));

            JsonObject prog = new JsonObject();
            progress.forEach((key, data) -> {
                if (!data.active.isEmpty() || !data.completed.isEmpty()) {
                    PlayerQuestData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().ifPresent(j -> prog.add(key, j));
                }
            });
            Files.writeString(dir.resolve("team-progress.json"), GSON.toJson(prog));
            dirty = false;
        } catch (Exception e) {
            JustQuests.LOG.error("Could not write the team files", e);
        }
    }

    // --- teams made with /quest team -------------------------------------------------------------

    public Party partyOf(UUID player) {
        return byMember.get(player);
    }

    public Party partyByName(String name) {
        for (Party p : parties.values()) {
            if (p.name.equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    public Party create(String name, UUID leader, String leaderName) {
        String id;
        do {
            id = UUID.randomUUID().toString().substring(0, 8);
        } while (parties.containsKey(id));
        Party p = new Party(id, name, leader);
        p.names.put(leader, leaderName);
        parties.put(id, p);
        byMember.put(leader, p);
        markDirty();
        return p;
    }

    public void invite(Party p, UUID target) {
        invites.put(target, new Invite(p.id, System.currentTimeMillis() + INVITE_MS));
    }

    /** The team that invited the player (and is still there), or null; the invitation is used up. */
    public Party takeInvite(UUID player) {
        Invite inv = invites.remove(player);
        if (inv == null || inv.until() < System.currentTimeMillis()) return null;
        return parties.get(inv.party());
    }

    public void join(Party p, UUID player, String playerName) {
        p.members.add(player);
        p.names.put(player, playerName);
        byMember.put(player, p);
        markDirty();
    }

    /** Takes the player out of their team: a leaving leader hands over, an empty team ends. */
    public void leave(UUID player) {
        Party p = byMember.remove(player);
        if (p == null) return;
        p.members.remove(player);
        p.names.remove(player);
        if (p.members.isEmpty()) parties.remove(p.id);
        else if (p.leader.equals(player)) p.leader = p.members.iterator().next();
        markDirty();
    }

    // --- team quests -----------------------------------------------------------------------------

    /** A team's quests, created if absent. */
    public PlayerQuestData progress(String key) {
        return progress.computeIfAbsent(key, k -> new PlayerQuestData());
    }

    /** A team's quests, or null. */
    public PlayerQuestData peekProgress(String key) {
        return key == null ? null : progress.get(key);
    }

    public void owe(String who, String questId) {
        owed.computeIfAbsent(who, k -> new LinkedHashSet<>()).add(questId);
        markDirty();
    }

    /** The quests owed to this uuid or name, removed from the list; empty if none. */
    public Set<String> takeOwed(String who) {
        Set<String> ids = owed.remove(who);
        if (ids == null) return Set.of();
        markDirty();
        return ids;
    }
}
