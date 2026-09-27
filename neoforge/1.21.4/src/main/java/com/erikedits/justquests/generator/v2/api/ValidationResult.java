package com.erikedits.justquests.generator.v2.api;

/**
 * Result of {@link QuestValidator#validate}.
 *
 * @param ok      true if the quest parses and passes the loader rules
 * @param message error text if not ok (may be null or empty when ok)
 */
public record ValidationResult(boolean ok, String message) {
    /** Shared success instance. */
    public static final ValidationResult OK = new ValidationResult(true, "");

    /**
     * Creates a failed result.
     *
     * @param message the reason
     * @return a failed result
     */
    public static ValidationResult fail(String message) {
        return new ValidationResult(false, message);
    }
}
