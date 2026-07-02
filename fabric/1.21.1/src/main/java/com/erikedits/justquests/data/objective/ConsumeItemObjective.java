package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Eat or drink (finish using) X of an item, or any item in a tag. */
public record ConsumeItemObjective(ItemMatcher item, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:consume_item";

    public static final MapCodec<ConsumeItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.fieldOf("item").forGetter(ConsumeItemObjective::item),
        Codec.INT.fieldOf("count").forGetter(ConsumeItemObjective::count)
    ).apply(instance, ConsumeItemObjective::new));

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
        return "Consume " + count + "x " + item.label();
    }

    @Override
    public Component display() {
        return Component.literal("Consume " + count + "x ").append(item.name());
    }
}
