package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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

    /**
     * The name without the enchantments a filter asks for (0.5.3, #25), so a goal line can put them
     * where they read best: "Combine Diamond Sword with Enchanted Book (Sharpness I)".
     */
    default Component plainName() {
        return name();
    }

    /** The item alone, without any words a filter adds ("Diamond Sword", "Potion of Swiftness"). */
    default Component bareName() {
        return name();
    }

    /** The enchantments a filter asks for, as "Sharpness I, Unbreaking III"; empty without. */
    default Optional<Component> enchantmentNames() {
        return Optional.empty();
    }

    /** The anvil name a filter asks for; empty without. */
    default Optional<String> anvilName() {
        return Optional.empty();
    }

    /** "minecraft:oak_log", "#minecraft:logs", or {"id": ..., "enchantments" / "potion" / "name": ...}. */
    Codec<ItemMatcher> CODEC = Codec.either(Codec.STRING, Filtered.CODEC).xmap(
        either -> either.map(ItemMatcher::parse, filtered -> filtered),
        matcher -> matcher instanceof Filtered f
            ? Either.<String, Filtered>right(f) : Either.<String, Filtered>left(matcher.label()));

    static ItemMatcher parse(String s) {
        if (s.startsWith("#")) {
            return new Tag(TagKey.create(Registries.ITEM, Identifier.parse(s.substring(1))));
        }
        return new Single(BuiltInRegistries.ITEM.getValue(Identifier.parse(s)));
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
            return Msg.tr("justquests.goal.any", Msg.pretty(tag.location().getPath()));
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

        /** "Diamond Sword named "Excalibur" (Sharpness II)", "Potion of Swiftness". */
        @Override
        public Component name() {
            Optional<Component> enchants = enchantmentNames();
            return enchants.isPresent() ? Msg.tr("justquests.goal.with_extras", plainName(), enchants.get()) : plainName();
        }

        @Override
        public Component plainName() {
            return customName.isPresent() ? Msg.tr("justquests.goal.named", bareName(), customName.get()) : bareName();
        }

        @Override
        public Component bareName() {
            return potion.isPresent() ? potionName(potion.get()) : base.name();
        }

        @Override
        public Optional<Component> enchantmentNames() {
            if (enchantments.isEmpty()) return Optional.empty();
            MutableComponent out = Component.literal("");
            boolean first = true;
            for (Map.Entry<String, Integer> e : enchantments.entrySet()) {
                if (!first) out.append(", ");
                first = false;
                out.append(Component.translatable("enchantment." + ns(e.getKey()).replace(':', '.')))
                    .append(" ").append(Component.translatable("enchantment.level." + e.getValue()));
            }
            return Optional.of(out);
        }

        @Override
        public Optional<String> anvilName() {
            return customName;
        }

        /**
         * The game's own name for a potion, "Potion of Swiftness" ("Splash Potion of ...", "Arrow of
         * ..." for those items); "strong_" adds II, "long_" (long).
         */
        private Component potionName(String id) {
            String path = ns(id).substring(ns(id).indexOf(':') + 1);
            boolean strong = path.startsWith("strong_");
            boolean lasting = path.startsWith("long_");
            String effect = strong ? path.substring(7) : lasting ? path.substring(5) : path;
            String kind = "potion";
            if (base instanceof Single s) {
                String item = BuiltInRegistries.ITEM.getKey(s.item()).getPath();
                if (item.equals("splash_potion") || item.equals("lingering_potion") || item.equals("tipped_arrow")) kind = item;
            }
            Component name = Component.translatableWithFallback("item.minecraft." + kind + ".effect." + effect,
                "Potion of " + Character.toUpperCase(effect.charAt(0)) + effect.substring(1).replace('_', ' '));
            if (strong) return Msg.tr("justquests.goal.potion_strong", name);
            if (lasting) return Msg.tr("justquests.goal.potion_long", name);
            return name;
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
