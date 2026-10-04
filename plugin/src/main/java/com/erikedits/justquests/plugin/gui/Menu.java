package com.erikedits.justquests.plugin.gui;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.text.Items;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A page of the quest book: a chest inventory whose items are buttons. Nothing can be taken out or
 * put in (see {@link MenuListener}); a click runs the slot's action.
 */
public abstract class Menu implements InventoryHolder {
    protected static final int SIZE = 54;

    protected final JustQuestsPlugin plugin;
    protected final Player player;
    protected final String lang;
    private Inventory inventory;
    private final Map<Integer, Consumer<ClickType>> actions = new HashMap<>();

    protected Menu(JustQuestsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.lang = Lang.of(player);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected abstract String title();

    protected abstract void render();

    public void open() {
        inventory = Bukkit.createInventory(this, SIZE, title());
        redraw();
        player.openInventory(inventory);
    }

    /** Draws the page again in place (after an action changed something). */
    public void redraw() {
        inventory.clear();
        actions.clear();
        render();
    }

    void click(int slot, ClickType type) {
        Consumer<ClickType> action = actions.get(slot);
        if (action == null) return;
        player.playSound(player.getLocation(), "minecraft:ui.button.click", SoundCategory.MASTER, 0.4f, 1f);
        action.accept(type);
    }

    protected void set(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected void set(int slot, ItemStack item, Consumer<ClickType> action) {
        inventory.setItem(slot, item);
        actions.put(slot, action);
    }

    /** Opens another page on the next tick (opening inside a click event is not safe). */
    protected void go(Menu next) {
        Bukkit.getScheduler().runTask(plugin, next::open);
    }

    /** The bottom row as a dark frame. */
    protected void frame() {
        ItemStack pane = Items.make(Material.GRAY_STAINED_GLASS_PANE, 1, Text.lit(" "), List.of(), false);
        for (int i = 45; i < 54; i++) set(i, pane);
    }

    protected BaseComponent tr(String key, Object... args) {
        return Text.tr(lang, key, args);
    }

    protected String legacy(String key, Object... args) {
        return Text.legacy(lang, key, args);
    }

    protected ItemStack item(Material m, BaseComponent name, List<BaseComponent> lore) {
        return Items.make(m, 1, name, lore, false);
    }

    protected ItemStack item(Material m, int count, BaseComponent name, List<BaseComponent> lore, boolean glow) {
        return Items.make(m, count, name, lore, glow);
    }

    /** Splits text into lore lines of about {@code width} characters (by words; by characters for Japanese). */
    protected static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        boolean words = text.indexOf(' ') >= 0;
        if (!words) width = Math.max(10, width / 2);
        StringBuilder line = new StringBuilder();
        if (words) {
            for (String w : text.split(" ")) {
                if (line.length() > 0 && line.length() + 1 + w.length() > width) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                if (line.length() > 0) line.append(' ');
                line.append(w);
            }
        } else {
            for (int i = 0; i < text.length(); i++) {
                line.append(text.charAt(i));
                if (line.length() >= width) {
                    out.add(line.toString());
                    line.setLength(0);
                }
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }
}
