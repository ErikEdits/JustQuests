package com.erikedits.justquests.plugin.text;

import com.erikedits.justquests.plugin.compat.Compat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Menu items with a component name and lore. The plain Spigot API only takes strings there, so the
 * item is built from the same text the /give command reads, and names stay translation keys the
 * client translates. That text changed twice: NBT with JSON strings before 1.20.5, item components
 * with JSON strings until 1.21.4, item components with SNBT from 1.21.5; all three are written here.
 * Should the server reject one, the item falls back to plain strings with English names.
 */
public final class Items {
    private enum Format { NBT, COMPONENTS, SNBT }

    private static Format format = Format.SNBT;
    private static boolean warned;
    private static Logger log;

    private Items() {}

    /** Whether an item had to fall back to plain text. */
    public static boolean fellBack() {
        return warned;
    }

    public static void init(Logger logger) {
        log = logger;
        String v = Bukkit.getBukkitVersion();
        format = atLeast(v, 1, 21, 5) ? Format.SNBT : atLeast(v, 1, 20, 5) ? Format.COMPONENTS : Format.NBT;
    }

    /** "1.21.4-R0.1-SNAPSHOT" is at least "1.21" (missing parts count as 0). */
    public static boolean atLeast(String version, String min) {
        String[] p = min.split("\\.");
        int[] n = new int[3];
        for (int i = 0; i < 3 && i < p.length; i++) n[i] = Integer.parseInt(p[i]);
        return atLeast(version, n[0], n[1], n[2]);
    }

    /** "1.21.4-R0.1-SNAPSHOT" (or "26.1-...") is at least major.minor.patch. */
    static boolean atLeast(String version, int major, int minor, int patch) {
        String v = version.contains("-") ? version.substring(0, version.indexOf('-')) : version;
        String[] parts = v.split("\\.");
        int[] want = {major, minor, patch};
        for (int i = 0; i < 3; i++) {
            int have;
            try {
                have = i < parts.length ? Integer.parseInt(parts[i]) : 0;
            } catch (NumberFormatException e) {
                have = 0;
            }
            if (have != want[i]) return have > want[i];
        }
        return true;
    }

    public static ItemStack make(Material material, int count, BaseComponent name, List<BaseComponent> lore, boolean glow) {
        if (material == null || material.isAir() || !material.isItem()) material = Material.PAPER;
        ItemStack stack;
        try {
            String id = Compat.key(material).toString();
            stack = Bukkit.getItemFactory().createItemStack(format == Format.NBT ? nbt(id, name, lore, glow) : components(id, name, lore, glow));
        } catch (RuntimeException e) {
            if (!warned && log != null) {
                warned = true;
                log.warning("Menu items fall back to plain text (the server did not take component text): " + e.getMessage());
            }
            stack = plain(material, name, lore, glow);
        }
        stack.setAmount(Math.max(1, Math.min(count, stack.getMaxStackSize())));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());   // only the lore: no attributes, enchantments or potion lines
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** 1.20.5 and newer: "id[minecraft:custom_name=...,minecraft:lore=[...]]". */
    private static String components(String id, BaseComponent name, List<BaseComponent> lore, boolean glow) {
        StringBuilder sb = new StringBuilder(id).append('[');
        sb.append("minecraft:custom_name=").append(text(name, ChatColor.WHITE));
        if (!lore.isEmpty()) {
            sb.append(",minecraft:lore=[");
            for (int i = 0; i < lore.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(text(lore.get(i), ChatColor.GRAY));
            }
            sb.append(']');
        }
        if (glow) sb.append(",minecraft:enchantment_glint_override=true");
        return sb.append(']').toString();
    }

    /** Before 1.20.5: "id{display:{Name:'...',Lore:['...']}}", the glow from a (hidden) enchantment. */
    private static String nbt(String id, BaseComponent name, List<BaseComponent> lore, boolean glow) {
        StringBuilder sb = new StringBuilder(id).append("{display:{Name:").append(text(name, ChatColor.WHITE));
        if (!lore.isEmpty()) {
            sb.append(",Lore:[");
            for (int i = 0; i < lore.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(text(lore.get(i), ChatColor.GRAY));
            }
            sb.append(']');
        }
        sb.append('}');
        if (glow) sb.append(",Enchantments:[{id:\"minecraft:unbreaking\",lvl:1s}]");
        return sb.append('}').toString();
    }

    private static ItemStack plain(Material material, BaseComponent name, List<BaseComponent> lore, boolean glow) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RESET + name.toLegacyText());
            List<String> lines = new ArrayList<>();
            for (BaseComponent l : lore) lines.add(ChatColor.GRAY + l.toLegacyText());
            meta.setLore(lines);
            if (glow) Compat.glint(meta);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** One line as the item text format wants it: not italic, in the default colour unless coloured. */
    private static String text(BaseComponent line, ChatColor color) {
        TextComponent root = new TextComponent("");
        root.setItalic(false);
        root.setColor(color);
        root.addExtra(line);
        String json = ComponentSerializer.toString(root);
        if (format == Format.SNBT) return json;   // a JSON object is valid SNBT
        return "'" + json.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
