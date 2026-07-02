package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

/** Breed X animals of a given type. */
public record BreedAnimalObjective(EntityType<?> entity, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:breed_animal";

    public static final MapCodec<BreedAnimalObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Registry.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(BreedAnimalObjective::entity),
        Codec.INT.fieldOf("count").forGetter(BreedAnimalObjective::count)
    ).apply(instance, BreedAnimalObjective::new));

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
        return "Breed " + count + "x " + Registry.ENTITY_TYPE.getKey(entity);
    }

    @Override
    public Component display() {
        return new net.minecraft.network.chat.TextComponent("Breed " + count + "x ").append(entity.getDescription());
    }
}
