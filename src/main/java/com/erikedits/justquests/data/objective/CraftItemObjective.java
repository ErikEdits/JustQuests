package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Craft X of a given item. */
public record CraftItemObjective(Item item, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:craft_item";

    public static final MapCodec<CraftItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(CraftItemObjective::item),
        Codec.INT.fieldOf("count").forGetter(CraftItemObjective::count)
    ).apply(instance, CraftItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean matches(ItemStack stack) {
        return stack.is(item);
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Craft " + count + "x " + BuiltInRegistries.ITEM.getKey(item);
    }
}
