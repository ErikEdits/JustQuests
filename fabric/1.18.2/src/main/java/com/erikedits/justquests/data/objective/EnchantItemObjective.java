package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Enchant X items at an enchanting table, optionally only a given item ("item": id, #tag or a
 * filter object). Counted from the vanilla "Items Enchanted" statistic (see StatObjectives), so
 * an anvil doesn't count, the same as in the statistics screen.
 */
public record EnchantItemObjective(Optional<ItemMatcher> item, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:enchant_item";

    public static final MapCodec<EnchantItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(EnchantItemObjective::item),
        Codec.INT.fieldOf("count").forGetter(EnchantItemObjective::count)
    ).apply(instance, EnchantItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** Whether the item just enchanted (empty if it already left the table) counts. */
    public boolean accepts(ItemStack enchanted) {
        return item.isEmpty() || (!enchanted.isEmpty() && item.get().matches(enchanted));
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Enchant " + count + "x " + item.map(ItemMatcher::label).orElse("any item");
    }

    @Override
    public Component display() {
        return item.isPresent()
            ? new net.minecraft.network.chat.TextComponent("Enchant " + count + "x ").append(item.get().name())
            : new net.minecraft.network.chat.TextComponent("Enchant " + count + (count == 1 ? " item" : " items"));
    }
}
