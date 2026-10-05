package com.erikedits.justquests.plugin.gen;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Random;

/** Fireworks over everyone online when the server goal is reached; they never hurt anyone. */
public final class Fireworks implements Listener {
    private static final Color[] COLORS = {Color.YELLOW, Color.ORANGE, Color.LIME, Color.AQUA, Color.FUCHSIA, Color.WHITE};

    private final NamespacedKey key;
    private final Random random = new Random();

    Fireworks(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "celebration");
    }

    /** Three rockets around every online player. */
    void celebrate() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 3; i++) {
                Firework fw = p.getWorld().spawn(p.getLocation().add(random.nextInt(5) - 2, 1, random.nextInt(5) - 2), Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                    .with(i == 0 ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.STAR)
                    .withColor(COLORS[random.nextInt(COLORS.length)], COLORS[random.nextInt(COLORS.length)])
                    .withFade(Color.WHITE)
                    .trail(true)
                    .flicker(i == 2)
                    .build());
                meta.setPower(1 + i % 2);
                fw.setFireworkMeta(meta);
                fw.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Firework fw && fw.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            e.setCancelled(true);
        }
    }
}
