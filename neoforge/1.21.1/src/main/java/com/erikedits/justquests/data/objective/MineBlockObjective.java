package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** Break (mine) X blocks of a given type. Distinct from collect_item, which
 *  counts items picked up — this counts the block-break itself. */
public record MineBlockObjective(Block block, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:mine_block";

    public static final MapCodec<MineBlockObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(MineBlockObjective::block),
        Codec.INT.fieldOf("count").forGetter(MineBlockObjective::count)
    ).apply(instance, MineBlockObjective::new));

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
        return "Mine " + count + "x " + BuiltInRegistries.BLOCK.getKey(block);
    }

    @Override
    public Component display() {
        return Component.literal("Mine " + count + "x ").append(block.getName());
    }
}
