package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Reach a given XP level. */
public record ReachLevelObjective(int level) implements QuestObjective {
    public static final String TYPE_ID = "justquests:reach_level";

    public static final MapCodec<ReachLevelObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("level").forGetter(ReachLevelObjective::level)
    ).apply(instance, ReachLevelObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean reached(ServerPlayer player) {
        return player.experienceLevel >= this.level;
    }

    @Override
    public int requiredCount() {
        return 1;
    }

    @Override
    public String displayName() {
        return "Reach level " + level;
    }

    @Override
    public Component display() {
        return Component.literal(displayName());
    }
}
