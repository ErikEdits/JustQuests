package com.erikedits.justquests.data.objective;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads what an item filter checks (enchantments, potion, custom name) from an ItemStack.
 * This build reads data components (1.20.5+); the older builds read NBT.
 */
final class ItemExtras {
    private ItemExtras() {}

    /** Enchantment id -> level, including an enchanted book's stored enchantments. */
    static Map<String, Integer> enchantments(ItemStack stack) {
        Map<String, Integer> out = new HashMap<>();
        for (ItemEnchantments list : List.of(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY),
                stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))) {
            for (Object2IntMap.Entry<Holder<Enchantment>> e : list.entrySet()) {
                e.getKey().unwrapKey().ifPresent(key -> out.merge(key.location().toString(), e.getIntValue(), Math::max));
            }
        }
        return out;
    }

    /** Potion id (potions, splash/lingering potions, tipped arrows), or null. */
    static String potion(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents == null ? null
            : contents.potion().flatMap(Holder::unwrapKey).map(key -> key.location().toString()).orElse(null);
    }

    /** Name given at an anvil, or null. */
    static String customName(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return name == null ? null : name.getString();
    }
}
