package com.erikedits.justquests.plugin.book;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.gui.MainMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

/**
 * Quest givers: any mob, villager or armor stand an operator marks with /quest npc set opens the
 * quest book on right-click (instead of trading). A marked mob stands still, is silent, can't be
 * hurt and never despawns; the mark lives on the entity itself.
 */
public final class QuestNpc implements Listener {
    private static final double REACH = 6.0;

    private final JustQuestsPlugin plugin;
    private final NamespacedKey key;

    public QuestNpc(JustQuestsPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "quest_npc");
    }

    public boolean is(Entity e) {
        return e.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** The entity the player looks at (not a player), up to 6 blocks away, or null. */
    public Entity target(Player p) {
        RayTraceResult hit = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), REACH, 0.3,
            e -> !(e instanceof Player));
        if (hit == null || hit.getHitEntity() == null) return null;
        // a wall between the player and the entity: not the one they mean
        RayTraceResult block = p.rayTraceBlocks(p.getEyeLocation().distance(hit.getHitPosition().toLocation(p.getWorld())));
        return block != null && block.getHitBlock() != null ? null : hit.getHitEntity();
    }

    /** Marks the entity as a quest giver, with a name over its head if one is given. */
    public void set(Entity e, String name) {
        e.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        if (name != null && !name.isBlank()) {
            e.setCustomName(name.replace('&', '§'));
            e.setCustomNameVisible(true);
        }
        e.setInvulnerable(true);
        e.setSilent(true);
        e.setPersistent(true);
        if (e instanceof LivingEntity living) {
            living.setAI(false);
            living.setRemoveWhenFarAway(false);
        }
    }

    /** Makes a quest giver an ordinary entity again (its name stays). */
    public void remove(Entity e) {
        e.getPersistentDataContainer().remove(key);
        e.setInvulnerable(false);
        e.setSilent(false);
        if (e instanceof LivingEntity living) living.setAI(true);
    }

    /**
     * A right-click on an entity can arrive as both events; the menu opens from one of them only:
     * the "at" event for armor stands (holding an item, the client sends only that one), the plain
     * one for everything else. Both are cancelled for both hands: no trading, no item swap.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!is(e.getRightClicked())) return;
        e.setCancelled(true);
        if (!(e.getRightClicked() instanceof ArmorStand)) open(e.getPlayer(), e.getHand());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractAt(PlayerInteractAtEntityEvent e) {
        if (!is(e.getRightClicked())) return;
        e.setCancelled(true);
        if (e.getRightClicked() instanceof ArmorStand) open(e.getPlayer(), e.getHand());
    }

    private void open(Player player, EquipmentSlot hand) {
        if (hand != EquipmentSlot.HAND || !plugin.allowed(player, "justquests.command.open", false)) return;
        new MainMenu(plugin, player).open();
    }
}
