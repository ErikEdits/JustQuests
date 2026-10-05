package com.erikedits.justquests.plugin.gui;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.progress.QuestStatus;
import com.erikedits.justquests.plugin.quest.Objective;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.quest.Reward;
import com.erikedits.justquests.plugin.progress.ProgressService;
import com.erikedits.justquests.plugin.team.Teams.TeamRef;
import com.erikedits.justquests.plugin.text.Items;
import com.erikedits.justquests.plugin.text.Text;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A page of quests (by status, by category or all), 45 per page; a click opens the quest. */
public final class QuestListMenu extends Menu {
    static final Comparator<Quest> ORDER = Comparator
        .comparing(Quest::category, String.CASE_INSENSITIVE_ORDER)
        .thenComparingInt(Quest::sort)
        .thenComparing(Quest::id);

    /** Which quests the page lists. */
    public record Filter(QuestStatus status, String category) {
        static Filter all() { return new Filter(null, null); }
        static Filter status(QuestStatus s) { return new Filter(s, null); }
        static Filter category(String c) { return new Filter(null, c); }
    }

    private final Filter filter;
    private final int page;

    public QuestListMenu(JustQuestsPlugin plugin, Player player, Filter filter, int page) {
        super(plugin, player);
        this.filter = filter;
        this.page = page;
    }

    /** The same page again, drawn fresh. */
    QuestListMenu copy() {
        return new QuestListMenu(plugin, player, filter, page);
    }

    private String name() {
        if (filter.status() != null) return Text.legacy(lang, filter.status().key);
        if (filter.category() != null) return plugin.categoryName(lang, filter.category());
        return legacy("justquests.plugin.gui.all");
    }

    @Override
    protected String title() {
        return legacy("justquests.plugin.gui.title_list", name());
    }

    @Override
    protected void render() {
        frame();
        PlayerData data = plugin.view(player.getUniqueId());
        boolean hideDone = plugin.hidesCompleted(player.getUniqueId()) && filter.status() != QuestStatus.COMPLETED;
        List<Quest> list = new ArrayList<>();
        for (Quest q : plugin.visibleQuests(player)) {
            QuestStatus s = QuestStatus.of(plugin, player.getUniqueId(), data, q);
            if (filter.status() != null && s != filter.status()) continue;
            if (filter.category() != null && !q.category().equalsIgnoreCase(filter.category())) continue;
            if (hideDone && s == QuestStatus.COMPLETED) continue;
            list.add(q);
        }
        list.sort(Comparator.<Quest, QuestStatus>comparing(q -> QuestStatus.of(plugin, player.getUniqueId(), data, q)).thenComparing(ORDER));

        int pages = Math.max(1, (list.size() + 44) / 45);
        int p = Math.min(page, pages - 1);
        for (int i = 0; i < 45 && p * 45 + i < list.size(); i++) {
            Quest q = list.get(p * 45 + i);
            set(i, questItem(plugin, player, lang, data, q, true), click -> {
                if (click.isShiftClick() && QuestStatus.of(plugin, player.getUniqueId(), plugin.view(player.getUniqueId()), q) == QuestStatus.AVAILABLE) {
                    player.spigot().sendMessage(plugin.progress().accept(player, q.id()).message());
                    redraw();
                } else {
                    go(new QuestMenu(plugin, player, q.id(), this));
                }
            });
        }
        if (list.isEmpty()) set(22, item(Material.BARRIER, tr("justquests.book.nothing"), List.of()));

        set(45, item(Material.ARROW, tr("justquests.plugin.gui.back"), List.of()), click -> go(new MainMenu(plugin, player)));
        if (p > 0) {
            set(48, item(Material.SPECTRAL_ARROW, tr("justquests.plugin.gui.prev"), List.of()),
                click -> go(new QuestListMenu(plugin, player, filter, p - 1)));
        }
        set(49, item(Material.PAPER, tr("justquests.plugin.gui.page", p + 1, pages), List.of()));
        if (p < pages - 1) {
            set(50, item(Material.SPECTRAL_ARROW, tr("justquests.plugin.gui.next"), List.of()),
                click -> go(new QuestListMenu(plugin, player, filter, p + 1)));
        }
        if (filter.status() != QuestStatus.COMPLETED) {
            boolean hide = plugin.hidesCompleted(player.getUniqueId());
            set(53, item(hide ? Material.ENDER_EYE : Material.ENDER_PEARL,
                    tr(hide ? "justquests.book.hint.show_completed" : "justquests.book.hint.hide_completed"), List.of()),
                click -> {
                    plugin.setHidesCompleted(player.getUniqueId(), !hide);
                    redraw();
                });
        }
    }

    /** A quest as a book item: status, description, goals with progress and rewards in the lore. */
    static ItemStack questItem(JustQuestsPlugin plugin, Player player, String lang, PlayerData data, Quest q, boolean hint) {
        QuestStatus s = QuestStatus.of(plugin, player.getUniqueId(), data, q);
        List<BaseComponent> lore = new ArrayList<>();
        lore.add(Text.lit(s.color + Text.legacy(lang, s.key) + " §8· §7" + plugin.categoryName(lang, q.category())));
        String gen = plugin.generated().bookLine(q.id(), player.getUniqueId(), lang);
        if (gen != null) lore.add(Text.lit("§8" + gen));
        if (q.team()) {
            TeamRef team = plugin.teams().of(player.getUniqueId());
            lore.add(team == null ? Text.tr(lang, "justquests.team.line_none") : Text.tr(lang, "justquests.team.line", team.name()));
        }
        for (String line : wrap(q.description().get(lang), 38)) lore.add(Text.lit("§7§o" + line));

        String missing = plugin.progress().missingRequirement(data, q);
        if (s == QuestStatus.LOCKED && missing != null) {
            lore.add(Text.lit(""));
            lore.add(Text.lit("§c" + Text.legacy(lang, "justquests.book.needs", plugin.progress().title(missing, lang))));
            return Items.make(Material.IRON_BARS, 1, Text.lit("§8" + q.title().get(lang)), lore, false);
        }

        lore.add(Text.lit(""));
        lore.add(Text.lit("§6" + Text.legacy(lang, "justquests.book.objectives")));
        for (int i = 0; i < q.objectives().size(); i++) lore.add(goalLine(lang, data, q, i));
        if (!q.rewards().isEmpty()) {
            lore.add(Text.lit("§6" + Text.legacy(lang, "justquests.book.rewards")));
            for (Reward r : q.rewards()) {
                TextComponent line = new TextComponent("");
                line.addExtra(Text.lit(" §a"));
                line.addExtra(r.display(lang));
                lore.add(line);
            }
        }
        if (s == QuestStatus.COMPLETED && q.repeatable()) {
            long left = plugin.progress().cooldownLeft(data, q);
            if (left > 0) lore.add(Text.lit("§8" + Text.legacy(lang, "justquests.book.again_in",
                ProgressService.duration(lang, left))));
        }
        if (hint) {
            lore.add(Text.lit(""));
            lore.add(Text.tr(lang, "justquests.plugin.gui.click_details"));
            if (s == QuestStatus.AVAILABLE) lore.add(Text.tr(lang, "justquests.plugin.gui.shift_accept"));
        }
        return Items.make(q.iconOrGoal(), 1, Text.lit(s.color + q.title().get(lang)), lore, s == QuestStatus.CLAIM);
    }

    /** " ✓ goal" when done, " 3/16 goal" while active, " • goal" otherwise. */
    static BaseComponent goalLine(String lang, PlayerData data, Quest q, int i) {
        Objective o = q.objectives().get(i);
        TextComponent line = new TextComponent("");
        boolean active = data != null && data.isActive(q.id());
        int have = active ? Math.min(data.progress(q.id(), i), o.requiredCount()) : 0;
        if (active && have >= o.requiredCount()) line.addExtra(Text.lit(" §a✓ §7"));
        else if (active) line.addExtra(Text.lit(" §e" + have + "/" + o.requiredCount() + " §f"));
        else line.addExtra(Text.lit(" §8• §f"));
        line.addExtra(o.display(lang));
        return line;
    }
}
