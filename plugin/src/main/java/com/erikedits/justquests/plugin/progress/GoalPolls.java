package com.erikedits.justquests.plugin.progress;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.compat.Compat;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.quest.Objective;
import com.erikedits.justquests.plugin.quest.Quest;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The objective types of 0.6.0 that are looked at once a second: statistics (only what happens after
 * the quest was taken counts), structures and biomes where the player stands.
 */
final class GoalPolls {
    private final JustQuestsPlugin plugin;
    /** Per player: the statistic values the last poll saw. */
    private final Map<UUID, Map<Statistic, Integer>> seen = new HashMap<>();

    GoalPolls(JustQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    void tick(Player player, PlayerData view) {
        Map<Statistic, Integer> units = new HashMap<>();
        boolean structures = false, biomes = false;
        for (String id : view.active.keySet()) {
            Quest quest = plugin.quests().get(id);
            if (quest == null) continue;
            for (Objective o : quest.objectives()) {
                if (o instanceof Objective.Stat st) {
                    Statistic s = st.statistic();
                    if (s != null) units.put(s, st.unit());
                } else if (o instanceof Objective.FindStructure) {
                    structures = true;
                } else if (o instanceof Objective.FindBiome) {
                    biomes = true;
                }
            }
        }
        stats(player, units);
        Location at = player.getLocation();
        if (biomes) {
            String biome = Compat.key(at.getBlock().getBiome()).toString();
            org.bukkit.World.Environment env = at.getWorld().getEnvironment();
            plugin.progress().advance(player, biome, o -> o instanceof Objective.FindBiome f && f.accepts(biome, env) ? 1 : 0);
        }
        if (structures) {
            // each structure by its id and where it starts, so three villages are three
            for (String[] s : Compat.structuresAt(at)) {
                String id = s[0];
                plugin.progress().advance(player, id + "@" + s[1], o -> o instanceof Objective.FindStructure f && f.accepts(id) ? 1 : 0);
            }
        }
    }

    /** Whole units (blocks, minutes) gained since the last poll; the rest waits for the next one. */
    private void stats(Player player, Map<Statistic, Integer> units) {
        if (units.isEmpty()) {
            seen.remove(player.getUniqueId());
            return;
        }
        Map<Statistic, Integer> old = seen.getOrDefault(player.getUniqueId(), Map.of());
        Map<Statistic, Integer> next = new HashMap<>();
        Map<Statistic, Integer> gained = new HashMap<>();
        for (Map.Entry<Statistic, Integer> e : units.entrySet()) {
            Statistic s = e.getKey();
            int unit = e.getValue();
            int now = player.getStatistic(s);
            Integer was = old.get(s);
            int n = was == null || now <= was ? 0 : (now - was) / unit;
            next.put(s, was == null || now < was ? now : was + n * unit);
            if (n > 0) gained.put(s, n);
        }
        seen.put(player.getUniqueId(), next);
        for (Map.Entry<Statistic, Integer> e : gained.entrySet()) {
            Statistic stat = e.getKey();
            int n = e.getValue();
            // by the statistic itself: Bukkit's names are not always the game's ids
            plugin.progress().advance(player, o -> o instanceof Objective.Stat st && st.statistic() == stat ? n : 0);
        }
    }

    void forget(UUID player) {
        seen.remove(player);
    }
}
