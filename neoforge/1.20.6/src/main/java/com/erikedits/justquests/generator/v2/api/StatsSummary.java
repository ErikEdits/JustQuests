package com.erikedits.justquests.generator.v2.api;

import java.util.Locale;
import java.util.Map;

/**
 * Anonymous test-phase statistics (§15). No player UUIDs or names are recorded.
 *
 * @param enabled                   whether stats recording is on
 * @param questsGenerated           quests generated since stats began
 * @param questsClaimed             claims (a quest claimed twice counts twice)
 * @param questsCompleted           completions
 * @param questsAbandoned           abandons
 * @param questsExpired             claims released by expiry
 * @param byObjectiveType           buckets keyed by objective type ({@code "collect_item"} …)
 * @param byTier                    buckets keyed by tier ({@code "0"} … {@code "4"})
 * @param byDifficulty              buckets keyed by difficulty ({@code "EASY"} …)
 * @param byProfile                 buckets keyed by profile ({@code "vanilla"}, {@code "farmersdelight"} …)
 * @param medianClaimToCompleteMin  median observed minutes from claim to completion; NaN without data
 * @param medianEstimatedMin        median estimated minutes of completed quests; NaN without data
 * @param calibrationRatio          median of (observed / estimated) over completed quests; NaN without data
 * @param calibrationByFamilyType   observed/estimated median per {@code family|type} key (&ge; 1 sample)
 * @param abandonRate               abandons / claims (0 without claims)
 * @param relaxations               how often each set-rule relaxation fired
 * @param rejectionsByStep          candidates rejected per achievability pipeline step
 */
public record StatsSummary(boolean enabled, long questsGenerated, long questsClaimed, long questsCompleted,
                           long questsAbandoned, long questsExpired, Map<String, Bucket> byObjectiveType,
                           Map<String, Bucket> byTier, Map<String, Bucket> byDifficulty, Map<String, Bucket> byProfile,
                           double medianClaimToCompleteMin, double medianEstimatedMin, double calibrationRatio,
                           Map<String, Double> calibrationByFamilyType, double abandonRate,
                           Map<String, Long> relaxations, Map<String, Long> rejectionsByStep) {

    /** Canonical constructor; copies all maps (iteration order preserved). */
    public StatsSummary {
        byObjectiveType = copy(byObjectiveType);
        byTier = copy(byTier);
        byDifficulty = copy(byDifficulty);
        byProfile = copy(byProfile);
        calibrationByFamilyType = copy(calibrationByFamilyType);
        relaxations = copy(relaxations);
        rejectionsByStep = copy(rejectionsByStep);
    }

    private static <V> Map<String, V> copy(Map<String, V> m) {
        return m == null ? Map.of() : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(m));
    }

    /**
     * Counters for one grouping key.
     *
     * @param generated quests generated
     * @param claimed   claims
     * @param completed completions
     * @param abandoned abandons
     * @param expired   expiries
     */
    public record Bucket(long generated, long claimed, long completed, long abandoned, long expired) {
        /**
         * Share of claims that were completed.
         *
         * @return completed / claimed, or 0 without claims
         */
        public double completionRate() {
            return claimed == 0 ? 0.0 : (double) completed / claimed;
        }

        /**
         * Share of generated quests that were claimed.
         *
         * @return claimed / generated, or 0 without quests
         */
        public double claimRate() {
            return generated == 0 ? 0.0 : (double) claimed / generated;
        }
    }

    /**
     * Formats the summary for chat or console.
     *
     * @return a multi-line English printout for chat or console
     */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        if (!enabled) {
            sb.append("Generator stats are disabled (generatorStats=false).\n");
        }
        sb.append(String.format(Locale.ROOT, "Generated %d, claimed %d, completed %d, abandoned %d, expired %d%n",
            questsGenerated, questsClaimed, questsCompleted, questsAbandoned, questsExpired));
        sb.append(String.format(Locale.ROOT, "Abandon rate %.0f%%; median claim->complete %s min vs estimate %s min; calibration %s%n",
            abandonRate * 100.0, fmt(medianClaimToCompleteMin), fmt(medianEstimatedMin), fmt(calibrationRatio)));
        appendBuckets(sb, "By objective type", byObjectiveType);
        appendBuckets(sb, "By tier", byTier);
        appendBuckets(sb, "By difficulty", byDifficulty);
        appendBuckets(sb, "By profile", byProfile);
        if (!calibrationByFamilyType.isEmpty()) {
            sb.append("Calibration (observed/estimated) by family|type:\n");
            calibrationByFamilyType.forEach((k, v) -> sb.append("  ").append(k).append(' ').append(fmt(v)).append('\n'));
        }
        if (!relaxations.isEmpty()) {
            sb.append("Relaxations: ").append(relaxations).append('\n');
        }
        if (!rejectionsByStep.isEmpty()) {
            sb.append("Rejected candidates by step: ").append(rejectionsByStep).append('\n');
        }
        return sb.toString();
    }

    private static void appendBuckets(StringBuilder sb, String title, Map<String, Bucket> m) {
        if (m.isEmpty()) {
            return;
        }
        sb.append(title).append(":\n");
        m.forEach((k, b) -> sb.append(String.format(Locale.ROOT,
            "  %-16s gen %4d  claimed %4d  done %4d  abandoned %3d  expired %3d  completion %3.0f%%%n",
            k, b.generated, b.claimed, b.completed, b.abandoned, b.expired, b.completionRate() * 100.0)));
    }

    private static String fmt(double d) {
        return Double.isNaN(d) ? "n/a" : String.format(Locale.ROOT, "%.2f", d);
    }
}
