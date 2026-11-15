package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Right-click a mob X times (0.6.0): shear a sheep, milk a cow, put a saddle on a horse. "item" asks
 * for what the player holds. Clicking the same mob again counts after a second.
 */
public record InteractEntityObjective(EntityMatcher entity, Optional<ItemMatcher> item, int count, boolean distinct)
        implements QuestObjective {
    public static final String TYPE_ID = "justquests:interact_entity";

    public static final MapCodec<InteractEntityObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        EntityMatcher.CODEC.fieldOf("entity").forGetter(InteractEntityObjective::entity),
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(InteractEntityObjective::item),
        Codec.INT.fieldOf("count").forGetter(InteractEntityObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(InteractEntityObjective::distinct)
    ).apply(instance, InteractEntityObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean accepts(EntityType<?> type, ItemStack held) {
        return entity.matches(type) && (item.isEmpty() || item.get().matches(held));
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Interact " + count + "x " + entity.label() + item.map(i -> " with " + i.label()).orElse("");
    }

    @Override
    public Component display() {
        if (item.isPresent()) return Msg.tr("justquests.goal.interact_item_count", item.get().name(), count, entity.name());
        return Msg.tr("justquests.goal.interact_count", count, entity.name());
    }

    @Override
    public Component goal() {
        return item.isPresent() ? Msg.tr("justquests.goal.interact_item", item.get().name(), entity.name())
            : Msg.tr("justquests.goal.interact", entity.name());
    }
}
