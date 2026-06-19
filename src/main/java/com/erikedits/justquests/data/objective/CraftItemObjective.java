package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Craft X of an item (or any item in a tag). */
public record CraftItemObjective(HolderSet<Item> items, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:craft_item";

    public static final MapCodec<CraftItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemObjectives.ITEM_OR_TAG.fieldOf("item").forGetter(CraftItemObjective::items),
        Codec.INT.fieldOf("count").forGetter(CraftItemObjective::count)
    ).apply(instance, CraftItemObjective::new));

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
        return "Craft " + count + "x " + ObjectiveNames.describe(items);
    }
}
