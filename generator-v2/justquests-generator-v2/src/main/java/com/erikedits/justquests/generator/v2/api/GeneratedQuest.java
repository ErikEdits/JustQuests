package com.erikedits.justquests.generator.v2.api;

import com.google.gson.JsonObject;

/**
 * One quest of a {@link SetRequest} set.
 *
 * @param json        the quest in the mod's format (no id yet)
 * @param signature   what makes it a repeat (objective types and targets); keep it for the history
 * @param estMinutes  estimated play time in minutes
 * @param rewardValue value of its rewards (value units, about one minute of play each)
 */
public record GeneratedQuest(JsonObject json, String signature, double estMinutes, double rewardValue) {
}
