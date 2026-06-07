package com.erikedits.justquests.data.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;

public interface QuestReward {
    Codec<QuestReward> CODEC = Codec.STRING.dispatch(
        "type",
        QuestReward::typeId,
        QuestReward::codecForType
    );

    static MapCodec<? extends QuestReward> codecForType(String type) {
        return switch (type) {
            case GiveItemReward.TYPE_ID -> GiveItemReward.MAP_CODEC;
            default -> throw new IllegalStateException("Unknown reward type: " + type);
        };
    }

    String typeId();

    void grant(ServerPlayer player);

    String displayName();
}
