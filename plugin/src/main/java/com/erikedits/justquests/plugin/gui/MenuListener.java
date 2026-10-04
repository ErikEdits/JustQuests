package com.erikedits.justquests.plugin.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Keeps the quest book's items in place and turns clicks into button presses. */
public final class MenuListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        e.setCancelled(true);   // also clicks in the player's own inventory: no shift-clicking items in
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(e.getView().getTopInventory())) {
            menu.click(e.getRawSlot(), e.getClick());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }
}
