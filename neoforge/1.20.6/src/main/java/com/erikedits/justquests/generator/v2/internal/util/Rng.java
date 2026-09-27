package com.erikedits.justquests.generator.v2.internal.util;

import java.util.List;
import java.util.SplittableRandom;
import java.util.function.ToDoubleFunction;

/** The single seeded random source of one generation run. Deterministic for a given seed. */
public final class Rng {
    private final SplittableRandom random;
    private final long seed;

    public Rng(long seed) {
        this.seed = seed;
        this.random = new SplittableRandom(seed);
    }

    public long seed() {
        return seed;
    }

    public double nextDouble() {
        return random.nextDouble();
    }

    /** Uniform in [lo, hi]. */
    public double range(double lo, double hi) {
        return hi <= lo ? lo : lo + random.nextDouble() * (hi - lo);
    }

    public int nextInt(int bound) {
        return bound <= 1 ? 0 : random.nextInt(bound);
    }

    public boolean chance(double p) {
        return p > 0 && random.nextDouble() < p;
    }

    /** Weighted pick; returns null if the list is empty or all weights are &le; 0. */
    public <T> T weighted(List<T> items, ToDoubleFunction<T> weight) {
        double total = 0;
        for (T t : items) {
            double w = weight.applyAsDouble(t);
            if (w > 0 && !Double.isNaN(w)) {
                total += w;
            }
        }
        if (total <= 0) {
            return null;
        }
        double r = random.nextDouble() * total;
        T last = null;
        for (T t : items) {
            double w = weight.applyAsDouble(t);
            if (w > 0 && !Double.isNaN(w)) {
                last = t;
                r -= w;
                if (r < 0) {
                    return t;
                }
            }
        }
        return last;
    }

    public <T> T pick(List<T> items) {
        return items.isEmpty() ? null : items.get(nextInt(items.size()));
    }

    /** SplitMix64 finaliser, used to derive seeds. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Seed of a cycle: mix(worldSeed, cycleId, rerollCounter). */
    public static long cycleSeed(long worldSeed, long cycleId, long rerollCounter) {
        long h = mix(worldSeed ^ 0x9E3779B97F4A7C15L);
        h = mix(h ^ (cycleId * 0xC2B2AE3D27D4EB4FL));
        h = mix(h ^ (rerollCounter * 0x165667B19E3779F9L + 0x27D4EB2F165667C5L));
        return h;
    }
}
