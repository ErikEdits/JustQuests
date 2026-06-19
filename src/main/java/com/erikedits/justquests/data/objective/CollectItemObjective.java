package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Collect X of an item (or any item in a tag). The "item" field accepts a
 * single id (`minecraft:oak_log`), a list of ids, or a tag
 * (`#minecraft:logs`) — Q38.
 */
public record CollectItemObjective(HolderSet<Item> items, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:collect_item";

    public static final MapCodec<CollectItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemObjectives.ITEM_OR_TAG.fieldOf("item").forGetter(CollectItemObjective::items),
        Codec.INT.fieldOf("count").forGetter(CollectItemObjective::count)
    ).apply(instance, CollectItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean matches(ItemStack stack) {
        return items.contains(stack.getItemHolder());
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Collect " + count + "x " + ObjectiveNames.describe(items);
    }
}
