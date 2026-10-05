package com.erikedits.justquests.plugin.compat;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/**
 * What differs between server versions. Every plugin jar has its own copy of this class with the
 * same methods; this one is for Minecraft 1.21 - 1.21.11 (compiled against Spigot API 1.21).
 */
public final class Compat {
    public static final String RANGE = "1.21 - 1.21.11";
    /** The first version of the range, and the first one after it. */
    public static final String FIRST = "1.21";
    public static final String BEYOND = "26";

    private Compat() {}

    public static NamespacedKey key(Keyed k) {
        return k.getKey();
    }

    public static String itemKey(Material m) {
        return m.getItemTranslationKey();
    }

    public static String blockKey(Material m) {
        return m.getBlockTranslationKey();
    }

    public static String entityKey(EntityType t) {
        return t.getTranslationKey();
    }

    public static String effectKey(PotionEffectType t) {
        return t.getTranslationKey();
    }

    public static String enchantmentKey(Enchantment e) {
        return e.getTranslationKey();
    }

    /** The text the client shows when it has no translation for the key. */
    public static void fallback(TranslatableComponent t, String text) {
        t.setFallback(text);
    }

    public static BaseComponent legacy(String text) {
        return TextComponent.fromLegacy(text);
    }

    public static PotionEffectType effect(NamespacedKey key) {
        return Registry.EFFECT.get(key);
    }

    /** The potion's id ("minecraft:long_swiftness"), or null. */
    public static String basePotion(PotionMeta meta) {
        PotionType type = meta.getBasePotionType();
        return type == null ? null : type.getKey().toString();
    }

    /** Registered and switched on for the world (experimental content stays out while its feature is off). */
    public static boolean enabled(Material m, World w) {
        return m.isEnabledByFeature(w);
    }

    public static boolean enabled(EntityType t, World w) {
        return t.isEnabledByFeature(w);
    }

    /** Makes a menu item glow. */
    public static void glint(ItemMeta meta) {
        meta.setEnchantmentGlintOverride(true);
    }
}
