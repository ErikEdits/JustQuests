package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * Right-click a block X times (0.6.0): ring a bell, open a chest, use a composter. "item" asks for
 * what the player holds. Clicking the same block again counts after a second.
 */
public record UseBlockObjective(BlockMatcher block, Optional<ItemMatcher> item, int count, boolean distinct)
        implements QuestObjective {
    public static final String TYPE_ID = "justquests:use_block";

    public static final MapCodec<UseBlockObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BlockMatcher.CODEC.fieldOf("block").forGetter(UseBlockObjective::block),
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(UseBlockObjective::item),
        Codec.INT.fieldOf("count").forGetter(UseBlockObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(UseBlockObjective::distinct)
    ).apply(instance, UseBlockObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean accepts(Block used, ItemStack held) {
        return block.matches(used) && (item.isEmpty() || item.get().matches(held));
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Use " + count + "x " + block.label() + item.map(i -> " with " + i.label()).orElse("");
    }

    @Override
    public Component display() {
        if (item.isPresent()) return Msg.tr("justquests.goal.use_block_item_count", item.get().name(), count, block.name());
        return Msg.tr("justquests.goal.use_block_count", count, block.name());
    }

    @Override
    public Component goal() {
        return item.isPresent() ? Msg.tr("justquests.goal.use_block_item", item.get().name(), block.name())
            : Msg.tr("justquests.goal.use_block", block.name());
    }
}
