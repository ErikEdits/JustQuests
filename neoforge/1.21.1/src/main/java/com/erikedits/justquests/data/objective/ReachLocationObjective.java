package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Reach within a radius of a target position (optionally in a given dimension). */
public record ReachLocationObjective(Optional<ResourceLocation> dimension, int x, int y, int z, int radius)
        implements QuestObjective {
    public static final String TYPE_ID = "justquests:reach_location";

    public static final MapCodec<ReachLocationObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(ReachLocationObjective::dimension),
        Codec.INT.fieldOf("x").forGetter(ReachLocationObjective::x),
        Codec.INT.fieldOf("y").forGetter(ReachLocationObjective::y),
        Codec.INT.fieldOf("z").forGetter(ReachLocationObjective::z),
        Codec.INT.optionalFieldOf("radius", 4).forGetter(ReachLocationObjective::radius)
    ).apply(instance, ReachLocationObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean isAt(ServerPlayer player) {
        if (dimension.isPresent() && !player.level().dimension().location().equals(dimension.get())) {
            return false;
        }
        Vec3 p = player.position();
        double dx = p.x - (x + 0.5);
        double dy = p.y - (y + 0.5);
        double dz = p.z - (z + 0.5);
        return (dx * dx + dy * dy + dz * dz) <= (double) radius * radius;
    }

    @Override
    public int requiredCount() {
        return 1;
    }

    @Override
    public String displayName() {
        return "Reach (" + x + ", " + y + ", " + z + ")";
    }

    @Override
    public Component display() {
        return Component.literal(displayName());
    }
}
