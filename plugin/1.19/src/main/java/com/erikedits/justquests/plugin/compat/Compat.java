package com.erikedits.justquests.plugin.compat;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * What differs between server versions. Every plugin jar has its own copy of this class with the
 * same methods; this one is for Minecraft 1.19 - 1.20.6 (compiled against Spigot API 1.19, Java 17).
 * Methods that newer servers of the range added are looked up at runtime.
 */
public final class Compat {
    public static final String RANGE = "1.19 - 1.20.6";
    /** The first version of the range, and the first one after it. */
    public static final String FIRST = "1.19";
    public static final String BEYOND = "1.21";

    /** PotionType names that differ from the game's potion ids. */
    private static final Map<String, String> POTION_IDS = Map.of("SPEED", "swiftness", "JUMP", "leaping",
        "INSTANT_HEAL", "healing", "INSTANT_DAMAGE", "harming", "REGEN", "regeneration", "UNCRAFTABLE", "empty");

    private static final Method MATERIAL_ENABLED = method(Material.class, "isEnabledByFeature", World.class);     // 1.19.3+
    private static final Method ENTITY_ENABLED = method(EntityType.class, "isEnabledByFeature", World.class);     // 1.19.3+
    private static final Method BASE_POTION_TYPE = method(PotionMeta.class, "getBasePotionType");                 // 1.20.2+
    private static final Method GLINT = method(ItemMeta.class, "setEnchantmentGlintOverride", Boolean.class);     // 1.20.5+

    private Compat() {}

    private static Method method(Class<?> owner, String name, Class<?>... args) {
        try {
            return owner.getMethod(name, args);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static NamespacedKey key(Keyed k) {
        return k.getKey();
    }

    /** The game's own rule: blocks and their items are "block.ns.path", other items "item.ns.path". */
    public static String itemKey(Material m) {
        // the two block-and-item materials whose item is not the block's item
        boolean ownItem = m == Material.WHEAT || m == Material.NETHER_WART;
        return (m.isBlock() && !ownItem ? "block." : "item.") + m.getKey().getNamespace() + "." + m.getKey().getKey();
    }

    public static String blockKey(Material m) {
        return (m.isBlock() ? "block." : "item.") + m.getKey().getNamespace() + "." + m.getKey().getKey();
    }

    public static String entityKey(EntityType t) {
        return t.getKey() == null ? null : "entity." + t.getKey().getNamespace() + "." + t.getKey().getKey();
    }

    public static String effectKey(PotionEffectType t) {
        return "effect." + t.getKey().getNamespace() + "." + t.getKey().getKey();
    }

    public static String enchantmentKey(Enchantment e) {
        return "enchantment." + e.getKey().getNamespace() + "." + e.getKey().getKey();
    }

    /** Older chat components have no fallback text; vanilla keys always have a translation. */
    public static void fallback(TranslatableComponent t, String text) {
    }

    public static BaseComponent legacy(String text) {
        return new TextComponent(TextComponent.fromLegacyText(text));
    }

    public static PotionEffectType effect(NamespacedKey key) {
        return PotionEffectType.getByKey(key);
    }

    /** The potion's id ("minecraft:long_swiftness"), or null. */
    public static String basePotion(PotionMeta meta) {
        if (BASE_POTION_TYPE != null) {
            try {
                Object type = BASE_POTION_TYPE.invoke(meta);
                return type instanceof Keyed k ? k.getKey().toString() : null;
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        PotionData data = meta.getBasePotionData();
        if (data == null || data.getType() == null) return null;
        String name = data.getType().name();
        String id = POTION_IDS.getOrDefault(name, name.toLowerCase(Locale.ROOT));
        if (data.isExtended()) id = "long_" + id;
        else if (data.isUpgraded()) id = "strong_" + id;
        return "minecraft:" + id;
    }

    /** Registered and switched on for the world; before 1.19.3 (no experimental features) always. */
    public static boolean enabled(Material m, World w) {
        return call(MATERIAL_ENABLED, m, w);
    }

    public static boolean enabled(EntityType t, World w) {
        return call(ENTITY_ENABLED, t, w);
    }

    private static boolean call(Method check, Object target, World w) {
        if (check == null) return true;
        try {
            return (Boolean) check.invoke(target, w);
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }

    /** Makes a menu item glow: the glint switch from 1.20.5, else a hidden enchantment. */
    public static void glint(ItemMeta meta) {
        if (GLINT != null) {
            try {
                GLINT.invoke(meta, Boolean.TRUE);
                return;
            } catch (ReflectiveOperationException e) {
                // fall through to the enchantment
            }
        }
        Enchantment unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (unbreaking != null) meta.addEnchant(unbreaking, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
}
