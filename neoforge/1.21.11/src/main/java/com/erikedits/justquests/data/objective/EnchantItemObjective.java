package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

/**
 * Enchant X items at an enchanting table, optionally only a given item ("item": id, #tag or a
 * filter object). At the table it is counted from the vanilla "Items Enchanted" statistic (see
 * StatObjectives). "via" (0.5.2, #25): "table" (the default), "anvil" (a result taken out of an
 * anvil that has a new or higher enchantment than the item put in) or "any" (both).
 */
public record EnchantItemObjective(Optional<ItemMatcher> item, int count, boolean distinct, String via) implements QuestObjective {
    public static final String TYPE_ID = "justquests:enchant_item";

    public static final MapCodec<EnchantItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(EnchantItemObjective::item),
        Codec.INT.fieldOf("count").forGetter(EnchantItemObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(EnchantItemObjective::distinct),
        Codec.STRING.flatXmap(EnchantItemObjective::checkVia, EnchantItemObjective::checkVia)
            .optionalFieldOf("via", "table").forGetter(EnchantItemObjective::via)
    ).apply(instance, EnchantItemObjective::new));

    private static DataResult<String> checkVia(String via) {
        return via.equals("table") || via.equals("anvil") || via.equals("any")
            ? DataResult.success(via) : DataResult.error(() -> "via must be table, anvil or any, not " + via);
    }

    /** Whether enchanting at an enchanting table counts. */
    public boolean table() {
        return !via.equals("anvil");
    }

    /** Whether enchanting at an anvil counts. */
    public boolean anvil() {
        return !via.equals("table");
    }

    /** Whether the anvil gave the item a new enchantment or a higher level of one it had. */
    public static boolean raised(ItemStack before, ItemStack after) {
        Map<String, Integer> had = ItemExtras.enchantments(before);
        for (Map.Entry<String, Integer> e : ItemExtras.enchantments(after).entrySet()) {
            if (had.getOrDefault(e.getKey(), 0) < e.getValue()) return true;
        }
        return false;
    }

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
        return "Enchant " + count + "x " + item.map(ItemMatcher::label).orElse("any item")
            + (via.equals("table") ? "" : " (via " + via + ")");
    }

    @Override
    public Component display() {
        if (via.equals("anvil")) {
            return item.isPresent()
                ? Msg.tr("justquests.goal.enchant_anvil_count", count, item.get().name())
                : Msg.tr(count == 1 ? "justquests.goal.enchant_anvil_one" : "justquests.goal.enchant_anvil_many", count);
        }
        return item.isPresent()
            ? Msg.tr("justquests.goal.enchant_count", count, item.get().name())
            : Msg.tr(count == 1 ? "justquests.goal.enchant_one" : "justquests.goal.enchant_many", count);
    }
}
