package com.erikedits.justquests.generator.v2.internal.catalog;

import java.util.List;

/** Reward table records (rewards.json and profile extensions). */
public final class RewardDefs {
    private RewardDefs() {
    }

    /**
     * An item reward.
     *
     * @param profile       owning profile
     * @param id            item id (never a tag)
     * @param alts          alternative ids
     * @param value         value per unit (≈ minutes of average play)
     * @param tier          content tier of the reward
     * @param max           max count per quest
     * @param stack         stack size fallback (64/16/1)
     * @param family        family of the reward item (never equal to a quest family)
     * @param avoidFamilies extra quest families this reward must not accompany
     * @param weight        selection weight
     * @param minDifficulty lowest difficulty index allowed
     */
    public record Item(String profile, String id, List<String> alts, double value, int tier, int max, int stack,
                       String family, List<String> avoidFamilies, double weight, int minDifficulty) {
    }

    /**
     * A beneficial effect reward.
     *
     * @param profile        owning profile
     * @param id             effect id
     * @param valuePerMinute value of one minute at amplifier 0
     * @param minSeconds     shortest duration
     * @param maxSeconds     longest duration
     * @param amplifier      amplifier
     * @param minDifficulty  lowest difficulty index allowed
     * @param weight         selection weight
     */
    public record Effect(String profile, String id, double valuePerMinute, int minSeconds, int maxSeconds,
                         int amplifier, int minDifficulty, double weight) {
    }

    /**
     * A curated loot-table "mystery" reward.
     *
     * @param profile       owning profile
     * @param id            loot table id
     * @param value         estimated value
     * @param tier          tier
     * @param minDifficulty lowest difficulty index allowed
     * @param weight        selection weight
     */
    public record Loot(String profile, String id, double value, int tier, int minDifficulty, double weight) {
    }
}
