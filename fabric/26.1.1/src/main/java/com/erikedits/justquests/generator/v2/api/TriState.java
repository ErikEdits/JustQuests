package com.erikedits.justquests.generator.v2.api;

/**
 * Three-valued host answer. {@link #UNKNOWN} means "cannot answer cheaply on this build";
 * the core then trusts its bundled catalog. Only a definite {@link #NO} overrides the catalog.
 */
public enum TriState {
    /** The host confirmed the property. */
    YES,
    /** The host is certain the property does not hold; the core drops the candidate. */
    NO,
    /** The host cannot tell; the catalog decides. */
    UNKNOWN;

    /**
     * Converts a boolean to {@link #YES}/{@link #NO}.
     *
     * @param value the boolean
     * @return {@code YES} for true, {@code NO} for false
     */
    public static TriState of(boolean value) {
        return value ? YES : NO;
    }
}
