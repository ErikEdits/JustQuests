package com.erikedits.justquests.plugin.track;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.quest.Objective;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The plugin's stand-in for the mod's quest HUD: a boss bar with the pinned quest (or the first
 * active one) and how far it is. When nothing is active but rewards wait, it says so.
 */
public final class Tracker {
    private final JustQuestsPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public Tracker(JustQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    /** The quest the boss bar shows: the pinned one while active, else the first active quest. */
    public String shown(PlayerData data) {
        if (data == null) return null;
        if (data.pinned != null && data.isActive(data.pinned) && plugin.quests().get(data.pinned) != null) return data.pinned;
        for (String id : data.active.keySet()) {
            if (plugin.quests().get(id) != null) return id;
        }
        return null;
    }

    public void update(Player player) {
        PlayerData data = plugin.store().peek(player.getUniqueId());
        if (!plugin.settings().bossbar || data == null || !data.bossbar) {
            remove(player);
            return;
        }
        String lang = Lang.of(player);
        String title = null;
        double fill = 0;
        BarColor color = plugin.settings().bossbarColor;
        String id = shown(data);
        if (id != null) {
            Quest quest = plugin.quests().get(id);
            long need = 0, have = 0;
            double best = 0;
            for (int i = 0; i < quest.objectives().size(); i++) {
                Objective o = quest.objectives().get(i);
                int p = Math.min(data.progress(id, i), o.requiredCount());
                need += o.requiredCount();
                have += p;
                best = Math.max(best, p / (double) o.requiredCount());
            }
            fill = quest.any() ? best : need == 0 ? 0 : have / (double) need;
            title = Text.legacy(lang, "justquests.plugin.bossbar", quest.title().get(lang), count(quest, data));
        } else if (!data.pendingClaim.isEmpty()) {
            title = Text.legacy(lang, "justquests.hud.rewards", data.pendingClaim.size());
            fill = 1;
            color = BarColor.GREEN;
        }
        if (title == null) {
            remove(player);
            return;
        }
        BossBar bar = bars.get(player.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar(title, color, plugin.settings().bossbarStyle);
            bar.addPlayer(player);
            bars.put(player.getUniqueId(), bar);
        }
        bar.setTitle(title);
        bar.setColor(color);
        bar.setStyle(plugin.settings().bossbarStyle);
        bar.setProgress(Math.max(0, Math.min(1, fill)));
        bar.setVisible(true);
    }

    /** "12/32" for a quest with one goal, else "1/3" goals done. */
    public static String count(Quest quest, PlayerData data) {
        String id = quest.id();
        if (quest.objectives().size() == 1) {
            int need = quest.objectives().get(0).requiredCount();
            return Math.min(data.progress(id, 0), need) + "/" + need;
        }
        int done = 0;
        for (int i = 0; i < quest.objectives().size(); i++) {
            if (data.progress(id, i) >= quest.objectives().get(i).requiredCount()) done++;
        }
        return done + "/" + quest.objectives().size();
    }

    public void remove(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) update(p);
    }

    public void clear() {
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
    }
}
