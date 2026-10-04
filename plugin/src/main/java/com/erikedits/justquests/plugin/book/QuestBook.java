package com.erikedits.justquests.plugin.book;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.gui.MainMenu;
import com.erikedits.justquests.plugin.text.Items;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * The quest book item: a glowing book marked with a tag; right-click opens the quests. It is kept
 * out of crafting, enchanting and anvils, so it can't be used up by accident.
 */
public final class QuestBook implements Listener {
    private final JustQuestsPlugin plugin;
    private final NamespacedKey tag;

    public QuestBook(JustQuestsPlugin plugin) {
        this.plugin = plugin;
        this.tag = new NamespacedKey(plugin, "quest_book");
    }

    public ItemStack create(Player player) {
        String lang = Lang.of(player);
        ItemStack book = Items.make(Material.BOOK, 1, Text.lit("§6" + Text.legacy(lang, "justquests.plugin.book.name")),
            List.of(Text.tr(lang, "justquests.plugin.book.lore")), true);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(tag, PersistentDataType.BYTE, (byte) 1);
            book.setItemMeta(meta);
        }
        return book;
    }

    public boolean is(ItemStack stack) {
        if (stack == null || stack.getType() != Material.BOOK || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(tag, PersistentDataType.BYTE);
    }

    /** Gives the player a book; false if they already carry one. */
    public boolean give(Player player) {
        for (ItemStack s : player.getInventory().getContents()) {
            if (is(s)) return false;
        }
        com.erikedits.justquests.plugin.quest.Reward.give(player, create(player));
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND && e.getHand() != EquipmentSlot.OFF_HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!is(e.getItem())) return;
        Block block = e.getClickedBlock();
        if (block != null && !e.getPlayer().isSneaking() && block.getType().isInteractable()
            && block.getType() != Material.CHISELED_BOOKSHELF && block.getType() != Material.LECTERN) {
            e.setUseItemInHand(Event.Result.DENY);   // chests, doors, ... still open normally
            return;
        }
        e.setCancelled(true);
        if (!plugin.allowed(e.getPlayer(), "justquests.command.open", false)) return;
        new MainMenu(plugin, e.getPlayer()).open();
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        for (ItemStack s : e.getInventory().getMatrix()) {
            if (is(s)) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent e) {
        if (is(e.getItem())) e.setCancelled(true);
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        for (ItemStack s : e.getInventory().getContents()) {
            if (is(s)) {
                e.setResult(null);
                return;
            }
        }
    }
}
