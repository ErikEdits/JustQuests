package com.erikedits.justquests.plugin.hook;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.gen.PluginGenerator;
import com.erikedits.justquests.plugin.progress.QuestStatus;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.track.Tracker;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * %justquests_...% for scoreboards, tab lists and chat when the server has PlaceholderAPI. Only
 * this class touches PlaceholderAPI, and only after the plugin found it.
 *
 * <p>Player: completed, active, claimable, available, rank, tracked, tracked_progress, streak,
 * goal_yours. Server goal: goal, goal_progress, goal_percent, goal_left. Texts are in the player's
 * language (English for offline players); unknown values are empty.
 */
public final class Placeholders extends PlaceholderExpansion {
    private final JustQuestsPlugin plugin;

    private Placeholders(JustQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Registers the placeholders; call only when PlaceholderAPI is enabled. */
    public static void hook(JustQuestsPlugin plugin) {
        new Placeholders(plugin).register();
    }

    @Override
    public String getIdentifier() {
        return "justquests";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /** Stays registered through /papi reload: the plugin is not an expansion download. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        try {
            return value(player, params.toLowerCase(Locale.ROOT));
        } catch (RuntimeException e) {
            // scoreboard plugins may ask from another thread while the server changes the data
            return "";
        }
    }

    private String value(OfflinePlayer player, String key) {
        Player online = player == null ? null : player.getPlayer();
        String lang = online != null ? Lang.of(online) : Lang.DEFAULT;
        PluginGenerator gen = plugin.generator();
        switch (key) {
            case "goal" -> {
                BaseComponent goal = gen.goalObjective(lang);
                return goal == null ? "" : goal.toLegacyText();
            }
            case "goal_progress" -> {
                return gen.goalActive() ? gen.goalTotal() + "/" + gen.goalRequired() : "";
            }
            case "goal_percent" -> {
                return gen.goalActive() ? String.valueOf(gen.goalPercent()) : "";
            }
            case "goal_left" -> {
                return gen.goalActive() && !gen.goalDone() ? gen.goalLeft(lang).toLegacyText() : "";
            }
            default -> { }
        }
        if (player == null) return "";
        PlayerData data = plugin.view(player.getUniqueId());
        return switch (key) {
            case "completed" -> String.valueOf(data == null ? 0 : data.completed.size());
            case "active" -> String.valueOf(data == null ? 0 : data.active.size());
            case "claimable" -> String.valueOf(data == null ? 0 : data.pendingClaim.size());
            case "available" -> online == null ? "" : String.valueOf(available(online, data));
            case "rank" -> {
                int[] rank = plugin.rank(player.getUniqueId());
                yield rank == null ? "" : String.valueOf(rank[0]);
            }
            case "tracked" -> {
                Quest q = tracked(data);
                yield q == null ? "" : q.title().get(lang);
            }
            case "tracked_progress" -> {
                Quest q = tracked(data);
                yield q == null ? "" : Tracker.count(q, data);
            }
            case "streak" -> String.valueOf(gen.streak(player.getUniqueId()));
            case "goal_yours" -> gen.goalActive() ? String.valueOf(gen.goalContribution(player.getUniqueId())) : "";
            default -> null;   // not one of ours: PlaceholderAPI leaves the text as it is
        };
    }

    private long available(Player player, PlayerData data) {
        return plugin.visibleQuests(player).stream()
            .filter(q -> QuestStatus.of(plugin, player.getUniqueId(), data, q) == QuestStatus.AVAILABLE)
            .count();
    }

    /** The quest the boss bar shows (pinned, else the first active one), or null. */
    private Quest tracked(PlayerData data) {
        String id = plugin.tracker().shown(data);
        return id == null ? null : plugin.quests().get(id);
    }
}
