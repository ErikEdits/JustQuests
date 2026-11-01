package com.erikedits.justquests.create;

import com.erikedits.justquests.data.objective.ItemMatcher;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Make X of an item (or any item in a tag) with one kind of Create machine. */
public record MachineObjective(Machine machine, ItemMatcher item, int count, boolean distinct) implements QuestObjective {

    public static MapCodec<MachineObjective> codec(Machine machine) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemMatcher.CODEC.fieldOf("item").forGetter(MachineObjective::item),
            Codec.INT.fieldOf("count").forGetter(MachineObjective::count),
            // every kind of item counts only once
            Codec.BOOL.optionalFieldOf("distinct", false).forGetter(MachineObjective::distinct)
        ).apply(instance, (item, count, distinct) -> new MachineObjective(machine, item, count, distinct)));
    }

    public boolean matches(Machine by, ItemStack stack) {
        return machine == by && item.matches(stack);
    }

    @Override
    public String typeId() {
        return machine.typeId();
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return machine.name().toLowerCase(java.util.Locale.ROOT) + " " + count + "x " + item.label();
    }

    @Override
    public Component display() {
        return Component.translatable(machine.key() + "_count", count, item.name());
    }

    @Override
    public Component goal() {
        return Component.translatable(machine.key(), item.name());
    }

    @Override
    public Item icon() {
        List<Item> items = item.items();
        return items.isEmpty() ? null : items.get(0);
    }
}
