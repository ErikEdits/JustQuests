package com.erikedits.justquests.generator.v2.api;

import com.google.gson.JsonObject;

/**
 * Runs the mod's real quest codec (the same one used for datapack quests) plus the loader rules
 * (at least one objective, every count &gt; 0). The core calls this once for every quest it is about
 * to serve; a failing quest is discarded and replaced.
 */
public interface QuestValidator {
    /**
     * Validates a generated quest definition. Must not keep a reference to {@code questJson}.
     *
     * @param questId   full id, e.g. {@code "justquests:gen/1727431200_3"}
     * @param questJson quest JSON as emitted by the core (§6 format)
     * @return {@code ok=true} if the mod would load the quest
     */
    ValidationResult validate(String questId, JsonObject questJson);
}
