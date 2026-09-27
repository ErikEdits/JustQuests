package com.erikedits.justquests.generator.v2.api;

import java.util.Locale;
import java.util.Optional;

/** World difficulty for generated quests; one value per world, set by an OP. */
public enum Difficulty {
    /** Short, surface-level, early tools. */
    EASY,
    /** The default. */
    NORMAL,
    /** Expeditions: deeper tiers, more objectives, better rewards. */
    HARD;

    /**
     * Parses {@code "easy"|"normal"|"hard"} (case-insensitive).
     *
     * @param s text, may be null
     * @return the difficulty, or empty if not recognised
     */
    public static Optional<Difficulty> parse(String s) {
        if (s == null) {
            return Optional.empty();
        }
        switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "easy":
                return Optional.of(EASY);
            case "normal":
                return Optional.of(NORMAL);
            case "hard":
                return Optional.of(HARD);
            default:
                return Optional.empty();
        }
    }

    /**
     * The value as written in {@code settings.json}.
     *
     * @return lower-case settings value ({@code "easy"}, {@code "normal"}, {@code "hard"})
     */
    public String settingsValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
