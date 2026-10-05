package com.erikedits.justquests.plugin.gui;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.progress.ProgressService;
import com.erikedits.justquests.plugin.progress.QuestStatus;
import com.erikedits.justquests.plugin.quest.Objective;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.quest.Reward;
import com.erikedits.justquests.plugin.text.Text;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * One quest: its goals with progress, its rewards (a choice can be picked here) and the button
 * that fits - accept, abandon (click twice), claim, or why it can't be taken.
 */
public final class QuestMenu extends Menu {
    private static final int[] GOAL_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int[] REWARD_SLOTS = {37, 38, 39, 40, 41, 42, 43};

    private final String id;
    private final Menu back;
    private int pick = -1;
    private boolean confirmAbandon;

    public QuestMenu(JustQuestsPlugin plugin, Player player, String id, Menu back) {
        super(plugin, player);
        this.id = id;
        this.back = back;
    }

    @Override
    protected String title() {
        Quest q = plugin.quests().get(id);
        String t = q == null ? id : q.title().get(lang);
        return t.length() > 32 ? t.substring(0, 31) + "…" : t;
    }

    @Override
    protected void render() {
        frame();
        set(45, item(Material.ARROW, tr("justquests.plugin.gui.back"), List.of()),
            click -> go(back != null ? reopen(back) : new MainMenu(plugin, player)));
        Quest q = plugin.quests().get(id);
        if (q == null || !plugin.canSee(player, q)) {
            set(22, item(Material.BARRIER, tr("justquests.error.unknown_quest", id), List.of()));
            return;
        }
        PlayerData data = plugin.view(player.getUniqueId());
        QuestStatus status = QuestStatus.of(plugin, player.getUniqueId(), data, q);
        set(4, QuestListMenu.questItem(plugin, player, lang, data, q, false));

        if (status == QuestStatus.LOCKED) {
            String missing = plugin.progress().missingRequirement(data, q);
            List<BaseComponent> lore = new ArrayList<>();
            if (missing != null) lore.add(Text.lit("§c" + legacy("justquests.book.needs", plugin.progress().title(missing, lang))));
            String gen = plugin.generated().bookLine(id, player.getUniqueId(), lang);
            if (gen != null) lore.add(Text.lit("§7" + gen));
            set(49, item(Material.IRON_BARS, Text.lit("§8" + legacy("justquests.book.locked")), lore));
            return;
        }

        boolean active = data != null && data.isActive(id);
        for (int i = 0; i < q.objectives().size() && i < GOAL_SLOTS.length; i++) {
            Objective o = q.objectives().get(i);
            int have = active ? Math.min(data.progress(id, i), o.requiredCount()) : 0;
            boolean done = active && have >= o.requiredCount();
            List<BaseComponent> lore = new ArrayList<>();
            if (active) {
                lore.add(Text.lit((done ? "§a✓ " : "§e") + have + "/" + o.requiredCount()));
                lore.add(Text.lit(bar(have, o.requiredCount())));
            }
            BaseComponent name = Text.lit(done ? "§a" : "§f");
            name.addExtra(o.display(lang));
            set(GOAL_SLOTS[i], item(o.icon(), 1, name, lore, done));
        }

        Reward.Choice choice = Reward.Choice.of(q);
        boolean claimable = status == QuestStatus.CLAIM;
        int slot = 0;
        for (Reward r : q.rewards()) {
            if (r == choice) {
                for (int i = 0; i < choice.options().size() && slot < REWARD_SLOTS.length; i++) {
                    Reward option = choice.options().get(i);
                    boolean picked = pick == i;
                    List<BaseComponent> lore = new ArrayList<>();
                    lore.add(Text.lit("§6" + legacy("justquests.book.choose_one")));
                    if (claimable) lore.add(tr(picked ? "justquests.plugin.gui.picked" : "justquests.plugin.gui.click_pick"));
                    BaseComponent name = Text.lit(picked ? "§a" : "§f");
                    name.addExtra(option.display(lang));
                    final int index = i;
                    if (claimable) {
                        set(REWARD_SLOTS[slot++], item(option.icon(), option.iconCount(), name, lore, picked), click -> {
                            pick = index;
                            redraw();
                        });
                    } else {
                        set(REWARD_SLOTS[slot++], item(option.icon(), option.iconCount(), name, lore, false));
                    }
                }
            } else if (slot < REWARD_SLOTS.length) {
                BaseComponent name = Text.lit("§a");
                name.addExtra(r.display(lang));
                set(REWARD_SLOTS[slot++], item(r.icon(), r.iconCount(), name, List.of(), false));
            }
        }

        switch (status) {
            case AVAILABLE -> set(49, item(Material.EMERALD, 1, Text.lit("§a" + legacy("justquests.book.accept")),
                    List.of(tr("justquests.plugin.gui.click_accept")), true),
                click -> act(plugin.progress().accept(player, id)));
            case ACTIVE -> {
                set(49, item(confirmAbandon ? Material.TNT : Material.RED_DYE, 1,
                        Text.lit("§c" + legacy("justquests.book.abandon")),
                        List.of(tr(confirmAbandon ? "justquests.plugin.gui.confirm_abandon" : "justquests.plugin.gui.click_abandon")), confirmAbandon),
                    click -> {
                        if (!confirmAbandon) {
                            confirmAbandon = true;
                            redraw();
                        } else {
                            confirmAbandon = false;
                            act(plugin.progress().abandon(player, id));
                        }
                    });
                boolean pinned = id.equals(plugin.tracker().shown(data));
                if (plugin.settings().bossbar) {
                    set(53, item(pinned ? Material.LIGHT : Material.GLOWSTONE_DUST, 1,
                            tr(pinned ? "justquests.plugin.gui.pinned" : "justquests.plugin.gui.pin"), List.of(), pinned),
                        click -> {
                            PlayerData d = plugin.store().get(player.getUniqueId());
                            d.pinned = id;
                            d.bossbar = true;
                            plugin.store().markDirty(player.getUniqueId());
                            plugin.tracker().update(player);
                            redraw();
                        });
                }
            }
            case CLAIM -> {
                boolean needsPick = choice != null && pick < 0;
                set(49, item(Material.CHEST, 1, Text.lit("§6" + legacy("justquests.book.claim")),
                        List.of(tr(needsPick ? "justquests.plugin.gui.pick_first" : "justquests.plugin.gui.click_claim")), !needsPick),
                    click -> {
                        if (choice != null && pick < 0) return;
                        act(plugin.progress().claimOne(player, id, choice == null ? 0 : pick + 1));
                    });
            }
            case COMPLETED -> {
                long left = plugin.progress().cooldownLeft(data, q);
                BaseComponent label = left > 0
                    ? Text.lit("§7" + legacy("justquests.book.again_in", ProgressService.duration(lang, left)))
                    : Text.lit("§a" + legacy("justquests.book.rewards_received"));
                set(49, item(Material.WRITTEN_BOOK, label, List.of()));
            }
            default -> { }
        }
    }

    private void act(ProgressService.Result result) {
        if (result.message() != null) player.spigot().sendMessage(result.message());
        pick = -1;
        redraw();
    }

    /** "■■■■■□□□□□" for the progress. */
    private static String bar(int have, int need) {
        int filled = need <= 0 ? 10 : (int) Math.round(10.0 * have / need);
        return "§a" + "■".repeat(filled) + "§8" + "■".repeat(10 - filled);
    }

    /** A fresh copy of the previous page, so it shows the new state. */
    private Menu reopen(Menu m) {
        if (m instanceof QuestListMenu list) return list.copy();
        return new MainMenu(plugin, player);
    }
}
