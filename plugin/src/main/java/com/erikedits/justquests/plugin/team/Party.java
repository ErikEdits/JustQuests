package com.erikedits.justquests.plugin.team;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** A team made with /quest team: a name, a leader and its members (the leader among them). */
public final class Party {
    final String id;
    final String name;
    UUID leader;
    final Set<UUID> members = new LinkedHashSet<>();

    Party(String id, String name, UUID leader) {
        this.id = id;
        this.name = name;
        this.leader = leader;
        members.add(leader);
    }

    public String name() {
        return name;
    }

    public UUID leader() {
        return leader;
    }

    public Set<UUID> members() {
        return members;
    }
}
