package com.erikedits.justquests.generator.v2.api;

import java.util.UUID;

/**
 * Snapshot of a quest's claim for list/GUI/sync.
 *
 * <p>With exclusive claims off several players may hold the same quest; {@code holder} is then the
 * earliest holder and {@link com.erikedits.justquests.generator.v2.QuestGeneratorV2#holders} lists
 * all of them.
 *
 * @param state             claim state
 * @param holder            the (first) holding player; null when AVAILABLE; the completing player
 *                          when COMPLETED (exclusive mode)
 * @param claimedAtMillis   epoch ms of the claim, 0 if none
 * @param completedAtMillis epoch ms of completion, 0 if not completed
 */
public record ClaimView(ClaimState state, UUID holder, long claimedAtMillis, long completedAtMillis) {
    /** The view for unknown or free quests. */
    public static final ClaimView AVAILABLE = new ClaimView(ClaimState.AVAILABLE, null, 0L, 0L);

    /** Canonical constructor; {@code state} must not be null. */
    public ClaimView {
        java.util.Objects.requireNonNull(state, "state");
    }
}
