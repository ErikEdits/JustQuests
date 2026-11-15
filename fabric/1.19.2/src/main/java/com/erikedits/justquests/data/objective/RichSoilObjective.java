package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

/**
 * Farmer's Delight (#22, 0.6.0): organic compost the player put down turns into rich soil, X times.
 * Without Farmer's Delight it never counts.
 */
public record RichSoilObjective(int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:rich_soil";

    public static final MapCodec<RichSoilObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.optionalFieldOf("count", 1).forGetter(RichSoilObjective::count)
    ).apply(instance, RichSoilObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Make rich soil " + count + "x";
    }

    @Override
    public Component display() {
        return Msg.tr(count == 1 ? "justquests.goal.rich_soil_one" : "justquests.goal.rich_soil_many", count);
    }

    @Override
    public Component goal() {
        return Msg.tr("justquests.goal.rich_soil");
    }
}
