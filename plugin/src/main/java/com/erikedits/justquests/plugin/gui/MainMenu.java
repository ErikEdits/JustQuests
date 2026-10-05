package com.erikedits.justquests.plugin.gui;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.progress.QuestStatus;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.text.Text;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The quest book's first page: your stats, the quests by status (rewards ready, active, ...) and by
 * category, and the boss bar switch.
 */
public final class MainMenu extends Menu {
    private static final int[] CATEGORY_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    public MainMenu(JustQuestsPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return legacy("justquests.book.title");
    }

    @Override
    protected void render() {
        frame();
        plugin.generator().ensurePersonal(player);
        PlayerData data = plugin.view(player.getUniqueId());
        Collection<Quest> quests = plugin.visibleQuests(player);
        Map<QuestStatus, Integer> byStatus = new EnumMap<>(QuestStatus.class);
        Map<String, List<Quest>> byCategory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<Quest> personal = new ArrayList<>();
        List<Quest> weekly = new ArrayList<>();
        for (Quest q : quests) {
            byStatus.merge(QuestStatus.of(plugin, player.getUniqueId(), data, q), 1, Integer::sum);
            if (q.category().equals("personal")) personal.add(q);
            else if (q.category().equals("weekly")) weekly.add(q);
            else byCategory.computeIfAbsent(q.category(), k -> new ArrayList<>()).add(q);
        }

        set(4, stats(data, quests));
        if (plugin.settings().personal && plugin.generator().running()) {
            List<BaseComponent> lore = new ArrayList<>();
            lore.add(tr("justquests.plugin.gui.personal_lore"));
            lore.add(tr("justquests.plugin.gui.done_of", done(data, personal), personal.size()));
            BaseComponent streak = plugin.generator().streakLine(lang, player.getUniqueId());
            if (streak != null) lore.add(streak);
            lore.add(Text.lit(""));
            lore.add(tr("justquests.plugin.gui.click_open"));
            set(2, item(Material.CLOCK, Math.max(1, personal.size()), Text.lit("§b" + plugin.categoryName(lang, "personal")), lore,
                    hasWork(data, personal)),
                click -> go(new QuestListMenu(plugin, player, QuestListMenu.Filter.category("personal"), 0)));
        }
        if (plugin.settings().weekly && plugin.generator().running()) {
            List<BaseComponent> lore = new ArrayList<>();
            lore.add(tr("justquests.plugin.gui.weekly_lore"));
            lore.add(tr("justquests.plugin.gui.done_of", done(data, weekly), weekly.size()));
            lore.add(Text.lit(""));
            lore.add(tr("justquests.plugin.gui.click_open"));
            set(3, item(Material.NETHER_STAR, Math.max(1, weekly.size()), Text.lit("§d" + plugin.categoryName(lang, "weekly")), lore,
                    false),
                click -> go(new QuestListMenu(plugin, player, QuestListMenu.Filter.category("weekly"), 0)));
        }
        if (plugin.generator().goalActive()) {
            List<BaseComponent> lore = new ArrayList<>(plugin.generator().goalLines(lang, player.getUniqueId()));
            set(5, item(plugin.generator().goalIcon(), 1, Text.lit("§6" + legacy("justquests.plugin.goal.title")
                    + " §7(" + plugin.generator().goalPercent() + "%)"), lore, plugin.generator().goalDone()),
                click -> plugin.generator().goalLines(lang, player.getUniqueId()).forEach(l -> player.spigot().sendMessage(l)));
        }

        int slot = 10;
        for (QuestStatus s : new QuestStatus[]{QuestStatus.CLAIM, QuestStatus.ACTIVE, QuestStatus.AVAILABLE, QuestStatus.LOCKED, QuestStatus.COMPLETED}) {
            int n = byStatus.getOrDefault(s, 0);
            Material icon = switch (s) {
                case CLAIM -> Material.CHEST;
                case ACTIVE -> Material.WRITABLE_BOOK;
                case AVAILABLE -> Material.MAP;
                case LOCKED -> Material.IRON_BARS;
                case COMPLETED -> Material.WRITTEN_BOOK;
            };
            List<BaseComponent> lore = List.of(tr("justquests.plugin.gui.quests", n), Text.lit(""), tr("justquests.plugin.gui.click_open"));
            set(slot, item(icon, Math.max(1, n), Text.lit(s.color + Text.legacy(lang, s.key)), lore, s == QuestStatus.CLAIM && n > 0),
                click -> go(new QuestListMenu(plugin, player, QuestListMenu.Filter.status(s), 0)));
            slot++;
        }
        set(16, item(Material.BOOKSHELF, Math.max(1, quests.size()), Text.lit("§f" + legacy("justquests.plugin.gui.all")),
                List.of(tr("justquests.plugin.gui.quests", quests.size()), Text.lit(""), tr("justquests.plugin.gui.click_open")), false),
            click -> go(new QuestListMenu(plugin, player, QuestListMenu.Filter.all(), 0)));

        int i = 0;
        for (Map.Entry<String, List<Quest>> e : byCategory.entrySet()) {
            if (i >= CATEGORY_SLOTS.length) break;
            String category = e.getKey();
            List<Quest> list = e.getValue();
            list.sort(QuestListMenu.ORDER);
            int done = 0;
            for (Quest q : list) {
                if (data != null && data.isCompleted(q.id())) done++;
            }
            List<BaseComponent> lore = List.of(tr("justquests.plugin.gui.quests", list.size()),
                tr("justquests.plugin.gui.done_of", done, list.size()), Text.lit(""), tr("justquests.plugin.gui.click_open"));
            set(CATEGORY_SLOTS[i++], item(list.get(0).iconOrGoal(), 1, Text.lit("§6" + plugin.categoryName(lang, category)), lore, false),
                click -> go(new QuestListMenu(plugin, player, QuestListMenu.Filter.category(category), 0)));
        }
        if (quests.isEmpty()) set(22, item(Material.BARRIER, tr("justquests.book.empty"), List.of()));

        if (plugin.settings().bossbar) {
            boolean on = data == null || data.bossbar;
            set(48, item(on ? Material.LIME_DYE : Material.GRAY_DYE, 1,
                    tr(on ? "justquests.plugin.gui.bossbar_on" : "justquests.plugin.gui.bossbar_off"),
                    List.of(tr("justquests.plugin.gui.bossbar_hint")), false),
                click -> {
                    PlayerData d = plugin.store().get(player.getUniqueId());
                    d.bossbar = !d.bossbar;
                    plugin.store().markDirty(player.getUniqueId());
                    plugin.tracker().update(player);
                    redraw();
                });
        }
        set(49, item(Material.BARRIER, tr("justquests.plugin.gui.close"), List.of()), click -> player.closeInventory());
    }

    private static int done(PlayerData data, List<Quest> list) {
        int n = 0;
        for (Quest q : list) {
            if (data != null && (data.isCompleted(q.id()) || data.isClaimable(q.id()))) n++;
        }
        return n;
    }

    /**
     * How many of these quests the player has completed - not all completions, which include past
     * personal, weekly and board quests and would read "57 of 40".
     */
    public static int completedOf(PlayerData data, Collection<Quest> quests) {
        int n = 0;
        for (Quest q : quests) {
            if (data != null && data.isCompleted(q.id())) n++;
        }
        return n;
    }

    /** Something to take or to claim among the quests. */
    private static boolean hasWork(PlayerData data, List<Quest> list) {
        for (Quest q : list) {
            if (data == null || data.isClaimable(q.id()) || (!data.isActive(q.id()) && !data.isCompleted(q.id()))) return true;
        }
        return false;
    }

    private org.bukkit.inventory.ItemStack stats(PlayerData data, Collection<Quest> quests) {
        int total = quests.size();
        int completed = completedOf(data, quests);
        int active = data == null ? 0 : data.active.size();
        int pct = total > 0 ? Math.min(100, completed * 100 / total) : 0;
        List<BaseComponent> lore = new ArrayList<>();
        lore.add(Text.lit("§7" + legacy("justquests.book.stats.completed", completed, total, pct)));
        lore.add(Text.lit("§7" + legacy("justquests.book.stats.active", active)));
        int[] rank = plugin.rank(player.getUniqueId());
        if (rank != null) lore.add(Text.lit("§7" + legacy("justquests.book.stats.rank", rank[0], rank[1])));
        BaseComponent streak = plugin.generator().streakLine(lang, player.getUniqueId());
        if (streak != null) lore.add(streak);
        return item(Material.KNOWLEDGE_BOOK, 1, Text.lit("§e" + legacy("justquests.book.stats.title")), lore, false);
    }
}
