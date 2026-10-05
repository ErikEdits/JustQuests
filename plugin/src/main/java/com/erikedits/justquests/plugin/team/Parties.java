package com.erikedits.justquests.plugin.team;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The teams players make with /quest team, kept in plugins/JustQuests/teams.json. Invitations live
 * only in memory and run out after five minutes.
 */
public final class Parties {
    private static final long INVITE_MS = 5 * 60_000L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private record Invite(String party, long until) {}

    private final Path file;
    private final Logger log;
    private final Map<String, Party> parties = new LinkedHashMap<>();
    private final Map<UUID, Party> byMember = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();

    public Parties(Path file, Logger log) {
        this.file = file;
        this.log = log;
    }

    public Party of(UUID player) {
        return byMember.get(player);
    }

    public Party byName(String name) {
        for (Party p : parties.values()) {
            if (p.name.equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    public Collection<Party> all() {
        return parties.values();
    }

    public Party create(String name, UUID leader) {
        String id;
        do {
            id = UUID.randomUUID().toString().substring(0, 8);
        } while (parties.containsKey(id));
        Party p = new Party(id, name, leader);
        parties.put(id, p);
        byMember.put(leader, p);
        save();
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

    public void join(Party p, UUID player) {
        p.members.add(player);
        byMember.put(player, p);
        save();
    }

    /** Takes the player out of their team: a leaving leader hands over to the next member, an empty team ends. */
    public void leave(UUID player) {
        Party p = byMember.remove(player);
        if (p == null) return;
        p.members.remove(player);
        if (p.members.isEmpty()) parties.remove(p.id);
        else if (p.leader.equals(player)) p.leader = p.members.iterator().next();
        save();
    }

    /** "party:<id>" - the key its shared quests are kept under. */
    static String key(Party p) {
        return "party:" + p.id;
    }

    public static boolean validName(String name) {
        return name.matches("[A-Za-z0-9_-]{1,16}");
    }

    // --- persistence ---------------------------------------------------------------------------

    public void load() {
        parties.clear();
        byMember.clear();
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("teams").entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                Party p = new Party(e.getKey(), o.get("name").getAsString(), UUID.fromString(o.get("leader").getAsString()));
                for (JsonElement m : o.getAsJsonArray("members")) p.members.add(UUID.fromString(m.getAsString()));
                parties.put(p.id, p);
                for (UUID m : p.members) byMember.put(m, p);
            }
        } catch (Exception e) {
            log.warning("Could not read " + file.getFileName() + ": " + e.getMessage());
        }
    }

    public void save() {
        JsonObject teams = new JsonObject();
        for (Party p : parties.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", p.name);
            o.addProperty("leader", p.leader.toString());
            JsonArray members = new JsonArray();
            p.members.forEach(m -> members.add(m.toString()));
            o.add("members", members);
            teams.add(p.id, o);
        }
        JsonObject root = new JsonObject();
        root.add("teams", teams);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root));
        } catch (Exception e) {
            log.warning("Could not save " + file.getFileName() + ": " + e.getMessage());
        }
    }
}
