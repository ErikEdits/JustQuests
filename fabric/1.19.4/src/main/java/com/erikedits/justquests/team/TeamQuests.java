package com.erikedits.justquests.team;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.QuestMode;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.player.QuestProgress;
import com.erikedits.justquests.progress.QuestProgressService;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.erikedits.justquests.text.Msg;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Team quests ("team": true in a quest): one quest for a whole team - the team made with
 * /quest team, else the player's scoreboard team (/team). Any member takes it, every member's
 * actions count, and when it is done every member finds the rewards waiting to be claimed (a
 * scoreboard-team member who was offline gets them on the next visit). The quest book shows a
 * player their own quests plus their team's ({@link #view}).
 */
public final class TeamQuests {
    public static final int MAX_MEMBERS = 8;

    /** A command's outcome, with the lines for the player. */
    public record Result(boolean ok, List<Component> lines) {
        static Result ok(Component... lines) {
            return new Result(true, List.of(lines));
        }

        static Result fail(Component line) {
            return new Result(false, List.of(line));
        }
    }

    /** A player's team: the key its quests are kept under, its name, and what it is. */
    private record Team(String key, String name, TeamStore.Party party, PlayerTeam scoreboard) {}

    private TeamQuests() {}

    private static Team of(ServerPlayer player) {
        TeamStore store = TeamStore.get();
        if (store == null) return null;
        TeamStore.Party p = store.partyOf(player.getUUID());
        if (p != null) {
            p.names.put(player.getUUID(), player.getName().getString());
            return new Team(p.key(), p.name, p, null);
        }
        PlayerTeam t = server(player).getScoreboard().getPlayersTeam(player.getScoreboardName());
        return t == null ? null : new Team("team:" + t.getName(), t.getName(), null, t);
    }

    private static List<ServerPlayer> onlineMembers(ServerPlayer player, Team team) {
        MinecraftServer server = server(player);
        List<ServerPlayer> out = new ArrayList<>();
        if (team.party() != null) {
            for (UUID m : team.party().members) {
                ServerPlayer p = server.getPlayerList().getPlayer(m);
                if (p != null) out.add(p);
            }
        } else {
            for (String name : team.scoreboard().getPlayers()) {
                ServerPlayer p = server.getPlayerList().getPlayerByName(name);
                if (p != null) out.add(p);
            }
        }
        if (!out.contains(player)) out.add(player);
        return out;
    }

    /** What the player's quest book shows: their own quests plus their team's. For reading only. */
    public static PlayerQuestData view(ServerPlayer player, PlayerQuestData own) {
        settle(player);
        Team team = of(player);
        PlayerQuestData data = team == null ? null : TeamStore.get().peekProgress(team.key());
        if (data == null || (data.active.isEmpty() && data.completed.isEmpty())) return own;
        PlayerQuestData v = own == null ? new PlayerQuestData()
            : new PlayerQuestData(own.teamId, own.active, own.pendingClaim, own.completed);
        data.active.forEach(v.active::putIfAbsent);
        data.completed.forEach((id, at) -> v.completed.merge(id, at, Math::max));
        return v;
    }

    /** The rewards of team quests finished while the player was offline. */
    private static void settle(ServerPlayer player) {
        TeamStore teams = TeamStore.get();
        WorldQuestStore store = WorldQuestStore.get();
        if (teams == null || store == null) return;
        Set<String> ids = new LinkedHashSet<>(teams.takeOwed(player.getUUID().toString()));
        ids.addAll(teams.takeOwed(player.getScoreboardName().toLowerCase(Locale.ROOT)));
        if (ids.isEmpty()) return;
        PlayerQuestData data = store.get(player.getUUID());
        long now = System.currentTimeMillis();
        for (String s : ids) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id == null) continue;
            data.completed.put(id, now);
            data.pendingClaim.put(id, now);
            send(player, completed(player, id));
        }
        store.markDirty();
    }

    /** Every event: what it adds to the team's quests. */
    public static void advance(ServerPlayer player, QuestProgressService.ObjectiveTest test) {
        Team team = of(player);
        PlayerQuestData data = team == null ? null : TeamStore.get().peekProgress(team.key());
        if (data == null || data.active.isEmpty()) return;
        boolean changed = false;
        List<ResourceLocation> completing = new ArrayList<>();
        for (Map.Entry<ResourceLocation, QuestProgress> e : data.active.entrySet()) {
            Quest quest = QuestManager.INSTANCE.get(e.getKey());
            if (quest == null) continue;
            QuestProgress progress = e.getValue();
            boolean all = true, any = false;
            for (int i = 0; i < quest.objectives().size(); i++) {
                QuestObjective obj = quest.objectives().get(i);
                int add = test.amount(obj);
                if (add > 0) {
                    int real = Math.min(add, obj.requiredCount() - progress.get(i));
                    if (real > 0) {
                        progress.increment(i, real);
                        changed = true;
                    }
                }
                if (progress.get(i) >= obj.requiredCount()) any = true;
                else all = false;
            }
            if (quest.mode() == QuestMode.ANY ? any : all) completing.add(e.getKey());
        }
        for (ResourceLocation id : completing) finish(player, team, data, id);
        if (changed || !completing.isEmpty()) {
            TeamStore.get().markDirty();
            for (ServerPlayer m : onlineMembers(player, team)) sync(m);
        }
    }

    /** Done: completed for the team, and every member's rewards wait to be claimed. */
    private static void finish(ServerPlayer player, Team team, PlayerQuestData teamData, ResourceLocation id) {
        teamData.complete(id);
        WorldQuestStore store = WorldQuestStore.get();
        MinecraftServer server = server(player);
        Set<UUID> members = new LinkedHashSet<>();
        members.add(player.getUUID());
        if (team.party() != null) {
            members.addAll(team.party().members);
        } else {
            for (String name : team.scoreboard().getPlayers()) {
                ServerPlayer online = server.getPlayerList().getPlayerByName(name);
                if (online != null) members.add(online.getUUID());
                else TeamStore.get().owe(name.toLowerCase(Locale.ROOT), id.toString());
            }
        }
        long now = System.currentTimeMillis();
        for (UUID m : members) {
            PlayerQuestData d = store.get(m);
            d.complete(id);
            d.pendingClaim.put(id, now);
            ServerPlayer online = server.getPlayerList().getPlayer(m);
            if (online != null) send(online, completed(online, id));
        }
        store.markDirty();
    }

    private static Component completed(ServerPlayer p, ResourceLocation id) {
        return Msg.tr("justquests.team.completed", title(id, p)).append(" ").append(QuestProgressService.claimButton(id));
    }

    private static String title(ResourceLocation id, ServerPlayer p) {
        Quest q = QuestManager.INSTANCE.get(id);
        return q == null ? id.toString() : q.title().get(lang(p));
    }

    /** /quest accept for a team quest: taken for the whole team; the others hear about it. */
    public static Result accept(ServerPlayer player, ResourceLocation id, Quest quest) {
        Team team = of(player);
        if (team == null) return Result.fail(Msg.tr("justquests.team.none"));
        WorldQuestStore store = WorldQuestStore.get();
        if (store == null) return Result.fail(Msg.tr("justquests.error.storage"));
        PlayerQuestData own = store.get(player.getUUID());
        PlayerQuestData data = TeamStore.get().progress(team.key());
        if (data.isActive(id)) return Result.fail(Msg.tr("justquests.accept.already_active"));
        if (own.isClaimable(id)) return Result.fail(Msg.tr("justquests.accept.claim_first", id.toString()));
        for (ResourceLocation req : quest.requires()) {
            if (!own.isCompleted(req)) return Result.fail(Msg.tr("justquests.accept.locked", title(req, player)));
        }
        Long done = own.completed.get(id);
        Long teamDone = data.completed.get(id);
        if (done == null || (teamDone != null && teamDone > done)) done = teamDone;
        if (done != null) {
            if (!quest.repeatable()) return Result.fail(Msg.tr("justquests.accept.done"));
            long left = quest.cooldownHours().orElse(0) * 3_600_000L - (System.currentTimeMillis() - done);
            if (left > 0) return Result.fail(Msg.tr("justquests.accept.cooldown", duration(left)));
        }
        data.accept(id);
        TeamStore.get().markDirty();
        for (ServerPlayer m : onlineMembers(player, team)) {
            if (m != player) send(m, Msg.tr("justquests.team.accepted", player.getName().getString(), title(id, m)));
            sync(m);
        }
        return Result.ok(Msg.tr("justquests.accept.ok", title(id, player)));
    }

    /** /quest abandon for the team's quest: it ends for the whole team; null if the team has no such quest. */
    public static Result abandon(ServerPlayer player, ResourceLocation id) {
        Team team = of(player);
        PlayerQuestData data = team == null ? null : TeamStore.get().peekProgress(team.key());
        if (data == null || !data.isActive(id)) return null;
        data.abandon(id);
        TeamStore.get().markDirty();
        for (ServerPlayer m : onlineMembers(player, team)) {
            if (m != player) send(m, Msg.tr("justquests.team.abandoned", player.getName().getString(), title(id, m)));
            sync(m);
        }
        return Result.ok(Msg.tr("justquests.abandon.ok", title(id, player)));
    }

    /** /quest team [info | create <name> | invite <player> | accept | leave | kick <name>]. */
    public static Result command(ServerPlayer player, String sub, String arg) {
        TeamStore teams = TeamStore.get();
        if (teams == null) return Result.fail(Msg.tr("justquests.error.storage"));
        UUID me = player.getUUID();
        String myName = player.getName().getString();
        TeamStore.Party mine = teams.partyOf(me);
        MinecraftServer server = server(player);
        switch (sub) {
            case "create": {
                if (mine != null) return Result.fail(Msg.tr("justquests.team.already"));
                if (arg == null || !arg.matches("[A-Za-z0-9_-]{1,16}")) return Result.fail(Msg.tr("justquests.team.bad_name"));
                if (teams.partyByName(arg) != null) return Result.fail(Msg.tr("justquests.team.name_taken"));
                TeamStore.Party p = teams.create(arg, me, myName);
                sync(player);
                return Result.ok(Msg.tr("justquests.team.created", p.name));
            }
            case "invite": {
                ServerPlayer target = arg == null ? null : server.getPlayerList().getPlayerByName(arg);
                if (mine == null) return Result.fail(Msg.tr("justquests.team.not_in"));
                if (target == null) return Result.fail(Msg.tr("justquests.team.not_member", String.valueOf(arg)));
                if (mine.members.contains(target.getUUID())) return Result.fail(Msg.tr("justquests.team.already_member", target.getName().getString()));
                if (mine.members.size() >= MAX_MEMBERS) return Result.fail(Msg.tr("justquests.team.full", MAX_MEMBERS));
                teams.invite(mine, target.getUUID());
                send(target, Msg.tr("justquests.team.invite", myName, mine.name).append(" ")
                    .append(Msg.button(Msg.tr("justquests.team.accept_button"), "/quest team accept", Msg.tr("justquests.team.accept_hover"))));
                return Result.ok(Msg.tr("justquests.team.invited", target.getName().getString()));
            }
            case "accept": {
                if (mine != null) return Result.fail(Msg.tr("justquests.team.already"));
                TeamStore.Party p = teams.takeInvite(me);
                if (p == null) return Result.fail(Msg.tr("justquests.team.no_invite"));
                if (p.members.size() >= MAX_MEMBERS) return Result.fail(Msg.tr("justquests.team.full", MAX_MEMBERS));
                teams.join(p, me, myName);
                tell(server, p, Msg.tr("justquests.team.joined", myName, p.name), me);
                sync(player);
                return Result.ok(Msg.tr("justquests.team.joined", myName, p.name));
            }
            case "leave": {
                if (mine == null) return Result.fail(Msg.tr("justquests.team.not_in"));
                tell(server, mine, Msg.tr("justquests.team.left", myName, mine.name), me);
                teams.leave(me);
                sync(player);
                return Result.ok(Msg.tr("justquests.team.left", myName, mine.name));
            }
            case "kick": {
                if (mine == null) return Result.fail(Msg.tr("justquests.team.not_in"));
                if (!mine.leader.equals(me)) return Result.fail(Msg.tr("justquests.team.leader_only"));
                UUID target = null;
                for (UUID m : mine.members) {
                    if (!m.equals(me) && arg != null && arg.equalsIgnoreCase(mine.names.get(m))) target = m;
                }
                if (target == null) return Result.fail(Msg.tr("justquests.team.not_member", String.valueOf(arg)));
                String name = mine.names.get(target);
                tell(server, mine, Msg.tr("justquests.team.kicked", name, mine.name), me);
                teams.leave(target);
                ServerPlayer kicked = server.getPlayerList().getPlayer(target);
                if (kicked != null) sync(kicked);
                return Result.ok(Msg.tr("justquests.team.kicked", name, mine.name));
            }
            default: {
                if (mine == null) {
                    PlayerTeam sb = server.getScoreboard().getPlayersTeam(player.getScoreboardName());
                    return Result.ok(sb != null ? Msg.tr("justquests.team.scoreboard", sb.getName()) : Msg.tr("justquests.team.not_in"));
                }
                List<Component> lines = new ArrayList<>();
                lines.add(Msg.tr("justquests.team.info_header", mine.name, mine.members.size()));
                for (UUID m : mine.members) {
                    lines.add(Msg.tr("justquests.team.info_member", mine.names.getOrDefault(m, m.toString().substring(0, 8)),
                        m.equals(mine.leader) ? Msg.tr("justquests.team.info_leader") : Msg.empty(),
                        server.getPlayerList().getPlayer(m) != null ? Msg.tr("justquests.team.info_online") : Msg.empty()));
                }
                return new Result(true, lines);
            }
        }
    }

    /** A line for the team's other members who are online. */
    private static void tell(MinecraftServer server, TeamStore.Party party, Component line, UUID except) {
        for (UUID m : party.members) {
            ServerPlayer p = m.equals(except) ? null : server.getPlayerList().getPlayer(m);
            if (p != null) send(p, line);
        }
    }

    /** "2h 5m" or "3m". */
    private static Component duration(long ms) {
        long min = ms / 60000L;
        long h = min / 60, m = min % 60;
        return h > 0 ? Msg.tr("justquests.time.hours_minutes", h, m) : Msg.tr("justquests.time.minutes", Math.max(1, m));
    }

    // --- what differs between Minecraft versions --------------------------------------------------

    private static MinecraftServer server(ServerPlayer p) {
        return p.getLevel().getServer();
    }

    private static String lang(ServerPlayer p) {
        return com.erikedits.justquests.data.LocalizedText.DEFAULT_LANG;
    }

    private static void send(ServerPlayer p, Component c) {
        p.sendSystemMessage(c);
    }

    /** Resends the player's quests to their quest book (where there is one). */
    private static void sync(ServerPlayer p) {
        // no quest book on this version: nothing to send
    }
}
