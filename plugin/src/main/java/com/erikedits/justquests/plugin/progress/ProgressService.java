package com.erikedits.justquests.plugin.progress;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.quest.Objective;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.quest.Reward;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public void advance(Player player, Test test) {
        plugin.generated().progress(player, test);
        PlayerData data = plugin.store().peek(player.getUniqueId());
        if (data == null || data.active.isEmpty()) return;
        String lang = Lang.of(player);
        boolean changed = false;
        BaseComponent toast = null;
        List<String> completing = new ArrayList<>();

        for (Map.Entry<String, Map<Integer, Integer>> entry : data.active.entrySet()) {
            Quest quest = plugin.quests().get(entry.getKey());
            if (quest == null) continue;
            Map<Integer, Integer> progress = entry.getValue();
            boolean all = true, any = false;
            for (int i = 0; i < quest.objectives().size(); i++) {
                Objective obj = quest.objectives().get(i);
                int add = test.amount(obj);
                int have = progress.getOrDefault(i, 0);
                if (add > 0 && have < obj.requiredCount()) {
                    have = Math.min(obj.requiredCount(), have + add);
                    progress.put(i, have);
                    changed = true;
                    if (obj.requiredCount() > 1) {
                        toast = Text.tr(lang, "justquests.plugin.progress", obj.display(lang), have, obj.requiredCount());
                    }
                }
                if (have >= obj.requiredCount()) any = true;
                else all = false;
            }
            if (quest.any() ? any : all) completing.add(entry.getKey());
        }

        for (String id : completing) {
            Quest quest = plugin.quests().get(id);
            if (quest == null) continue;
            boolean claim = finish(player, data, id, quest);
            announce(player, quest, claim);
            toast = null;
            changed = true;
        }

        if (changed) {
            plugin.store().markDirty(player.getUniqueId());
            if (toast != null) player.spigot().sendMessage(ChatMessageType.ACTION_BAR, toast);
            plugin.tracker().update(player);
        }
    }

    private void announce(Player player, Quest quest, boolean claim) {
        String lang = Lang.of(player);
        String title = quest.title().get(lang);
        TextComponent chat = new TextComponent("");
        chat.addExtra(Text.tr(lang, "justquests.complete.chat", title));
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
        data.complete(id);
        if (id.equals(data.pinned)) data.pinned = null;
        plugin.generated().completed(id, player.getUniqueId());
        if (plugin.settings().claimRewards || Reward.Choice.of(quest) != null) {
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
        if (quest == null) return false;
        grant(player, quest, pick);
        plugin.store().markDirty(player.getUniqueId());
        return true;
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

    /** The prerequisite the player still misses, or null. */
    public String missingRequirement(PlayerData data, Quest quest) {
        for (String req : quest.requires()) {
            if (data == null || !data.isCompleted(req)) return req;
        }
        return null;
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
        PlayerData data = plugin.store().get(player.getUniqueId());
        if (data.isActive(id)) return new Result(false, Text.tr(lang, "justquests.accept.already_active"));
        if (data.isClaimable(id)) return new Result(false, Text.tr(lang, "justquests.accept.claim_first", id));
        String missing = missingRequirement(data, quest);
        if (missing != null) return new Result(false, Text.tr(lang, "justquests.accept.locked", title(missing, lang)));
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

    public Result abandon(Player player, String id) {
        String lang = Lang.of(player);
        PlayerData data = plugin.store().peek(player.getUniqueId());
        if (data == null || !data.isActive(id)) return new Result(false, Text.tr(lang, "justquests.abandon.not_active"));
        data.abandon(id);
        if (id.equals(data.pinned)) data.pinned = null;
        plugin.generated().abandoned(id, player.getUniqueId());
        plugin.store().markDirty(player.getUniqueId());
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
