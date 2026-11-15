package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Farmer's Delight (#22, 0.6.0): cook X meals. "item" checks the meal, "via" how it was made: cooking_pot
 * (taken out of the pot or served into a bowl), skillet (placed or in the hand), stove or feast (a serving
 * taken from a feast). Without Farmer's Delight it never counts.
 */
public record CookItemObjective(Optional<ItemMatcher> item, Optional<String> via, int count, boolean distinct)
        implements QuestObjective {
    public static final String TYPE_ID = "justquests:cook_item";
    public static final List<String> VIA = List.of("cooking_pot", "skillet", "stove", "feast");

    public static final MapCodec<CookItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(CookItemObjective::item),
        // cooking_pot, skillet, stove, feast or any (another word never matches)
        Codec.STRING.optionalFieldOf("via").forGetter(CookItemObjective::via),
        Codec.INT.fieldOf("count").forGetter(CookItemObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(CookItemObjective::distinct)
    ).apply(instance, CookItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** A meal made the way how says (cooking_pot, skillet, stove, feast). */
    public boolean accepts(ItemStack meal, String how) {
        return (item.isEmpty() || item.get().matches(meal)) && (via.isEmpty() || via.get().equals("any") || via.get().equals(how));
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Cook " + count + "x " + item.map(ItemMatcher::label).orElse("any meal") + via.map(v -> " via " + v).orElse("");
    }

    private Component withVia(Component goal) {
        if (via.isEmpty() || via.get().equals("any")) return goal;
        return Msg.tr("justquests.goal.with_extras", goal, Msg.tr("justquests.goal.via." + via.get()));
    }

    @Override
    public Component display() {
        if (item.isPresent()) return withVia(Msg.tr("justquests.goal.cook_count", count, item.get().name()));
        return withVia(Msg.tr(count == 1 ? "justquests.goal.cook_one" : "justquests.goal.cook_many", count));
    }

    @Override
    public Component goal() {
        return withVia(item.isPresent() ? Msg.tr("justquests.goal.cook", item.get().name()) : Msg.tr("justquests.goal.cook_any"));
    }
}
