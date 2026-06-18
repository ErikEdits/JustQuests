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
            case PlaceBlockObjective.TYPE_ID -> PlaceBlockObjective.MAP_CODEC;
            case CraftItemObjective.TYPE_ID -> CraftItemObjective.MAP_CODEC;
            case TameAnimalObjective.TYPE_ID -> TameAnimalObjective.MAP_CODEC;
            case GainAdvancementObjective.TYPE_ID -> GainAdvancementObjective.MAP_CODEC;
            case VisitDimensionObjective.TYPE_ID -> VisitDimensionObjective.MAP_CODEC;
            case ReachLevelObjective.TYPE_ID -> ReachLevelObjective.MAP_CODEC;
            case ReachLocationObjective.TYPE_ID -> ReachLocationObjective.MAP_CODEC;
            default -> throw new IllegalStateException("Unknown objective type: " + type);
        };
    }

    String typeId();

    int requiredCount();

    String displayName();
}
