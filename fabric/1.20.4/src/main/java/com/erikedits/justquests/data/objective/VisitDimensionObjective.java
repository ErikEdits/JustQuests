package com.erikedits.justquests.data.objective;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Enter a given dimension (vanilla or modded, matched by id). */
public record VisitDimensionObjective(ResourceLocation dimension) implements QuestObjective {
    public static final String TYPE_ID = "justquests:visit_dimension";

    public static final MapCodec<VisitDimensionObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("dimension").forGetter(VisitDimensionObjective::dimension)
    ).apply(instance, VisitDimensionObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean matches(ResourceLocation dim) {
        return dim.equals(this.dimension);
    }

    @Override
    public int requiredCount() {
        return 1;
    }

    @Override
    public String displayName() {
        return "Visit " + dimension;
    }

    @Override
    public Component display() {
        return Component.literal(displayName());
    }
}
