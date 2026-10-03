package com.erikedits.justquests.data.objective;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads what an item filter checks (enchantments, potion, custom name) from an ItemStack.
 * This build stores them as NBT (before 1.20.5); the 1.20.5+ builds read data components.
 */
final class ItemExtras {
    private ItemExtras() {}

    /** Enchantment id -> level, including an enchanted book's stored enchantments. */
    static Map<String, Integer> enchantments(ItemStack stack) {
        Map<String, Integer> out = new HashMap<>();
        EnchantmentHelper.getEnchantments(stack).forEach((enchantment, level) -> {
            ResourceLocation id = Registry.ENCHANTMENT.getKey(enchantment);
            if (id != null) out.put(id.toString(), level);
        });
        return out;
    }

    /** Potion id (potions, splash/lingering potions, tipped arrows), or null. */
    static String potion(ItemStack stack) {
        ResourceLocation id = Registry.POTION.getKey(PotionUtils.getPotion(stack));
        return id == null || id.getPath().equals("empty") ? null : id.toString();
    }

    /** Name given at an anvil, or null. */
    static String customName(ItemStack stack) {
        return stack.hasCustomHoverName() ? stack.getHoverName().getString() : null;
    }
}
