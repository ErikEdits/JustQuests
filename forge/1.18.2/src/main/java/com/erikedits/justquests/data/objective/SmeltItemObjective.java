package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Smelt (take from a furnace output) X of an item, or any item in a tag. */
public record SmeltItemObjective(ItemMatcher item, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:smelt_item";

    public static final MapCodec<SmeltItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.fieldOf("item").forGetter(SmeltItemObjective::item),
        Codec.INT.fieldOf("count").forGetter(SmeltItemObjective::count)
    ).apply(instance, SmeltItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean matches(ItemStack stack) {
        return item.matches(stack);
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Smelt " + count + "x " + item.label();
    }

    @Override
    public Component display() {
        return new net.minecraft.network.chat.TextComponent("Smelt " + count + "x ").append(item.name());
    }
}
