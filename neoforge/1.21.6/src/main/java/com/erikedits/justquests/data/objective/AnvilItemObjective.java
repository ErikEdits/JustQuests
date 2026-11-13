package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Take X results out of an anvil (#25): enchanting with a book, combining two items, repairing or
 * renaming. "item" checks what comes out (an item filter can ask for its enchantments), "with" what
 * lay in the right slot, such as "minecraft:enchanted_book". Both are optional.
 */
public record AnvilItemObjective(Optional<ItemMatcher> item, Optional<ItemMatcher> with, int count, boolean distinct)
        implements QuestObjective {
    public static final String TYPE_ID = "justquests:anvil_item";

    public static final MapCodec<AnvilItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(AnvilItemObjective::item),
        ItemMatcher.CODEC.optionalFieldOf("with").forGetter(AnvilItemObjective::with),
        Codec.INT.fieldOf("count").forGetter(AnvilItemObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(AnvilItemObjective::distinct)
    ).apply(instance, AnvilItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** Whether taking result out of an anvil, with right in the right slot, counts. */
    public boolean accepts(ItemStack result, ItemStack right) {
        return (item.isEmpty() || item.get().matches(result))
            && (with.isEmpty() || (!right.isEmpty() && with.get().matches(right)));
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Anvil " + count + "x " + item.map(ItemMatcher::label).orElse("any item")
            + with.map(w -> " with " + w.label()).orElse("");
    }

    @Override
    public Component display() {
        if (item.isPresent() && with.isPresent()) return Msg.tr("justquests.goal.anvil_combine_count", count, item.get().name(), with.get().name());
        if (item.isPresent()) return Msg.tr("justquests.goal.anvil_make_count", count, item.get().name());
        if (with.isPresent()) return Msg.tr("justquests.goal.anvil_with_count", count, with.get().name());
        return Msg.tr(count == 1 ? "justquests.goal.anvil_one" : "justquests.goal.anvil_many", count);
    }

    @Override
    public Component goal() {
        if (item.isPresent() && with.isPresent()) return Msg.tr("justquests.goal.anvil_combine", item.get().name(), with.get().name());
        if (item.isPresent()) return Msg.tr("justquests.goal.anvil_make", item.get().name());
        if (with.isPresent()) return Msg.tr("justquests.goal.anvil_with", with.get().name());
        return Msg.tr("justquests.goal.anvil_any");
    }
}
