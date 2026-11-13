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
import org.bukkit.event.inventory.InventoryClickEvent;
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

    /** The kind a distinct objective counts once: "minecraft:apple", "minecraft:zombie". */
    private static String kind(org.bukkit.Keyed thing) {
        return thing.getKey().toString();
    }

    /** collect_item: what actually left the ground. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        ItemStack stack = e.getItem().getItemStack();
        int picked = stack.getAmount() - Math.max(0, e.getRemaining());
        if (picked <= 0) return;
        progress().advance(player, kind(stack.getType()), o -> o instanceof Objective.Collect c && c.item().matches(stack) ? picked : 0);
    }

    /** kill_mob */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        // reset_on_death: quests with that option start over when the player dies
        if (e.getEntity() instanceof Player dead) progress().died(dead);
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        EntityType type = e.getEntityType();
        progress().advance(killer, kind(type), o -> o instanceof Objective.Kill k && k.entity().matches(type) ? 1 : 0);
    }

    /** place_block */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Material block = e.getBlockPlaced().getType();
        progress().advance(e.getPlayer(), kind(block), o -> o instanceof Objective.Place p && p.block().matches(block) ? 1 : 0);
    }

    /** mine_block: the break itself, distinct from collect_item. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Material block = e.getBlock().getType();
        progress().advance(e.getPlayer(), kind(block), o -> o instanceof Objective.Mine m && m.block().matches(block) ? 1 : 0);
    }

    /**
     * craft_item and use_item come from the vanilla statistics, as in the mod: "Times Used" for
     * use_item, "Times Crafted" for craft_item (it covers shift-clicks, the stonecutter and the
     * smithing table). Furnace outputs also raise "Times Crafted", so only crafting grids, the
     * stonecutter and the smithing table count here; smelting has its own objective.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStatistic(PlayerStatisticIncrementEvent e) {
        int gained = e.getNewValue() - e.getPreviousValue();
        if (gained <= 0 || e.getMaterial() == null) return;
        Material item = e.getMaterial();
        Player player = e.getPlayer();
        if (e.getStatistic() == Statistic.USE_ITEM) {
            progress().advance(player, kind(item), o -> o instanceof Objective.Use u && u.item().matchesType(item) ? gained : 0);
        } else if (e.getStatistic() == Statistic.CRAFT_ITEM) {
            InventoryType open = player.getOpenInventory().getTopInventory().getType();
            // SMITHING, and SMITHING_NEW on 1.19.4 (by name: the oldest API has no SMITHING_NEW)
            if (open != InventoryType.CRAFTING && open != InventoryType.WORKBENCH && open != InventoryType.STONECUTTER
                && !open.name().startsWith("SMITHING")) return;
            progress().advance(player, kind(item), o -> o instanceof Objective.Craft c && c.item().matchesType(item) ? gained : 0);
        }
    }

    /** smelt_item: taking results out of a furnace, blast furnace or smoker. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSmelt(FurnaceExtractEvent e) {
        ItemStack stack = new ItemStack(e.getItemType());
        int n = e.getItemAmount();
        if (n <= 0) return;
        progress().advance(e.getPlayer(), kind(e.getItemType()), o -> o instanceof Objective.Smelt s && s.item().matches(stack) ? n : 0);
    }

    /** consume_item */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack stack = e.getItem();
        progress().advance(e.getPlayer(), kind(stack.getType()), o -> o instanceof Objective.Consume c && c.item().matches(stack) ? 1 : 0);
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
        progress().advance(e.getEnchanter(), kind(e.getItem().getType()), o -> o instanceof Objective.Enchant en && en.table()
            && (en.item() == null || en.item().matches(result) || en.item().matches(e.getItem())) ? 1 : 0);
    }

    /**
     * anvil_item, grindstone_item, enchant_item "via": "anvil", and craft_item at a loom or cartography
     * table (0.5.2, #25). There is no event for taking these results, so a click on the result slot is
     * remembered and checked a tick later: a take used up the inputs, a click that could not take the
     * result (not enough levels, a full cursor) left them.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorkstation(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        org.bukkit.inventory.Inventory top = e.getView().getTopInventory();
        InventoryType type = top.getType();
        int out = switch (type) {
            case ANVIL, GRINDSTONE, CARTOGRAPHY -> 2;
            case LOOM -> 3;
            default -> -1;
        };
        if (out < 0 || e.getRawSlot() != out) return;
        ItemStack result = copy(e.getCurrentItem());
        if (result == null) return;
        ItemStack first = copy(top.getItem(0));
        ItemStack second = copy(top.getItem(1));
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack firstNow = top.getItem(0);
            ItemStack secondNow = top.getItem(1);
            switch (type) {
                case ANVIL -> {
                    // the anvil always empties its left slot when the result is taken
                    if (first == null || copy(firstNow) != null) return;
                    progress().advance(player, kind(result.getType()), o -> {
                        if (o instanceof Objective.Anvil a) return a.accepts(result, second) ? result.getAmount() : 0;
                        if (o instanceof Objective.Enchant en) {
                            return en.anvil() && (en.item() == null || en.item().matches(result))
                                && Objective.Enchant.raised(first, result) ? 1 : 0;
                        }
                        return 0;
                    });
                }
                case GRINDSTONE -> {
                    if (copy(firstNow) != null || copy(secondNow) != null) return;
                    progress().advance(player, kind(result.getType()), o ->
                        o instanceof Objective.Grindstone g && g.accepts(result, first, second) ? result.getAmount() : 0);
                }
                default -> {
                    // loom and cartography table: one of each input is used up per result
                    if (!used(first, firstNow) || !used(second, secondNow)) return;
                    progress().advance(player, kind(result.getType()), o ->
                        o instanceof Objective.Craft c && c.item().matches(result) ? result.getAmount() : 0);
                }
            }
        });
    }

    /** A copy of a real stack, or null for none or air. */
    private static ItemStack copy(ItemStack stack) {
        return stack == null || stack.getType().isAir() || stack.getAmount() <= 0 ? null : stack.clone();
    }

    /** Whether some of an input went away (and was not swapped for another item). */
    private static boolean used(ItemStack before, ItemStack now) {
        if (before == null) return false;
        ItemStack after = copy(now);
        return after == null || (after.isSimilar(before) && after.getAmount() < before.getAmount());
    }

    /** tame_animal */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player player)) return;
        EntityType type = e.getEntityType();
        progress().advance(player, kind(type), o -> o instanceof Objective.Tame t && t.entity().matches(type) ? 1 : 0);
    }

    /** breed_animal: matched on the species of the parents. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player player)) return;
        EntityType type = e.getMother().getType();
        progress().advance(player, kind(type), o -> o instanceof Objective.Breed b && b.entity().matches(type) ? 1 : 0);
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

    /** reach_location, reach_level and have_item: checked once a second for players with active quests. */
    public void tick(Player player) {
        PlayerData data = plugin.view(player.getUniqueId());
        if (data == null || data.active.isEmpty()) return;
        progress().advance(player, o -> {
            if (o instanceof Objective.Location l && l.isAt(player)) return 1;
            if (o instanceof Objective.Level l && l.reached(player)) return 1;
            // an advancement the player already has counts too, also one earned before the quest was taken
            if (o instanceof Objective.Advancement a && a.earnedBy(player)) return 1;
            // have_item looks into the inventory, wherever the items came from
            if (o instanceof Objective.Have h) return h.held(player);
            return 0;
        });
    }
}
