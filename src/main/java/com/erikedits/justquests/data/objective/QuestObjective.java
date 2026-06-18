package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

/**
 * Base for all objective types. Matching is type-specific (an item
 * objective matches an ItemStack, a kill objective matches an EntityType,
 * etc.), so the base only exposes the shared metadata. Event handlers
 * route to the concrete type via instanceof and a QuestProgressService
 * test (see PlayerQuestEvents / QuestProgressService).
 */
public interface QuestObjective {
    Codec<QuestObjective> CODEC = Codec.STRING.dispatch(
        "type",
        QuestObjective::typeId,
        QuestObjective::codecForType
    );

    static MapCodec<? extends QuestObjective> codecForType(String type) {
        return switch (type) {
            case CollectItemObjective.TYPE_ID -> CollectItemObjective.MAP_CODEC;
            case KillMobObjective.TYPE_ID -> KillMobObjective.MAP_CODEC;
            default -> throw new IllegalStateException("Unknown objective type: " + type);
        };
    }

    String typeId();

    int requiredCount();

    String displayName();
}
