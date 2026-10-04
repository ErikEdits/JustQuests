package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

/** Kill X entities of a given type. */
public record KillMobObjective(EntityMatcher entity, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:kill_mob";

    public static final MapCodec<KillMobObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        EntityMatcher.CODEC.fieldOf("entity").forGetter(KillMobObjective::entity),
        Codec.INT.fieldOf("count").forGetter(KillMobObjective::count)
    ).apply(instance, KillMobObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** Type-specific match (not part of the base interface). */
    public boolean matches(EntityType<?> type) {
        return entity.matches(type);
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Kill " + count + "x " + entity.label();
    }

    @Override
    public Component display() {
        return Component.literal("Kill " + count + "x ").append(entity.name());
    }
}
