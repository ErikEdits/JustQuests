package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Farmer's Delight (#22, 0.6.0): cut X items on a cutting board. "item" checks what was cut (a cabbage),
 * "tool" what it was cut with (a knife, an axe, shears). Without Farmer's Delight it never counts.
 */
public record CutItemObjective(Optional<ItemMatcher> item, Optional<ItemMatcher> tool, int count, boolean distinct)
        implements QuestObjective {
    public static final String TYPE_ID = "justquests:cut_item";

    public static final MapCodec<CutItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(CutItemObjective::item),
        ItemMatcher.CODEC.optionalFieldOf("tool").forGetter(CutItemObjective::tool),
        Codec.INT.fieldOf("count").forGetter(CutItemObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(CutItemObjective::distinct)
    ).apply(instance, CutItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean accepts(ItemStack cut, ItemStack usedTool) {
        return (item.isEmpty() || item.get().matches(cut)) && (tool.isEmpty() || tool.get().matches(usedTool));
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Cut " + count + "x " + item.map(ItemMatcher::label).orElse("anything") + tool.map(t -> " with " + t.label()).orElse("");
    }

    private Component withTool(Component goal) {
        return tool.isPresent() ? Msg.tr("justquests.goal.with_tool", goal, tool.get().name()) : goal;
    }

    @Override
    public Component display() {
        if (item.isPresent()) return withTool(Msg.tr("justquests.goal.cut_count", count, item.get().name()));
        return withTool(Msg.tr(count == 1 ? "justquests.goal.cut_one" : "justquests.goal.cut_many", count));
    }

    @Override
    public Component goal() {
        return withTool(item.isPresent() ? Msg.tr("justquests.goal.cut", item.get().name()) : Msg.tr("justquests.goal.cut_any"));
    }
}
