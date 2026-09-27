package com.erikedits.justquests.generator.v2.api;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generator settings, built by the mod from the world's {@code settings.json} (§13).
 * Immutable; use {@link #defaults()}, {@link #builder()} or the {@code with*} methods.
 *
 * <p>The canonical constructor only normalises nulls. Range clamping happens in
 * {@link #sanitized(List)}, which the core calls on every config it receives and which reports each
 * adjustment so it can be logged.
 *
 * @param enabled            generator on/off ({@code generatedQuests})
 * @param questsPerCycle     quests per cycle, clamped to 1..{@code maxPerCycle} ({@code generatedCount})
 * @param maxPerCycle        hard cap protecting servers, 1..50 (default 20)
 * @param difficulty         EASY/NORMAL/HARD ({@code difficulty})
 * @param exclusiveClaims    first come, first served ({@code generatorExclusiveClaims})
 * @param oneActivePerPlayer at most one unfinished generated quest per player ({@code generatorOneActivePerPlayer})
 * @param releaseOnAbandon   abandoned current-cycle quests become available again ({@code generatorReleaseOnAbandon})
 * @param claimExpiryHours   0 = never; otherwise claims older than this are released ({@code generatorClaimExpiryHours})
 * @param cycleHours         rotation period in hours, 1..168 ({@code generatorCycleHours})
 * @param cycleAnchorHour    local hour of the first boundary of a day, 0..23 ({@code generatorCycleAnchorHour})
 * @param zone               time zone of the boundaries (default: system zone)
 * @param historyDays        no-repeat window in days, 0..30 (default 6)
 * @param moddedShare        target share 0..1 of quests using modded content ({@code generatorModdedShare})
 * @param disabledProfiles   profile ids that stay off even if their mod is installed ({@code generatorDisabledProfiles})
 * @param adaptiveBalancing  apply self-calibration from stats (§9.9) ({@code generatorAdaptiveBalancing})
 * @param statsEnabled       record test-phase statistics (§15) ({@code generatorStats})
 */
public record GeneratorConfig(boolean enabled, int questsPerCycle, int maxPerCycle, Difficulty difficulty,
                              boolean exclusiveClaims, boolean oneActivePerPlayer, boolean releaseOnAbandon,
                              int claimExpiryHours, int cycleHours, int cycleAnchorHour, ZoneId zone,
                              int historyDays, double moddedShare, Set<String> disabledProfiles,
                              boolean adaptiveBalancing, boolean statsEnabled) {

    /** Largest value accepted for {@code maxPerCycle}. */
    public static final int ABSOLUTE_MAX_PER_CYCLE = 50;

    /** Canonical constructor: replaces nulls with defaults and copies the profile set. */
    public GeneratorConfig {
        difficulty = difficulty == null ? Difficulty.NORMAL : difficulty;
        zone = zone == null ? ZoneId.systemDefault() : zone;
        TreeSet<String> profiles = new TreeSet<>();
        if (disabledProfiles != null) {
            for (String p : disabledProfiles) {
                if (p != null && !p.isBlank()) {
                    profiles.add(p.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        disabledProfiles = Collections.unmodifiableSet(profiles);
    }

    /**
     * The defaults from §13.
     *
     * @return the defaults from §13 (enabled, 5 per cycle, max 20, NORMAL, exclusive, one active,
     *         release on abandon, no expiry, 12 h cycles anchored at 00:00 system time, 6-day history,
     *         35 % modded share, no disabled profiles, no adaptive balancing, stats on)
     */
    public static GeneratorConfig defaults() {
        return new GeneratorConfig(true, 5, 20, Difficulty.NORMAL, true, true, true, 0, 12, 0,
            ZoneId.systemDefault(), 6, 0.35, Set.of(), false, true);
    }

    /**
     * A builder pre-filled with the defaults.
     *
     * @return a builder pre-filled with {@link #defaults()}
     */
    public static Builder builder() {
        return new Builder(defaults());
    }

    /**
     * Sets {@code toBuilder}.
     *
     * @return a builder pre-filled with this config
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Returns a copy with every value clamped into its allowed range.
     *
     * @param adjustments receives one human-readable line per changed value (may be null)
     * @return the sanitised config (this instance if nothing changed)
     */
    public GeneratorConfig sanitized(List<String> adjustments) {
        List<String> notes = adjustments == null ? new ArrayList<>() : adjustments;
        int max = clamp("maxPerCycle", maxPerCycle, 1, ABSOLUTE_MAX_PER_CYCLE, notes);
        int perCycle = clamp("questsPerCycle", questsPerCycle, 1, max, notes);
        int expiry = clamp("claimExpiryHours", claimExpiryHours, 0, 24 * 365, notes);
        int hours = clamp("cycleHours", cycleHours, 1, 168, notes);
        int anchor = clamp("cycleAnchorHour", cycleAnchorHour, 0, 23, notes);
        int history = clamp("historyDays", historyDays, 0, 30, notes);
        double share = moddedShare;
        if (Double.isNaN(share) || share < 0.0 || share > 1.0) {
            double fixed = Double.isNaN(share) ? 0.35 : Math.max(0.0, Math.min(1.0, share));
            notes.add("moddedShare " + share + " -> " + fixed);
            share = fixed;
        }
        GeneratorConfig out = new GeneratorConfig(enabled, perCycle, max, difficulty, exclusiveClaims,
            oneActivePerPlayer, releaseOnAbandon, expiry, hours, anchor, zone, history, share, disabledProfiles,
            adaptiveBalancing, statsEnabled);
        return out.equals(this) ? this : out;
    }

    private static int clamp(String name, int value, int min, int max, List<String> notes) {
        int v = Math.max(min, Math.min(max, value));
        if (v != value) {
            notes.add(name + " " + value + " -> " + v);
        }
        return v;
    }

    /**
     * Returns a copy with {@code enabled} replaced.
     *
     * @param v new value
     * @return a copy with {@code enabled} replaced
     */
    public GeneratorConfig withEnabled(boolean v) {
        return toBuilder().enabled(v).build();
    }

    /**
     * Returns a copy with {@code questsPerCycle} replaced.
     *
     * @param v new value
     * @return a copy with {@code questsPerCycle} replaced
     */
    public GeneratorConfig withQuestsPerCycle(int v) {
        return toBuilder().questsPerCycle(v).build();
    }

    /**
     * Returns a copy with {@code difficulty} replaced.
     *
     * @param v new value
     * @return a copy with {@code difficulty} replaced
     */
    public GeneratorConfig withDifficulty(Difficulty v) {
        return toBuilder().difficulty(v).build();
    }

    /**
     * Returns a copy with {@code exclusiveClaims} replaced.
     *
     * @param v new value
     * @return a copy with {@code exclusiveClaims} replaced
     */
    public GeneratorConfig withExclusiveClaims(boolean v) {
        return toBuilder().exclusiveClaims(v).build();
    }

    /**
     * Returns a copy with {@code oneActivePerPlayer} replaced.
     *
     * @param v new value
     * @return a copy with {@code oneActivePerPlayer} replaced
     */
    public GeneratorConfig withOneActivePerPlayer(boolean v) {
        return toBuilder().oneActivePerPlayer(v).build();
    }

    /**
     * Returns a copy with {@code claimExpiryHours} replaced.
     *
     * @param v new value
     * @return a copy with {@code claimExpiryHours} replaced
     */
    public GeneratorConfig withClaimExpiryHours(int v) {
        return toBuilder().claimExpiryHours(v).build();
    }

    /**
     * Returns a copy with {@code zone} replaced.
     *
     * @param v new value
     * @return a copy with {@code zone} replaced
     */
    public GeneratorConfig withZone(ZoneId v) {
        return toBuilder().zone(v).build();
    }

    /**
     * Returns a copy with {@code moddedShare} replaced.
     *
     * @param v new value
     * @return a copy with {@code moddedShare} replaced
     */
    public GeneratorConfig withModdedShare(double v) {
        return toBuilder().moddedShare(v).build();
    }

    /** Mutable builder for {@link GeneratorConfig}. Not thread-safe. */
    public static final class Builder {
        private boolean enabled;
        private int questsPerCycle;
        private int maxPerCycle;
        private Difficulty difficulty;
        private boolean exclusiveClaims;
        private boolean oneActivePerPlayer;
        private boolean releaseOnAbandon;
        private int claimExpiryHours;
        private int cycleHours;
        private int cycleAnchorHour;
        private ZoneId zone;
        private int historyDays;
        private double moddedShare;
        private Set<String> disabledProfiles;
        private boolean adaptiveBalancing;
        private boolean statsEnabled;

        private Builder(GeneratorConfig c) {
            enabled = c.enabled;
            questsPerCycle = c.questsPerCycle;
            maxPerCycle = c.maxPerCycle;
            difficulty = c.difficulty;
            exclusiveClaims = c.exclusiveClaims;
            oneActivePerPlayer = c.oneActivePerPlayer;
            releaseOnAbandon = c.releaseOnAbandon;
            claimExpiryHours = c.claimExpiryHours;
            cycleHours = c.cycleHours;
            cycleAnchorHour = c.cycleAnchorHour;
            zone = c.zone;
            historyDays = c.historyDays;
            moddedShare = c.moddedShare;
            disabledProfiles = c.disabledProfiles;
            adaptiveBalancing = c.adaptiveBalancing;
            statsEnabled = c.statsEnabled;
        }

        /**
         * Sets {@code enabled}.
         *
         * @param v value
         * @return this builder
         */
        public Builder enabled(boolean v) {
            enabled = v;
            return this;
        }

        /**
         * Sets {@code questsPerCycle}.
         *
         * @param v value
         * @return this builder
         */
        public Builder questsPerCycle(int v) {
            questsPerCycle = v;
            return this;
        }

        /**
         * Sets {@code maxPerCycle}.
         *
         * @param v value
         * @return this builder
         */
        public Builder maxPerCycle(int v) {
            maxPerCycle = v;
            return this;
        }

        /**
         * Sets {@code difficulty}.
         *
         * @param v value
         * @return this builder
         */
        public Builder difficulty(Difficulty v) {
            difficulty = v;
            return this;
        }

        /**
         * Sets {@code exclusiveClaims}.
         *
         * @param v value
         * @return this builder
         */
        public Builder exclusiveClaims(boolean v) {
            exclusiveClaims = v;
            return this;
        }

        /**
         * Sets {@code oneActivePerPlayer}.
         *
         * @param v value
         * @return this builder
         */
        public Builder oneActivePerPlayer(boolean v) {
            oneActivePerPlayer = v;
            return this;
        }

        /**
         * Sets {@code releaseOnAbandon}.
         *
         * @param v value
         * @return this builder
         */
        public Builder releaseOnAbandon(boolean v) {
            releaseOnAbandon = v;
            return this;
        }

        /**
         * Sets {@code claimExpiryHours}.
         *
         * @param v value
         * @return this builder
         */
        public Builder claimExpiryHours(int v) {
            claimExpiryHours = v;
            return this;
        }

        /**
         * Sets {@code cycleHours}.
         *
         * @param v value
         * @return this builder
         */
        public Builder cycleHours(int v) {
            cycleHours = v;
            return this;
        }

        /**
         * Sets {@code cycleAnchorHour}.
         *
         * @param v value
         * @return this builder
         */
        public Builder cycleAnchorHour(int v) {
            cycleAnchorHour = v;
            return this;
        }

        /**
         * Sets {@code zone}.
         *
         * @param v value
         * @return this builder
         */
        public Builder zone(ZoneId v) {
            zone = v;
            return this;
        }

        /**
         * Sets {@code historyDays}.
         *
         * @param v value
         * @return this builder
         */
        public Builder historyDays(int v) {
            historyDays = v;
            return this;
        }

        /**
         * Sets {@code moddedShare}.
         *
         * @param v value
         * @return this builder
         */
        public Builder moddedShare(double v) {
            moddedShare = v;
            return this;
        }

        /**
         * Sets {@code disabledProfiles}.
         *
         * @param v profile ids
         * @return this builder
         */
        public Builder disabledProfiles(Set<String> v) {
            disabledProfiles = v;
            return this;
        }

        /**
         * Sets {@code adaptiveBalancing}.
         *
         * @param v value
         * @return this builder
         */
        public Builder adaptiveBalancing(boolean v) {
            adaptiveBalancing = v;
            return this;
        }

        /**
         * Sets {@code statsEnabled}.
         *
         * @param v value
         * @return this builder
         */
        public Builder statsEnabled(boolean v) {
            statsEnabled = v;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the config (not yet clamped; see {@link GeneratorConfig#sanitized(List)})
         */
        public GeneratorConfig build() {
            return new GeneratorConfig(enabled, questsPerCycle, maxPerCycle, difficulty, exclusiveClaims,
                oneActivePerPlayer, releaseOnAbandon, claimExpiryHours, cycleHours, cycleAnchorHour,
                Objects.requireNonNullElse(zone, ZoneId.systemDefault()), historyDays, moddedShare,
                disabledProfiles, adaptiveBalancing, statsEnabled);
        }
    }
}
