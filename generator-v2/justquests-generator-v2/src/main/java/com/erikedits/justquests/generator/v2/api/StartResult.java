package com.erikedits.justquests.generator.v2.api;

import java.util.List;

/**
 * Result of {@code start()}.
 *
 * @param deadQuestIds    generated quest ids that players hold but whose definition is unknown; the
 *                        mod removes them from every player's active quests
 * @param releasedClaims  claims in the saved state whose holder no longer had the quest active
 *                        (informational; nothing to remove)
 * @param initialRotation the rotation performed during start (reason {@code "none"} if the saved
 *                        cycle is still current)
 */
public record StartResult(List<String> deadQuestIds, List<ExpiredClaim> releasedClaims,
                          RotationResult initialRotation) {
    /** Canonical constructor; copies the lists. */
    public StartResult {
        deadQuestIds = List.copyOf(deadQuestIds);
        releasedClaims = List.copyOf(releasedClaims);
        java.util.Objects.requireNonNull(initialRotation, "initialRotation");
    }
}
