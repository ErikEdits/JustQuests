package com.erikedits.justquests.data.objective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;

/**
 * Base for all objective types. Matching is type-specific (an item
 * objective matches an ItemStack, a kill objective matches an EntityType,
 * etc.), so the base only exposes the shared metadata. Event handlers
 * route to the concrete type via instanceof and a QuestProgressService
 * test (see PlayerQuestEvents / QuestProgressService).
 */
public interface QuestObjective {
    // Pre-1.20.5 DFU's dispatch takes a Codec (not a MapCodec); MapCodec.codec()
    // keeps the same inline {"type":..., ...fields} JSON shape.
    Codec<QuestObjective> CODEC = Codec.STRING.dispatch(
        "type",
        QuestObjective::typeId,
        type -> codecForType(type).codec()
    );

    /** The objective types other mods added with {@link #register}. */
    java.util.Map<String, MapCodec<? extends QuestObjective>> ADDED = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Adds an objective type for an addon (the JustQuests Create Addon, for example). Call it while
     * the mod loads, before any quest is read. The addon counts its objective itself with
     * {@link com.erikedits.justquests.progress.QuestProgressService#advance}.
     */
    static void register(String type, MapCodec<? extends QuestObjective> codec) {
        ADDED.put(type, codec);
    }

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
            case MineBlockObjective.TYPE_ID -> MineBlockObjective.MAP_CODEC;
            case BreedAnimalObjective.TYPE_ID -> BreedAnimalObjective.MAP_CODEC;
            case ConsumeItemObjective.TYPE_ID -> ConsumeItemObjective.MAP_CODEC;
            case SmeltItemObjective.TYPE_ID -> SmeltItemObjective.MAP_CODEC;
            case EnchantItemObjective.TYPE_ID -> EnchantItemObjective.MAP_CODEC;
            case UseItemObjective.TYPE_ID -> UseItemObjective.MAP_CODEC;
            case HaveItemObjective.TYPE_ID -> HaveItemObjective.MAP_CODEC;
            case AnvilItemObjective.TYPE_ID -> AnvilItemObjective.MAP_CODEC;
            case GrindstoneItemObjective.TYPE_ID -> GrindstoneItemObjective.MAP_CODEC;
            default -> {
                MapCodec<? extends QuestObjective> added = ADDED.get(type);
                if (added == null) throw new IllegalStateException("Unknown objective type: " + type + " (does it need an addon?)");
                yield added;
            }
        };
    }

    String typeId();

    /** "distinct": every kind of item, block or mob counts only once (eat 10 different foods). */
    default boolean distinct() {
        return false;
    }

    /** {@link #display()}, marked "(all different)" for a distinct objective. */
    static Component shown(QuestObjective o) {
        return o.distinct() ? com.erikedits.justquests.text.Msg.tr("justquests.goal.distinct", o.display()) : o.display();
    }

    int requiredCount();

    /** Plain English, for logs and diagnostics (Q22). */
    String displayName();

    /**
     * Player-facing label. The connective words are English (Q22), but the
     * content name (item/mob/block) is a vanilla translatable component, so
     * it localizes to the player's client language for free (Q21).
     */
    Component display();

    /** The goal without its count, as the quest book shows it ("5/8" stands in front). */
    default Component goal() {
        return display();
    }

    /** The item the quest book shows for this objective, or null. Only asked for types of addons. */
    default net.minecraft.world.item.Item icon() {
        return null;
    }
}
