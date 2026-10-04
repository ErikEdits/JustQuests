package com.erikedits.justquests.generator.v2.api;

import java.util.Set;

/**
 * A set of quests outside the rotating board, for hosts with boards of their own (the server
 * plugin's personal, weekly and server-goal quests). Nothing is stored and nothing is claimed; the
 * host keeps the quests and the no-repeat history itself.
 *
 * @param seed            generation seed (the same request gives the same quests)
 * @param count           number of quests (1 .. 50)
 * @param difficulty      difficulty of the set
 * @param minutesScale    multiplies the difficulty's target play time (1 = normal, 4 = four times longer)
 * @param budgetScale     multiplies its reward budget (on top of the generator's {@link RewardOptions})
 * @param singleObjective one objective per quest
 * @param dayUnlocks      whether the Nether and the End unlock by game day too; false: only by
 *                        {@code world}'s advancement shares (for one player's own progress)
 * @param world           progression source (game day, players, advancement shares); null = the host's world
 * @param history         quest signatures to avoid (from earlier sets of this kind)
 * @param excludedTypes   objective types left out (short names, e.g. {@code reach_location})
 */
public record SetRequest(long seed, int count, Difficulty difficulty, double minutesScale, double budgetScale,
                         boolean singleObjective, boolean dayUnlocks, WorldContext world, Set<String> history,
                         Set<String> excludedTypes) {
    /** Normalises nulls and clamps the numbers. */
    public SetRequest {
        count = Math.max(1, Math.min(GeneratorConfig.ABSOLUTE_MAX_PER_CYCLE, count));
        difficulty = difficulty == null ? Difficulty.NORMAL : difficulty;
        minutesScale = Double.isNaN(minutesScale) ? 1.0 : Math.max(0.1, Math.min(1000.0, minutesScale));
        budgetScale = Double.isNaN(budgetScale) ? 1.0 : Math.max(0.01, Math.min(100.0, budgetScale));
        history = history == null ? Set.of() : Set.copyOf(history);
        excludedTypes = excludedTypes == null ? Set.of() : Set.copyOf(excludedTypes);
    }
}
