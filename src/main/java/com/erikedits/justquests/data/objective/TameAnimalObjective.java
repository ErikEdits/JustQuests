package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

/** Tame X animals of a given type. */
public record TameAnimalObjective(EntityType<?> entity, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:tame_animal";

    public static final MapCodec<TameAnimalObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(TameAnimalObjective::entity),
        Codec.INT.fieldOf("count").forGetter(TameAnimalObjective::count)
    ).apply(instance, TameAnimalObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean matches(EntityType<?> type) {
        return type == this.entity;
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Tame " + count + "x " + BuiltInRegistries.ENTITY_TYPE.getKey(entity);
    }
}
