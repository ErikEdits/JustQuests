package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Matches an item by a single id (`minecraft:oak_log`) or by a tag
 * (`#minecraft:logs`) — Q38. Stored as a plain string and resolved at
 * match time via {@code ItemStack.is(...)}, so it works with plain JsonOps
 * (no RegistryOps needed) and respects tags that bind after datapack load.
 */
public interface ItemMatcher {
    boolean matches(ItemStack stack);

    /** Plain id/tag string, for logs and diagnostics. */
    String label();

    /** Player-facing name; a single item localizes via its vanilla key. */
    Component name();

    Codec<ItemMatcher> CODEC = Codec.STRING.xmap(ItemMatcher::parse, ItemMatcher::label);

    static ItemMatcher parse(String s) {
        if (s.startsWith("#")) {
            return new Tag(TagKey.create(Registries.ITEM, ResourceLocation.parse(s.substring(1))));
        }
        return new Single(BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(s)));
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
    }
}
