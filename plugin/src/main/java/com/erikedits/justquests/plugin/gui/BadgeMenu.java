package com.erikedits.justquests.plugin.gui;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.badge.PluginBadges;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.text.Text;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The badge page of the chest-menu quest book (0.6.0): every badge, the earned ones shining; a click on one
 * puts its sign before the player's name. The bottom row: back, show none, and the Discord link.
 */
public final class BadgeMenu extends Menu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43};
    private final int page;

    public BadgeMenu(JustQuestsPlugin plugin, Player player, int page) {
        super(plugin, player);
        this.page = page;
    }

    @Override
    protected String title() {
        return legacy("justquests.book.badges");
    }

    @Override
    protected void render() {
        frame();
        PluginBadges badges = plugin.badges();
        PlayerData data = plugin.store().peek(player.getUniqueId());
        List<PluginBadges.Badge> all = badges.sorted();
        PluginBadges.Badge shown = badges.shown(data);
        long earned = all.stream().filter(b -> data != null && data.badges.containsKey(b.id())).count();
        set(4, item(Material.NAME_TAG, 1, Text.lit("§6" + legacy("justquests.book.badges")),
            List.of(tr("justquests.book.badges_count", earned, all.size())), false));
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        int start = page * SLOTS.length;
        for (int i = 0; i < SLOTS.length && start + i < all.size(); i++) {
            PluginBadges.Badge b = all.get(start + i);
            Long at = data == null ? null : data.badges.get(b.id());
            if (at == null && b.secret()) {
                set(SLOTS[i], item(Material.GRAY_DYE, 1, Text.lit("§8???"), List.of(), false));
                continue;
            }
            List<BaseComponent> lore = new ArrayList<>();
            if (!b.description().isBlank()) lore.add(Text.lit("§7" + b.description().get(lang)));
            if (at != null) {
                lore.add(tr("justquests.book.badge_earned", fmt.format(new Date(at))));
                lore.add(Text.lit(""));
                lore.add(tr(b == shown ? "justquests.book.badge_shown" : "justquests.book.badge_click"));
            } else {
                lore.add(tr("justquests.book.badge_how", b.how().isBlank() ? b.description().get(lang) : b.how().get(lang)));
            }
            String name = (at != null ? b.chatColor().toString() : "§8") + b.sign() + " " + b.name().get(lang);
            set(SLOTS[i], item(at != null ? b.material() : Material.GRAY_DYE, 1, Text.lit(name), lore, b == shown),
                click -> {
                    if (at != null && badges.show(player, b.id())) redraw();
                });
        }
        if (page > 0) set(45, item(Material.ARROW, tr("justquests.plugin.gui.previous"), List.of()), c -> go(new BadgeMenu(plugin, player, page - 1)));
        if (start + SLOTS.length < all.size()) set(53, item(Material.ARROW, tr("justquests.plugin.gui.next"), List.of()), c -> go(new BadgeMenu(plugin, player, page + 1)));
        set(47, item(Material.BARRIER, 1, tr("justquests.plugin.gui.badge_hide"), List.of(), false), c -> {
            badges.show(player, "none");
            redraw();
        });
        set(48, item(Material.ARROW, tr("justquests.plugin.gui.back"), List.of()), c -> go(new MainMenu(plugin, player)));
        String user = plugin.discord().user(player.getUniqueId());
        BaseComponent line = !plugin.settings().discordLink ? tr("justquests.book.discord_off")
            : user != null ? tr("justquests.book.discord_linked", user) : tr("justquests.book.discord_not_linked");
        set(50, item(Material.MAP, 1, line, List.of(Text.lit("§7/quest discord link <code>"), Text.lit("§7/quest discord status")), user != null),
            c -> {
                player.closeInventory();
                com.erikedits.justquests.plugin.text.Chat.send(player, plugin.discord().status(player));
            });
    }
}
