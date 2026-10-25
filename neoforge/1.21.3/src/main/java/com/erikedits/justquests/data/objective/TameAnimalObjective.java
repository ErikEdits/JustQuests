package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

/** Tame X animals of a given type. */
public record TameAnimalObjective(EntityMatcher entity, int count, boolean distinct) implements QuestObjective {
    public static final String TYPE_ID = "justquests:tame_animal";

    public static final MapCodec<TameAnimalObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        EntityMatcher.CODEC.fieldOf("entity").forGetter(TameAnimalObjective::entity),
        Codec.INT.fieldOf("count").forGetter(TameAnimalObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(TameAnimalObjective::distinct)
    ).apply(instance, TameAnimalObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean matches(EntityType<?> type) {
        return entity.matches(type);
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Tame " + count + "x " + entity.label();
    }

    @Override
    public Component display() {
        return Msg.tr("justquests.goal.tame_count", count, entity.name());
    }
}
