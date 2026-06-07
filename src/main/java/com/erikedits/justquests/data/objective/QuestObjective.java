package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;

public interface QuestObjective {
    Codec<QuestObjective> CODEC = Codec.STRING.dispatch(
        "type",
        QuestObjective::typeId,
        QuestObjective::codecForType
    );

    static MapCodec<? extends QuestObjective> codecForType(String type) {
        return switch (type) {
            case CollectItemObjective.TYPE_ID -> CollectItemObjective.MAP_CODEC;
            default -> throw new IllegalStateException("Unknown objective type: " + type);
        };
    }

    String typeId();

    boolean matches(ItemStack stack);

    int requiredCount();

    String displayName();
}
