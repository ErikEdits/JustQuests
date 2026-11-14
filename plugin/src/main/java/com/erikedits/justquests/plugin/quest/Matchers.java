package com.erikedits.justquests.plugin.quest;

import com.erikedits.justquests.plugin.compat.Compat;
import com.erikedits.justquests.plugin.text.Text;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Item, block and mob matchers: a single id ("minecraft:oak_log") or a tag ("#minecraft:logs"),
 * items optionally narrowed by enchantments, potion or custom name - the mod's quest format.
 * Tags are looked up when matching, so they follow datapack reloads.
 */
public final class Matchers {
    private Matchers() {}

    static NamespacedKey key(String id) {
        NamespacedKey key = NamespacedKey.fromString(id.toLowerCase(Locale.ROOT));
        if (key == null) throw new IllegalArgumentException("not a valid id: " + id);
        return key;
    }

    static String ns(String id) {
        return id.indexOf(':') >= 0 ? id : "minecraft:" + id;
    }

    static BaseComponent anyOf(String lang, NamespacedKey tag) {
        return Text.tr(lang, "justquests.goal.any", Text.pretty(tag.getKey()));
    }

    // --- items -------------------------------------------------------------------------------

    public interface ItemMatcher {
        boolean matches(ItemStack stack);

        /** Matches the item type alone (statistics count per type, so filters don't apply). */
        boolean matchesType(Material type);

        String label();

        BaseComponent name(String lang);

        /** An item to show for it in the quest book. */
        Material icon();

        /** The name without the enchantments a filter asks for (0.5.3, #25). */
        default BaseComponent plainName(String lang) {
            return name(lang);
        }

        /** The item alone, without any words a filter adds. */
        default BaseComponent bareName(String lang) {
            return name(lang);
        }

        /** The enchantments a filter asks for, as "Sharpness I, Unbreaking III"; null without. */
        default BaseComponent enchantmentNames(String lang) {
            return null;
        }

        /** The anvil name a filter asks for; null without. */
        default String anvilName() {
            return null;
        }

        static ItemMatcher parse(JsonElement e) {
            if (e != null && e.isJsonObject()) return Filtered.parse(e.getAsJsonObject());
            if (e == null || !e.isJsonPrimitive()) throw new IllegalArgumentException("item must be an id, a #tag or an object");
            return parse(e.getAsString());
        }

        static ItemMatcher parse(String s) {
            if (s.startsWith("#")) return new ItemTag(key(s.substring(1)));
            Material m = Registry.MATERIAL.get(key(s));
            if (m == null || !m.isItem()) throw new IllegalArgumentException("unknown item: " + s);
            return new SingleItem(m);
        }
    }

    public record SingleItem(Material item) implements ItemMatcher {
        @Override
        public boolean matches(ItemStack stack) {
            return stack != null && stack.getType() == item;
        }

        @Override
        public boolean matchesType(Material type) {
            return type == item;
        }

        @Override
        public String label() {
            return Compat.key(item).toString();
        }

        @Override
        public BaseComponent name(String lang) {
            return Text.item(item);
        }

        @Override
        public Material icon() {
            return item;
        }
    }

    public record ItemTag(NamespacedKey tag) implements ItemMatcher {
        private Tag<Material> resolve() {
            return Bukkit.getTag(Tag.REGISTRY_ITEMS, tag, Material.class);
        }

        @Override
        public boolean matches(ItemStack stack) {
            return stack != null && matchesType(stack.getType());
        }

        @Override
        public boolean matchesType(Material type) {
            Tag<Material> t = resolve();
            return t != null && t.isTagged(type);
        }

        @Override
        public String label() {
            return "#" + tag;
        }

        @Override
        public BaseComponent name(String lang) {
            return anyOf(lang, tag);
        }

        @Override
        public Material icon() {
            Tag<Material> t = resolve();
            if (t != null) {
                for (Material m : t.getValues()) {
                    if (m.isItem()) return m;
                }
            }
            return Material.PAPER;
        }
    }

    /** Enchantment id -> level of an item, including an enchanted book's stored enchantments. */
    public static Map<String, Integer> enchantments(ItemStack stack) {
        Map<String, Integer> on = new HashMap<>();
        ItemMeta meta = stack == null ? null : stack.getItemMeta();
        if (meta != null) {
            meta.getEnchants().forEach((e, lvl) -> on.merge(Compat.key(e).toString(), lvl, Math::max));
            if (meta instanceof EnchantmentStorageMeta book) {
                book.getStoredEnchants().forEach((e, lvl) -> on.merge(Compat.key(e).toString(), lvl, Math::max));
            }
        }
        return on;
    }

    public record Filtered(ItemMatcher base, Map<String, Integer> enchantments, String potion, String customName)
            implements ItemMatcher {
        static Filtered parse(JsonObject o) {
            if (!o.has("id")) throw new IllegalArgumentException("an item object needs an \"id\"");
            ItemMatcher base = ItemMatcher.parse(o.get("id").getAsString());
            Map<String, Integer> ench = new LinkedHashMap<>();
            if (o.has("enchantments")) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("enchantments").entrySet()) {
                    ench.put(ns(e.getKey().toLowerCase(Locale.ROOT)), e.getValue().getAsInt());
                }
            }
            String potion = o.has("potion") ? ns(o.get("potion").getAsString().toLowerCase(Locale.ROOT)) : null;
            String name = o.has("name") ? o.get("name").getAsString() : null;
            return new Filtered(base, ench, potion, name);
        }

        @Override
        public boolean matches(ItemStack stack) {
            if (!base.matches(stack)) return false;
            ItemMeta meta = stack.getItemMeta();
            if (!enchantments.isEmpty()) {
                Map<String, Integer> on = Matchers.enchantments(stack);
                for (Map.Entry<String, Integer> e : enchantments.entrySet()) {
                    if (on.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
                }
            }
            if (potion != null && !(meta instanceof PotionMeta p && potion.equals(Compat.basePotion(p)))) return false;
            if (customName != null) {
                String shown = meta != null && meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : null;
                return customName.equals(shown);
            }
            return true;
        }

        @Override
        public boolean matchesType(Material type) {
            return base.matchesType(type);
        }

        @Override
        public String label() {
            StringBuilder sb = new StringBuilder(base.label());
            enchantments.forEach((id, level) -> sb.append(" +").append(id).append(' ').append(level));
            if (potion != null) sb.append(" potion=").append(potion);
            if (customName != null) sb.append(" name=\"").append(customName).append('"');
            return sb.toString();
        }

        /** "Diamond Sword named "Excalibur" (Sharpness II)", "Potion of Swiftness", as in the mod. */
        @Override
        public BaseComponent name(String lang) {
            BaseComponent enchants = enchantmentNames(lang);
            return enchants != null ? Text.tr(lang, "justquests.goal.with_extras", plainName(lang), enchants) : plainName(lang);
        }

        @Override
        public BaseComponent plainName(String lang) {
            return customName != null ? Text.tr(lang, "justquests.goal.named", bareName(lang), customName) : bareName(lang);
        }

        @Override
        public BaseComponent bareName(String lang) {
            return potion != null ? potionName(lang) : base.name(lang);
        }

        @Override
        public BaseComponent enchantmentNames(String lang) {
            if (enchantments.isEmpty()) return null;
            TextComponent out = new TextComponent("");
            boolean first = true;
            for (Map.Entry<String, Integer> en : enchantments.entrySet()) {
                if (!first) out.addExtra(", ");
                first = false;
                Enchantment e = Registry.ENCHANTMENT.get(key(en.getKey()));
                out.addExtra(e != null ? Text.enchantment(e) : new TextComponent(en.getKey()));
                out.addExtra(" ");
                out.addExtra(new TranslatableComponent("enchantment.level." + en.getValue()));
            }
            return out;
        }

        @Override
        public String anvilName() {
            return customName;
        }

        /** The game's own name for the potion ("Potion of Swiftness"); "strong_" adds II, "long_" (long). */
        private BaseComponent potionName(String lang) {
            String path = potion.substring(potion.indexOf(':') + 1);
            boolean strong = path.startsWith("strong_");
            boolean lasting = path.startsWith("long_");
            String effect = strong ? path.substring(7) : lasting ? path.substring(5) : path;
            String item = base.icon() == null ? "potion" : base.icon().name().toLowerCase(Locale.ROOT);
            String kind = item.equals("splash_potion") || item.equals("lingering_potion") || item.equals("tipped_arrow") ? item : "potion";
            BaseComponent name = new TranslatableComponent("item.minecraft." + kind + ".effect." + effect);
            if (strong) return Text.tr(lang, "justquests.goal.potion_strong", name);
            if (lasting) return Text.tr(lang, "justquests.goal.potion_long", name);
            return name;
        }

        @Override
        public Material icon() {
            return base.icon();
        }
    }

    // --- blocks ------------------------------------------------------------------------------

    public interface BlockMatcher {
        boolean matches(Material block);

        String label();

        BaseComponent name(String lang);

        Material icon();

        static BlockMatcher parse(String s) {
            if (s.startsWith("#")) return new BlockTag(key(s.substring(1)));
            Material m = Registry.MATERIAL.get(key(s));
            if (m == null || !m.isBlock()) throw new IllegalArgumentException("unknown block: " + s);
            return new SingleBlock(m);
        }
    }

    public record SingleBlock(Material block) implements BlockMatcher {
        @Override
        public boolean matches(Material b) {
            return b == block;
        }

        @Override
        public String label() {
            return Compat.key(block).toString();
        }

        @Override
        public BaseComponent name(String lang) {
            return Text.block(block);
        }

        @Override
        public Material icon() {
            return block.isItem() ? block : Material.PAPER;
        }
    }

    public record BlockTag(NamespacedKey tag) implements BlockMatcher {
        private Tag<Material> resolve() {
            return Bukkit.getTag(Tag.REGISTRY_BLOCKS, tag, Material.class);
        }

        @Override
        public boolean matches(Material b) {
            Tag<Material> t = resolve();
            return t != null && t.isTagged(b);
        }

        @Override
        public String label() {
            return "#" + tag;
        }

        @Override
        public BaseComponent name(String lang) {
            return anyOf(lang, tag);
        }

        @Override
        public Material icon() {
            Tag<Material> t = resolve();
            if (t != null) {
                for (Material m : t.getValues()) {
                    if (m.isItem()) return m;
                }
            }
            return Material.PAPER;
        }
    }

    // --- mobs --------------------------------------------------------------------------------

    public interface EntityMatcher {
        boolean matches(EntityType type);

        String label();

        BaseComponent name(String lang);

        /** The mob's spawn egg, for the quest book. */
        Material icon();

        static EntityMatcher parse(String s) {
            if (s.startsWith("#")) return new EntityTag(key(s.substring(1)));
            EntityType t = Registry.ENTITY_TYPE.get(key(s));
            if (t == null) throw new IllegalArgumentException("unknown mob: " + s);
            return new SingleEntity(t);
        }
    }

    static Material egg(EntityType t) {
        NamespacedKey key = NamespacedKey.fromString(Compat.key(t) + "_spawn_egg");
        Material m = key == null ? null : Registry.MATERIAL.get(key);
        return m != null ? m : Material.SPAWNER;
    }

    public record SingleEntity(EntityType type) implements EntityMatcher {
        @Override
        public boolean matches(EntityType t) {
            return t == type;
        }

        @Override
        public String label() {
            return Compat.key(type).toString();
        }

        @Override
        public BaseComponent name(String lang) {
            return Text.entity(type);
        }

        @Override
        public Material icon() {
            return egg(type);
        }
    }

    public record EntityTag(NamespacedKey tag) implements EntityMatcher {
        private Tag<EntityType> resolve() {
            return Bukkit.getTag(Tag.REGISTRY_ENTITY_TYPES, tag, EntityType.class);
        }

        @Override
        public boolean matches(EntityType t) {
            Tag<EntityType> tg = resolve();
            return tg != null && tg.isTagged(t);
        }

        @Override
        public String label() {
            return "#" + tag;
        }

        @Override
        public BaseComponent name(String lang) {
            return anyOf(lang, tag);
        }

        @Override
        public Material icon() {
            Tag<EntityType> tg = resolve();
            if (tg != null) {
                for (EntityType t : tg.getValues()) return egg(t);
            }
            return Material.SPAWNER;
        }
    }
}
