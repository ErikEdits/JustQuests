package com.erikedits.justquests.plugin.progress;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.quest.Objective;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.quest.Reward;
import com.erikedits.justquests.plugin.team.Teams.TeamRef;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The one place that moves quests forward, as in the mod: every game event asks how much it adds to
 * each objective of the player's active quests, and completion, rewards and messages follow from
 * here. Accepting, abandoning and claiming live here too, so the commands and the quest book behave
 * the same.
 */
public final class ProgressService {
    /** How much an event adds to an objective (0 = nothing). */
    @FunctionalInterface
    public interface Test {
        int amount(Objective objective);
    }

    /** The outcome of an action, with the message for the player. */
    public record Result(boolean ok, BaseComponent message) {}

    private final JustQuestsPlugin plugin;

    public ProgressService(JustQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    /** What an event did to a set of active quests. */
    private record Step(boolean changed, BaseComponent toast, List<String> completing) {}

    public void advance(Player player, Test test) {
        advance(player, null, test);
    }

    /**
     * As {@link #advance(Player, Test)}, for an event about one kind of thing (the id of an item,
     * block or mob): a distinct objective counts every kind only once.
     */
    public void advance(Player player, String kind, Test test) {
        plugin.generated().progress(player, test);
        String lang = Lang.of(player);
        boolean changed = false, completed = false;
        BaseComponent toast = null;

        PlayerData data = plugin.store().peek(player.getUniqueId());
        if (data != null && !data.active.isEmpty()) {
            Step s = step(data, kind, test, lang);
            for (String id : s.completing()) {
                Quest quest = plugin.quests().get(id);
                announce(player, quest, finish(player, data, id, quest));
            }
            if (s.changed()) plugin.store().markDirty(player.getUniqueId());
            changed = s.changed();
            completed = !s.completing().isEmpty();
            toast = s.toast();
        }

        TeamRef team = plugin.teams().of(player.getUniqueId());
        PlayerData teamData = plugin.teams().peek(team);
        if (teamData != null && !teamData.active.isEmpty()) {
            Step s = step(teamData, kind, test, lang);
            for (String id : s.completing()) finishTeam(player, team, teamData, id, plugin.quests().get(id));
            if (s.changed()) {
                plugin.teams().markDirty(team);
                for (Player member : plugin.teams().onlineMembers(team)) plugin.tracker().update(member);
            }
            changed |= s.changed();
            completed |= !s.completing().isEmpty();
            if (s.toast() != null) toast = s.toast();
        }

        if (changed) {
            if (toast != null && !completed) player.spigot().sendMessage(ChatMessageType.ACTION_BAR, toast);
            plugin.tracker().update(player);
        }
        unlocks(player);
    }

    /** Quests each player finished for the first time, until {@link #unlocks} looks at what they unlock. */
    private final Map<UUID, List<String>> fresh = new HashMap<>();

    /**
     * The quests that the player's newly finished quests unlocked: with auto-accept on (the default)
     * they are accepted at once, else the player gets an Accept button for each. Team quests only get
     * the button. Called after the "completed" messages, so the chat reads in order.
     */
    public void unlocks(Player player) {
        List<String> done = fresh.remove(player.getUniqueId());
        if (done == null) return;
        String lang = Lang.of(player);
        PlayerData data = plugin.store().get(player.getUniqueId());
        boolean auto = data.autoAccept != null ? data.autoAccept : plugin.settings().autoAccept;
        for (Quest quest : plugin.quests().all().values()) {
            if (quest.requires().stream().noneMatch(done::contains) || !plugin.canSee(player, quest)) continue;
            if (QuestStatus.of(plugin, player.getUniqueId(), plugin.view(player.getUniqueId()), quest) != QuestStatus.AVAILABLE) continue;
            String title = quest.title().get(lang);
            TextComponent line = new TextComponent("");
            if (auto && !quest.team() && accept(player, quest.id()).ok()) {
                line.addExtra(Text.tr(lang, "justquests.unlocked.accepted", title));
                line.addExtra(" ");
                line.addExtra(Text.button(Text.tr(lang, "justquests.unlocked.auto_off"), "/quest autoaccept off",
                    Text.tr(lang, "justquests.unlocked.auto_off_hover")));
            } else {
                line.addExtra(Text.tr(lang, "justquests.unlocked", title));
                line.addExtra(" ");
                line.addExtra(Text.button(Text.tr(lang, "justquests.unlocked.accept"), "/quest accept " + quest.id(),
                    Text.tr(lang, "justquests.unlocked.accept_hover")));
            }
            player.spigot().sendMessage(line);
        }
    }

    /**
     * /quest accept all: every quest the player may take now, except team quests (they are taken for
     * the whole team) and the generated board's (they are claimed one at a time). How many it took.
     */
    public int acceptAll(Player player) {
        PlayerData view = plugin.view(player.getUniqueId());
        int count = 0;
        for (Quest quest : plugin.quests().all().values()) {
            if (quest.team() || plugin.generated().onBoard(quest.id()) || !plugin.canSee(player, quest)) continue;
            if (!plugin.generated().visible(quest.id(), player.getUniqueId())) continue;
            if (QuestStatus.of(plugin, player.getUniqueId(), view, quest) != QuestStatus.AVAILABLE) continue;
            if (accept(player, quest.id()).ok()) count++;
        }
        return count;
    }

    /** Adds what the event counts to each active quest; the quests now complete come back. */
    private Step step(PlayerData data, String kind, Test test, String lang) {
        boolean changed = false;
        BaseComponent toast = null;
        List<String> completing = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, Integer>> entry : data.active.entrySet()) {
            Quest quest = plugin.quests().get(entry.getKey());
            if (quest == null) continue;
            Map<Integer, Integer> progress = entry.getValue();
            for (int i = 0; i < quest.objectives().size(); i++) {
                Objective obj = quest.objectives().get(i);
                int add = test.amount(obj);
                int have = progress.getOrDefault(i, 0);
                // have_item: the amount is what the player holds right now, and the progress only goes up
                if (obj instanceof Objective.Have) add = Math.max(0, add - have);
                if (add > 0 && have < obj.requiredCount() && obj.distinct()) {
                    // a distinct objective counts a kind it has not counted yet, once
                    add = kind != null && data.see(entry.getKey(), i, kind) ? 1 : 0;
                }
                if (add > 0 && have < obj.requiredCount()) {
                    have = Math.min(obj.requiredCount(), have + add);
                    progress.put(i, have);
                    changed = true;
                    if (obj.requiredCount() > 1) {
                        toast = Text.tr(lang, "justquests.plugin.progress", quest.shown(i, lang), have, obj.requiredCount());
                    }
                }
            }
            if (quest.done(i -> progress.getOrDefault(i, 0) >= quest.objectives().get(i).requiredCount())) completing.add(entry.getKey());
        }
        return new Step(changed, toast, completing);
    }

    /** reset_on_death: the player died, so their quests and their team's with that option start over. */
    public void died(Player player) {
        PlayerData data = plugin.store().peek(player.getUniqueId());
        List<String> own = data == null ? List.of() : reset(data);
        if (!own.isEmpty()) {
            String lang = Lang.of(player);
            for (String id : own) player.spigot().sendMessage(Text.tr(lang, "justquests.reset.death", title(id, lang)));
            plugin.store().markDirty(player.getUniqueId());
        }
        TeamRef team = plugin.teams().of(player.getUniqueId());
        PlayerData teamData = plugin.teams().peek(team);
        List<String> shared = teamData == null ? List.of() : reset(teamData);
        if (!shared.isEmpty()) {
            plugin.teams().markDirty(team);
            for (Player member : plugin.teams().onlineMembers(team)) {
                String lang = Lang.of(member);
                for (String id : shared) {
                    member.spigot().sendMessage(Text.tr(lang, "justquests.reset.death_team", title(id, lang), player.getName()));
                }
                plugin.tracker().update(member);
            }
        }
        if (!own.isEmpty()) plugin.tracker().update(player);
    }

    /** Sets the reset_on_death quests that have progress back to zero; their ids come back. */
    private List<String> reset(PlayerData data) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, Integer>> e : data.active.entrySet()) {
            Quest quest = plugin.quests().get(e.getKey());
            if (quest == null || !quest.resetOnDeath() || (e.getValue().isEmpty() && !data.seen.containsKey(e.getKey()))) continue;
            e.getValue().clear();
            data.seen.remove(e.getKey());
            out.add(e.getKey());
        }
        return out;
    }

    /**
     * A team quest is done: it counts as completed for the team. Members online get the rewards
     * like for their own quests (waiting to be claimed, or at once with claimRewards off); members
     * offline find them waiting to be claimed.
     */
    private void finishTeam(Player player, TeamRef team, PlayerData teamData, String id, Quest quest) {
        teamData.complete(id);
        long now = System.currentTimeMillis();
        Set<UUID> members = plugin.teams().members(team);
        members.add(player.getUniqueId());
        for (UUID member : members) {
            PlayerData d = plugin.store().get(member);
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                notifyDone(online, quest, finish(online, d, id, quest), "justquests.team.completed");
                unlocks(online);
                plugin.tracker().update(online);
            } else {
                d.complete(id);
                if (!quest.rewards().isEmpty()) d.pendingClaim.put(id, now);
                if (id.equals(d.pinned)) d.pinned = null;
            }
            plugin.store().markDirty(member);
        }
        broadcast(player, quest);
    }

    private void announce(Player player, Quest quest, boolean claim) {
        notifyDone(player, quest, claim, "justquests.complete.chat");
        broadcast(player, quest);
    }

    /** The finished quest in chat (with the claim button), the sound and the toast. */
    private void notifyDone(Player player, Quest quest, boolean claim, String key) {
        String lang = Lang.of(player);
        String title = quest.title().get(lang);
        TextComponent chat = new TextComponent("");
        chat.addExtra(Text.tr(lang, key, title));
        if (claim) {
            chat.addExtra(" ");
            chat.addExtra(claimButton(lang, quest.id()));
        }
        player.spigot().sendMessage(chat);
        if (plugin.settings().completionSound) {
            player.playSound(player.getLocation(), "minecraft:ui.toast.challenge_complete", SoundCategory.MASTER, 1f, 1f);
        }
        if (plugin.settings().completionToast) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, Text.tr(lang, "justquests.complete.toast", title));
        }
    }

    private void broadcast(Player player, Quest quest) {
        if (plugin.settings().announceCompletions) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.spigot().sendMessage(Text.tr(Lang.of(p), "justquests.complete.broadcast", player.getName(), quest.title().get(Lang.of(p))));
            }
            Bukkit.getConsoleSender().sendMessage(Text.legacy(Lang.DEFAULT, "justquests.complete.broadcast", player.getName(), quest.title().getDefault()));
        }
    }

    /**
     * Marks a quest finished. With claimRewards on (the default) its rewards wait for the Claim
     * button or /quest claim; otherwise they are paid now. Quests with a choice always wait.
     *
     * @return true when the rewards wait to be claimed
     */
    public boolean finish(Player player, PlayerData data, String id, Quest quest) {
        // what it unlocks is taken or offered by unlocks(), after the caller's messages
        if (!data.isCompleted(id)) fresh.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(id);
        data.complete(id);
        if (id.equals(data.pinned)) data.pinned = null;
        // a quest without rewards has nothing to claim: it is simply done
        boolean waits = !quest.rewards().isEmpty() && (plugin.settings().claimRewards || Reward.Choice.of(quest) != null);
        plugin.generated().completed(id, player.getUniqueId(), waits);
        if (waits) {
            data.pendingClaim.put(id, System.currentTimeMillis());
            return true;
        }
        grant(player, quest, -1);
        return false;
    }

    /** Pays out a finished quest's waiting rewards, with the picked option (0-based) of a choice. */
    public boolean claim(Player player, PlayerData data, String id, int pick) {
        if (data.pendingClaim.remove(id) == null) return false;
        Quest quest = plugin.quests().get(id);
        if (quest != null) grant(player, quest, pick);
        plugin.generated().rewardsClaimed(id, player.getUniqueId());
        plugin.store().markDirty(player.getUniqueId());
        return quest != null;
    }

    private void grant(Player player, Quest quest, int pick) {
        Reward.Choice choice = Reward.Choice.of(quest);
        for (Reward r : quest.rewards()) {
            if (r == choice) choice.grant(player, pick);
            else r.grant(player);
        }
        plugin.generated().bonus(player, quest);
    }

    /** A clickable [Claim rewards] that runs /quest claim for the quest. */
    public BaseComponent claimButton(String lang, String id) {
        return Text.button(Text.tr(lang, "justquests.rewards.button"), "/quest claim " + id,
            Text.tr(lang, "justquests.rewards.button_hover"));
    }

    // --- actions shared by the commands and the quest book ------------------------------------

    /** A prerequisite the player still misses (the first), or null once enough are done (requires_min). */
    public String missingRequirement(PlayerData data, Quest quest) {
        if (quest.needed() == 0 || (data != null && quest.unlocked(data::isCompleted))) return null;
        for (String req : quest.requires()) {
            if (data == null || !data.isCompleted(req)) return req;
        }
        return null;
    }

    /** The book's line for a locked quest: "Needs: X", or with requires_min "Needs 12 of 16 quests (5 done)". */
    public String lockedLine(String lang, PlayerData data, Quest quest, String missing) {
        if (quest.needed() < quest.requires().size()) {
            return Text.legacy(lang, "justquests.book.needs_of", quest.needed(), quest.requires().size(),
                data == null ? 0 : quest.requiredDone(data::isCompleted));
        }
        return Text.legacy(lang, "justquests.book.needs", title(missing, lang));
    }

    /** Why a quest cannot be accepted yet: the first missing quest, or with requires_min how many. */
    private BaseComponent locked(String lang, PlayerData data, Quest quest, String missing) {
        if (quest.needed() < quest.requires().size()) {
            return Text.tr(lang, "justquests.accept.locked_of", quest.needed(), quest.requires().size(),
                quest.requiredDone(data::isCompleted));
        }
        return Text.tr(lang, "justquests.accept.locked", title(missing, lang));
    }

    /** Milliseconds until a repeatable quest can be taken again (0 = now). */
    public long cooldownLeft(PlayerData data, Quest quest) {
        if (data == null || !data.isCompleted(quest.id()) || quest.cooldownHours() <= 0) return 0;
        long left = quest.cooldownHours() * 3_600_000L - (System.currentTimeMillis() - data.completed.get(quest.id()));
        return Math.max(0, left);
    }

    public String title(String id, String lang) {
        Quest q = plugin.quests().get(id);
        return q != null ? q.title().get(lang) : id;
    }

    public Result accept(Player player, String id) {
        String lang = Lang.of(player);
        Quest quest = plugin.quests().get(id);
        if (quest == null) return new Result(false, Text.tr(lang, "justquests.error.unknown_quest", id));
        if (!plugin.canSee(player, quest)) return new Result(false, Text.tr(lang, "justquests.accept.no_permission"));
        if (quest.team()) return acceptTeam(player, quest, lang);
        PlayerData data = plugin.store().get(player.getUniqueId());
        if (data.isActive(id)) return new Result(false, Text.tr(lang, "justquests.accept.already_active"));
        if (data.isClaimable(id)) return new Result(false, Text.tr(lang, "justquests.accept.claim_first", id));
        String missing = missingRequirement(data, quest);
        if (missing != null) return new Result(false, locked(lang, data, quest, missing));
        if (data.isCompleted(id)) {
            if (!quest.repeatable()) return new Result(false, Text.tr(lang, "justquests.accept.done"));
            long left = cooldownLeft(data, quest);
            if (left > 0) return new Result(false, Text.tr(lang, "justquests.accept.cooldown", duration(lang, left)));
        }
        BaseComponent denied = plugin.generated().claim(id, player.getUniqueId(), lang);
        if (denied != null) return new Result(false, denied);
        data.accept(id);
        if (data.pinned == null) data.pinned = id;
        plugin.store().markDirty(player.getUniqueId());
        plugin.tracker().update(player);
        return new Result(true, Text.tr(lang, "justquests.accept.ok", quest.title().get(lang)));
    }

    /** A team quest is taken for the whole team; the others hear about it. */
    private Result acceptTeam(Player player, Quest quest, String lang) {
        String id = quest.id();
        TeamRef team = plugin.teams().of(player.getUniqueId());
        if (team == null) return new Result(false, Text.tr(lang, "justquests.team.none"));
        PlayerData data = plugin.store().get(player.getUniqueId());
        PlayerData teamData = plugin.teams().progress(team);
        if (teamData.isActive(id)) return new Result(false, Text.tr(lang, "justquests.accept.already_active"));
        if (data.isClaimable(id)) return new Result(false, Text.tr(lang, "justquests.accept.claim_first", id));
        String missing = missingRequirement(data, quest);
        if (missing != null) return new Result(false, locked(lang, data, quest, missing));
        PlayerData view = plugin.view(player.getUniqueId());
        if (view != null && view.isCompleted(id)) {
            if (!quest.repeatable()) return new Result(false, Text.tr(lang, "justquests.accept.done"));
            long left = cooldownLeft(view, quest);
            if (left > 0) return new Result(false, Text.tr(lang, "justquests.accept.cooldown", duration(lang, left)));
        }
        teamData.accept(id);
        plugin.teams().markDirty(team);
        if (data.pinned == null) data.pinned = id;
        plugin.store().markDirty(player.getUniqueId());
        for (Player member : plugin.teams().onlineMembers(team)) {
            if (!member.equals(player)) {
                member.spigot().sendMessage(Text.tr(Lang.of(member), "justquests.team.accepted", player.getName(),
                    quest.title().get(Lang.of(member))));
            }
            plugin.tracker().update(member);
        }
        plugin.tracker().update(player);
        return new Result(true, Text.tr(lang, "justquests.accept.ok", quest.title().get(lang)));
    }

    public Result abandon(Player player, String id) {
        String lang = Lang.of(player);
        PlayerData data = plugin.store().peek(player.getUniqueId());
        if (data == null || !data.isActive(id)) return abandonTeam(player, id, lang);
        data.abandon(id);
        if (id.equals(data.pinned)) data.pinned = null;
        plugin.generated().abandoned(id, player.getUniqueId());
        plugin.store().markDirty(player.getUniqueId());
        plugin.tracker().update(player);
        return new Result(true, Text.tr(lang, "justquests.abandon.ok", title(id, lang)));
    }

    /** Any member may give up a team quest; it ends for the whole team. */
    private Result abandonTeam(Player player, String id, String lang) {
        TeamRef team = plugin.teams().of(player.getUniqueId());
        PlayerData teamData = plugin.teams().peek(team);
        if (teamData == null || !teamData.isActive(id)) return new Result(false, Text.tr(lang, "justquests.abandon.not_active"));
        teamData.abandon(id);
        plugin.teams().markDirty(team);
        for (Player member : plugin.teams().onlineMembers(team)) {
            PlayerData d = plugin.store().peek(member.getUniqueId());
            if (d != null && id.equals(d.pinned)) {
                d.pinned = null;
                plugin.store().markDirty(member.getUniqueId());
            }
            if (!member.equals(player)) {
                member.spigot().sendMessage(Text.tr(Lang.of(member), "justquests.team.abandoned", player.getName(),
                    title(id, Lang.of(member))));
            }
            plugin.tracker().update(member);
        }
        plugin.tracker().update(player);
        return new Result(true, Text.tr(lang, "justquests.abandon.ok", title(id, lang)));
    }

    /** Claims one quest's rewards; {@code pick} is the 1-based option of a choice (0 = none yet). */
    public Result claimOne(Player player, String id, int pick) {
        String lang = Lang.of(player);
        PlayerData data = plugin.store().peek(player.getUniqueId());
        Quest quest = plugin.quests().get(id);
        if (data == null || quest == null || !data.isClaimable(id)) {
            return new Result(false, Text.tr(lang, "justquests.rewards.nothing"));
        }
        Reward.Choice choice = Reward.Choice.of(quest);
        if (choice != null && (pick < 1 || pick > choice.options().size())) return new Result(false, null);
        claim(player, data, id, pick - 1);
        plugin.tracker().update(player);
        return new Result(true, Text.tr(lang, "justquests.rewards.claimed", quest.title().get(lang)));
    }

    /** "2h 5m" or "3m". */
    public static BaseComponent duration(String lang, long ms) {
        long min = ms / 60_000L;
        long h = min / 60, m = min % 60;
        return h > 0 ? Text.tr(lang, "justquests.time.hours_minutes", h, m) : Text.tr(lang, "justquests.time.minutes", Math.max(1, m));
    }
}
