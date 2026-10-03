package com.erikedits.justquests.data.objective;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Matches an item by a single id (`minecraft:oak_log`) or by a tag
 * (`#minecraft:logs`) — Q38 — optionally narrowed by enchantments, potion or
 * custom name ({@link Filtered}). Stored as a plain string and resolved at
 * match time via {@code ItemStack.is(...)}, so it works with plain JsonOps
 * (no RegistryOps needed) and respects tags that bind after datapack load.
 */
public interface ItemMatcher {
    boolean matches(ItemStack stack);

    /** Plain id/tag string, for logs and diagnostics. */
    String label();

    /** Player-facing name; a single item localizes via its vanilla key. */
    Component name();

    /** The concrete items (a tag's members); use_item reads their "used" statistics. */
    List<Item> items();

    /** "minecraft:oak_log", "#minecraft:logs", or {"id": ..., "enchantments" / "potion" / "name": ...}. */
    Codec<ItemMatcher> CODEC = Codec.either(Codec.STRING, Filtered.CODEC).xmap(
        either -> either.map(ItemMatcher::parse, filtered -> filtered),
        matcher -> matcher instanceof Filtered f
            ? Either.<String, Filtered>right(f) : Either.<String, Filtered>left(matcher.label()));

    static ItemMatcher parse(String s) {
        if (s.startsWith("#")) {
            return new Tag(TagKey.create(Registries.ITEM, new ResourceLocation(s.substring(1))));
        }
        return new Single(BuiltInRegistries.ITEM.get(new ResourceLocation(s)));
    }

    record Single(Item item) implements ItemMatcher {
        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(item);
        }

        @Override
        public String label() {
            return BuiltInRegistries.ITEM.getKey(item).toString();
        }

        @Override
        public Component name() {
            return new ItemStack(item).getHoverName();
        }

        @Override
        public List<Item> items() {
            return List.of(item);
        }
    }

    record Tag(TagKey<Item> tag) implements ItemMatcher {
        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(tag);
        }

        @Override
        public String label() {
            return "#" + tag.location();
        }

        @Override
        public Component name() {
            return Component.literal("#" + tag.location());
        }

        @Override
        public List<Item> items() {
            List<Item> out = new ArrayList<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) out.add(holder.value());
            return out;
        }
    }

    /**
     * An id or tag plus optional extras, written as an object:
     * {"id": "minecraft:diamond_sword", "enchantments": {"minecraft:sharpness": 2}, "potion": ..., "name": ...}.
     * Enchantment levels are minimums; ids without a namespace mean minecraft:. The same JSON works
     * on every version because {@link ItemExtras} reads NBT or data components as the build needs.
     */
    record Filtered(ItemMatcher base, Map<String, Integer> enchantments, Optional<String> potion,
                    Optional<String> customName) implements ItemMatcher {
        static final Codec<Filtered> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(ItemMatcher::parse, ItemMatcher::label).fieldOf("id").forGetter(Filtered::base),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("enchantments", Map.of()).forGetter(Filtered::enchantments),
            Codec.STRING.optionalFieldOf("potion").forGetter(Filtered::potion),
            Codec.STRING.optionalFieldOf("name").forGetter(Filtered::customName)
        ).apply(instance, Filtered::new));

        @Override
        public boolean matches(ItemStack stack) {
            if (!base.matches(stack)) return false;
            if (!enchantments.isEmpty()) {
                Map<String, Integer> on = ItemExtras.enchantments(stack);
                for (Map.Entry<String, Integer> e : enchantments.entrySet()) {
                    if (on.getOrDefault(ns(e.getKey()), 0) < e.getValue()) return false;
                }
            }
            if (potion.isPresent() && !ns(potion.get()).equals(ItemExtras.potion(stack))) return false;
            return customName.isEmpty() || customName.get().equals(ItemExtras.customName(stack));
        }

        @Override
        public String label() {
            StringBuilder sb = new StringBuilder(base.label());
            enchantments.forEach((id, level) -> sb.append(" +").append(ns(id)).append(' ').append(level));
            potion.ifPresent(p -> sb.append(" potion=").append(ns(p)));
            customName.ifPresent(n -> sb.append(" name=\"").append(n).append('"'));
            return sb.toString();
        }

        @Override
        public Component name() {
            List<Component> extras = new ArrayList<>();
            enchantments.forEach((id, level) -> extras.add(Component.translatable("enchantment." + ns(id).replace(':', '.'))
                .append(" ").append(Component.translatable("enchantment.level." + level))));
            potion.ifPresent(p -> extras.add(Component.literal(ns(p).substring(ns(p).indexOf(':') + 1).replace('_', ' '))));
            customName.ifPresent(n -> extras.add(Component.literal("\"" + n + "\"")));
            MutableComponent out = base.name().copy();
            if (extras.isEmpty()) return out;
            out.append(" (");
            for (int i = 0; i < extras.size(); i++) {
                if (i > 0) out.append(", ");
                out.append(extras.get(i));
            }
            return out.append(")");
        }

        @Override
        public List<Item> items() {
            return base.items();
        }

        private static String ns(String id) {
            return id.indexOf(':') >= 0 ? id : "minecraft:" + id;
        }
    }
}
