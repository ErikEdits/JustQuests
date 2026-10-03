package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

/**
 * Use an item X times (id or #tag). Counted from the item's vanilla "Times Used" statistic (see
 * StatObjectives), so only uses that did something count: a thrown snowball, bone meal that was
 * applied, a spyglass look, a placed block, a tool that mined. Statistics count per item type,
 * so filters (enchantments, potion, name) are ignored here.
 */
public record UseItemObjective(ItemMatcher item, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:use_item";

    public static final MapCodec<UseItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.fieldOf("item").forGetter(UseItemObjective::item),
        Codec.INT.fieldOf("count").forGetter(UseItemObjective::count)
    ).apply(instance, UseItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Use " + count + "x " + item.label();
    }

    @Override
    public Component display() {
        return Component.literal("Use " + count + "x ").append(item.name());
    }
}
