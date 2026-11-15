package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Catch X things with a fishing rod (0.6.0); "item" checks what was caught (fish, treasure, junk). */
public record FishItemObjective(Optional<ItemMatcher> item, int count, boolean distinct) implements QuestObjective {
    public static final String TYPE_ID = "justquests:fish_item";

    public static final MapCodec<FishItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(FishItemObjective::item),
        Codec.INT.fieldOf("count").forGetter(FishItemObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(FishItemObjective::distinct)
    ).apply(instance, FishItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean accepts(ItemStack caught) {
        return item.isEmpty() || item.get().matches(caught);
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Fish " + count + "x " + item.map(ItemMatcher::label).orElse("anything");
    }

    @Override
    public Component display() {
        if (item.isPresent()) return Msg.tr("justquests.goal.fish_count", count, item.get().name());
        return Msg.tr(count == 1 ? "justquests.goal.fish_one" : "justquests.goal.fish_many", count);
    }

    @Override
    public Component goal() {
        return item.isPresent() ? Msg.tr("justquests.goal.fish", item.get().name()) : Msg.tr("justquests.goal.fish_any");
    }
}
