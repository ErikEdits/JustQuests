package com.erikedits.justquests.generator.v2.api;

/** Claim state of one generated quest (§10). */
public enum ClaimState {
    /** Anyone may accept it. */
    AVAILABLE,
    /** Held by a player (with exclusive claims: locked for everyone else). */
    CLAIMED,
    /** Finished (terminal); leaves the served set at the next rotation. */
    COMPLETED
}
