package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record CollectItemObjective(Item item, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:collect_item";

    public static final MapCodec<CollectItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(CollectItemObjective::item),
        Codec.INT.fieldOf("count").forGetter(CollectItemObjective::count)
    ).apply(instance, CollectItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.is(item);
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Collect " + count + "x " + BuiltInRegistries.ITEM.getKey(item);
    }
}
