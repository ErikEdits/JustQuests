package com.erikedits.justquests.generator.v2.internal.util;

/** Rounds counts to human-friendly values (§9.3). */
public final class NiceNumbers {
    private static final int[] GENERAL = {1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96, 128};
    private static final int[] STACK64 = {1, 2, 3, 4, 5, 6, 8, 16, 24, 32, 40, 48, 64, 80, 96, 128};
    private static final int[] STACK16 = {1, 2, 3, 4, 8, 12, 16, 20, 24, 32, 48, 64};
    private static final int[] UNSTACKABLE = {1, 2, 3, 4, 5};
    /** Larger friendly values, only for long sets (weekly quests, server goals). */
    private static final int[] GENERAL_LARGE = {1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96, 128,
        160, 192, 256, 320, 384, 448, 512, 640, 768, 1000, 1200, 1500, 2000, 2500, 3000, 4000, 5000, 6000, 7500,
        10000, 12500, 15000, 20000, 25000, 30000, 40000, 50000, 75000, 100000};
    private static final int[] STACK16_LARGE = {1, 2, 3, 4, 8, 12, 16, 20, 24, 32, 48, 64, 80, 96, 128, 160, 192, 256,
        320, 400, 500, 640, 800, 1000, 1200, 1600, 2000, 2500, 3200, 4000, 5000, 6400, 8000, 10000};
    private static final int[] UNSTACKABLE_LARGE = {1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 16, 20, 24, 25, 30, 32, 40, 48,
        50, 64, 80, 100};

    private NiceNumbers() {
    }

    /**
     * Nearest friendly value (in log space) to {@code raw}, restricted to [min, max].
     *
     * @param raw       ideal count (may be fractional)
     * @param min       lower bound (&ge; 1)
     * @param max       upper bound (&ge; min)
     * @param stackSize 64, 16, 1 for items; 0 for blocks/entities (general scale)
     * @return a count in [min, max]
     */
    public static int round(double raw, int min, int max, int stackSize) {
        return round(raw, min, max, stackSize, false);
    }

    /**
     * As {@link #round(double, int, int, int)}; {@code large} adds friendly values above 128 for
     * long sets.
     *
     * @param raw       ideal count (may be fractional)
     * @param min       lower bound (&ge; 1)
     * @param max       upper bound (&ge; min)
     * @param stackSize 64, 16, 1 for items; 0 for blocks/entities (general scale)
     * @param large     use the larger scales
     * @return a count in [min, max]
     */
    public static int round(double raw, int min, int max, int stackSize, boolean large) {
        int lo = Math.max(1, min);
        int hi = Math.max(lo, max);
        int[] scale = large
            ? (stackSize == 1 ? UNSTACKABLE_LARGE : stackSize == 16 ? STACK16_LARGE : GENERAL_LARGE)
            : (stackSize == 1 ? UNSTACKABLE : stackSize == 16 ? STACK16 : stackSize >= 64 ? STACK64 : GENERAL);
        double target = Math.max(1.0, raw);
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int v : scale) {
            if (v < lo || v > hi) {
                continue;
            }
            double d = Math.abs(Math.log(v) - Math.log(target));
            if (d < bestDist - 1e-12) {
                bestDist = d;
                best = v;
            }
        }
        if (best < 0) {
            // no friendly value inside the bounds: fall back to the clamped raw value
            return (int) Math.max(lo, Math.min(hi, Math.round(target)));
        }
        return best;
    }
}
