package com.erikedits.justquests.generator.v2.api;

/**
 * Outcome of {@link com.erikedits.justquests.generator.v2.QuestGeneratorV2#tryClaim}.
 * {@link #OK} and {@link #ALREADY_YOURS} mean "proceed with the accept"; everything else except
 * {@link #NOT_GENERATED} means "deny with {@link #denyMessage()}".
 */
public enum ClaimResult {
    /** Claimed now; the mod adds the quest to the player. */
    OK(null),
    /** The player already holds it; the mod proceeds (idempotent). */
    ALREADY_YOURS(null),
    /** Deny: another player holds it. */
    CLAIMED_BY_OTHER("Another player already took this quest."),
    /** Deny: one-active rule. */
    HAS_ACTIVE_GENERATED("Finish your current generated quest first."),
    /** Deny: already finished. */
    COMPLETED("This quest was already completed."),
    /** Not a generated quest; the mod ignores claims entirely. */
    NOT_GENERATED(null),
    /** Deny: a generated id that is no longer offered. */
    NOT_SERVED("This generated quest is no longer available."),
    /** Deny: the generator is disabled in this world. */
    DISABLED("Generated quests are disabled in this world.");

    private final String denyMessage;

    ClaimResult(String denyMessage) {
        this.denyMessage = denyMessage;
    }

    /**
     * Whether the accept may continue.
     *
     * @return true if the mod should continue adding the quest to the player
     *         ({@link #OK}, {@link #ALREADY_YOURS}, {@link #NOT_GENERATED})
     */
    public boolean proceed() {
        return this == OK || this == ALREADY_YOURS || this == NOT_GENERATED;
    }

    /**
     * The suggested player-facing text when the accept is denied.
     *
     * @return the suggested English player-facing deny message, or null when {@link #proceed()}
     */
    public String denyMessage() {
        return denyMessage;
    }
}
