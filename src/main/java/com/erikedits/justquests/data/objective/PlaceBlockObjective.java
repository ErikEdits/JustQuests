package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/** Place X blocks of a given type. */
public record PlaceBlockObjective(Block block, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:place_block";

    public static final MapCodec<PlaceBlockObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(PlaceBlockObjective::block),
        Codec.INT.fieldOf("count").forGetter(PlaceBlockObjective::count)
    ).apply(instance, PlaceBlockObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean matches(Block b) {
        return b == this.block;
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Place " + count + "x " + BuiltInRegistries.BLOCK.getKey(block);
    }
}
