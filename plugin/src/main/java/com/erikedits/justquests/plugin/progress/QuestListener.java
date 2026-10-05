package com.erikedits.justquests.plugin.progress;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.quest.Objective;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Turns game events into quest progress. Events that other plugins can cancel (protection, claims)
 * are read at MONITOR priority and skipped when cancelled, so blocked actions never count.
 */
public final class QuestListener implements Listener {
    private final JustQuestsPlugin plugin;

    public QuestListener(JustQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    private ProgressService progress() {
        return plugin.progress();
    }

    /** collect_item: what actually left the ground. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        ItemStack stack = e.getItem().getItemStack();
        int picked = stack.getAmount() - Math.max(0, e.getRemaining());
        if (picked <= 0) return;
        progress().advance(player, o -> o instanceof Objective.Collect c && c.item().matches(stack) ? picked : 0);
    }

    /** kill_mob */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        EntityType type = e.getEntityType();
        progress().advance(killer, o -> o instanceof Objective.Kill k && k.entity().matches(type) ? 1 : 0);
    }

    /** place_block */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Material block = e.getBlockPlaced().getType();
        progress().advance(e.getPlayer(), o -> o instanceof Objective.Place p && p.block().matches(block) ? 1 : 0);
    }

    /** mine_block: the break itself, distinct from collect_item. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Material block = e.getBlock().getType();
        progress().advance(e.getPlayer(), o -> o instanceof Objective.Mine m && m.block().matches(block) ? 1 : 0);
    }

    /**
     * craft_item and use_item come from the vanilla statistics, as in the mod: "Times Used" for
     * use_item, "Times Crafted" for craft_item (it covers shift-clicks and the stonecutter). Furnace
     * outputs also raise "Times Crafted", so only crafting grids and the stonecutter count here;
     * smelting has its own objective.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStatistic(PlayerStatisticIncrementEvent e) {
        int gained = e.getNewValue() - e.getPreviousValue();
        if (gained <= 0 || e.getMaterial() == null) return;
        Material item = e.getMaterial();
        Player player = e.getPlayer();
        if (e.getStatistic() == Statistic.USE_ITEM) {
            progress().advance(player, o -> o instanceof Objective.Use u && u.item().matchesType(item) ? gained : 0);
        } else if (e.getStatistic() == Statistic.CRAFT_ITEM) {
            InventoryType open = player.getOpenInventory().getTopInventory().getType();
            if (open != InventoryType.CRAFTING && open != InventoryType.WORKBENCH && open != InventoryType.STONECUTTER) return;
            progress().advance(player, o -> o instanceof Objective.Craft c && c.item().matchesType(item) ? gained : 0);
        }
    }

    /** smelt_item: taking results out of a furnace, blast furnace or smoker. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSmelt(FurnaceExtractEvent e) {
        ItemStack stack = new ItemStack(e.getItemType());
        int n = e.getItemAmount();
        if (n <= 0) return;
        progress().advance(e.getPlayer(), o -> o instanceof Objective.Smelt s && s.item().matches(stack) ? n : 0);
    }

    /** consume_item */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack stack = e.getItem();
        progress().advance(e.getPlayer(), o -> o instanceof Objective.Consume c && c.item().matches(stack) ? 1 : 0);
    }

    /** enchant_item: an enchanting-table enchant, matched on the item as it comes out. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        ItemStack result = e.getItem().getType() == Material.BOOK ? new ItemStack(Material.ENCHANTED_BOOK) : e.getItem().clone();
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            e.getEnchantsToAdd().forEach((ench, level) -> {
                if (meta instanceof EnchantmentStorageMeta book) book.addStoredEnchant(ench, level, true);
                else meta.addEnchant(ench, level, true);
            });
            result.setItemMeta(meta);
        }
        progress().advance(e.getEnchanter(), o -> o instanceof Objective.Enchant en
            && (en.item() == null || en.item().matches(result) || en.item().matches(e.getItem())) ? 1 : 0);
    }

    /** tame_animal */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player player)) return;
        EntityType type = e.getEntityType();
        progress().advance(player, o -> o instanceof Objective.Tame t && t.entity().matches(type) ? 1 : 0);
    }

    /** breed_animal: matched on the species of the parents. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player player)) return;
        EntityType type = e.getMother().getType();
        progress().advance(player, o -> o instanceof Objective.Breed b && b.entity().matches(type) ? 1 : 0);
    }

    /** gain_advancement */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        String id = e.getAdvancement().getKey().toString();
        progress().advance(e.getPlayer(), o -> o instanceof Objective.Advancement a && a.advancement().equals(id) ? 1 : 0);
    }

    /** visit_dimension */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        progress().advance(player, o -> o instanceof Objective.Dimension d && d.matches(player.getWorld()) ? 1 : 0);
    }

    /** reach_level, right away instead of waiting for the next second. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevel(PlayerLevelChangeEvent e) {
        Player player = e.getPlayer();
        progress().advance(player, o -> o instanceof Objective.Level l && l.reached(player) ? 1 : 0);
    }

    /** reach_location and reach_level: checked once a second for players with active quests. */
    public void tick(Player player) {
        PlayerData data = plugin.view(player.getUniqueId());
        if (data == null || data.active.isEmpty()) return;
        progress().advance(player, o -> {
            if (o instanceof Objective.Location l && l.isAt(player)) return 1;
            if (o instanceof Objective.Level l && l.reached(player)) return 1;
            return 0;
        });
    }
}
