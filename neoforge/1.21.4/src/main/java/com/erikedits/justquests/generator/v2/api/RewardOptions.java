package com.erikedits.justquests.generator.v2.api;

/**
 * Optional reward extras on top of the value-based rewards. The mod uses {@link #STANDARD}, which
 * leaves the generated quests exactly as before; the server plugin turns them on.
 *
 * @param choice offer the item reward as a choice of up to three options of about the same value
 *               (that item, another item, or a loot table / effect); needs the host to support
 *               {@code justquests:choice}
 * @param scale  multiplies every reward budget (0.1 .. 10; 1 = unchanged)
 */
public record RewardOptions(boolean choice, double scale) {
    /** No choice, unscaled: the generator's normal rewards. */
    public static final RewardOptions STANDARD = new RewardOptions(false, 1.0);

    /** Clamps the scale into 0.1 .. 10 (NaN becomes 1). */
    public RewardOptions {
        scale = Double.isNaN(scale) ? 1.0 : Math.max(0.1, Math.min(10.0, scale));
    }
}
