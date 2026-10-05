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
                Map<String, Integer> on = new HashMap<>();
                if (meta != null) {
                    meta.getEnchants().forEach((e, lvl) -> on.merge(Compat.key(e).toString(), lvl, Math::max));
                    if (meta instanceof EnchantmentStorageMeta book) {
                        book.getStoredEnchants().forEach((e, lvl) -> on.merge(Compat.key(e).toString(), lvl, Math::max));
                    }
                }
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

        @Override
        public BaseComponent name(String lang) {
            List<BaseComponent> extras = new ArrayList<>();
            enchantments.forEach((id, level) -> {
                Enchantment e = Registry.ENCHANTMENT.get(key(id));
                TextComponent part = new TextComponent("");
                part.addExtra(e != null ? Text.enchantment(e) : new TextComponent(id));
                part.addExtra(" ");
                part.addExtra(new TranslatableComponent("enchantment.level." + level));
                extras.add(part);
            });
            if (potion != null) extras.add(new TextComponent(Text.pretty(potion.substring(potion.indexOf(':') + 1))));
            if (customName != null) extras.add(new TextComponent("\"" + customName + "\""));
            TextComponent out = new TextComponent("");
            out.addExtra(base.name(lang));
            if (extras.isEmpty()) return out;
            out.addExtra(" (");
            for (int i = 0; i < extras.size(); i++) {
                if (i > 0) out.addExtra(", ");
                out.addExtra(extras.get(i));
            }
            out.addExtra(")");
            return out;
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
