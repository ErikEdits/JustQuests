package com.erikedits.justquests.generator.v2.api;

import java.util.List;

/**
 * What a {@code tick()}, {@code reroll()} or the start-up rotation changed.
 * When {@code changed} is true the mod re-registers {@code servedQuests()} and syncs all players.
 *
 * @param changed       true if {@code servedQuests()} differs from before the call
 * @param reason        {@code "cycle"}, {@code "catch-up"}, {@code "reroll"}, {@code "expiry"} or {@code "none"}
 * @param cycleId       current cycle id (cycle start, epoch seconds); 0 if the generator never ran
 * @param added         quest ids new in the served set
 * @param retained      claimed quests from older cycles still served
 * @param removed       quest ids no longer served
 * @param expiredClaims claims released by expiry; the mod removes these quests from the players
 */
public record RotationResult(boolean changed, String reason, long cycleId, List<String> added,
                             List<String> retained, List<String> removed, List<ExpiredClaim> expiredClaims) {
    /** Canonical constructor; copies all lists. */
    public RotationResult {
        java.util.Objects.requireNonNull(reason, "reason");
        added = List.copyOf(added);
        retained = List.copyOf(retained);
        removed = List.copyOf(removed);
        expiredClaims = List.copyOf(expiredClaims);
    }

    /**
     * An "unchanged" result for the given cycle.
     *
     * @param cycleId the current cycle id
     * @return an unchanged result with reason {@code "none"}
     */
    public static RotationResult none(long cycleId) {
        return new RotationResult(false, "none", cycleId, List.of(), List.of(), List.of(), List.of());
    }
}
