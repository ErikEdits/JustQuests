package com.erikedits.justquests.generator.v2.api;

import java.util.Set;

/** What the running build of the mod can count and grant. */
public interface HostCapabilities {
    /**
     * True if the objective type accepts {@code "#tag"} targets on this build
     * (today: collect_item, craft_item, smelt_item, consume_item).
     *
     * @param objectiveType full type id, e.g. {@code "justquests:collect_item"}
     * @return whether tags are allowed in that objective's target field
     */
    boolean supportsTag(String objectiveType);

    /**
     * Objective types this build of the mod can count.
     *
     * @return objective type ids the running mod knows (e.g. {@code "justquests:smelt_item"})
     */
    Set<String> objectiveTypes();

    /**
     * Reward types this build of the mod can grant.
     *
     * @return reward type ids the running mod knows (e.g. {@code "justquests:give_item"})
     */
    Set<String> rewardTypes();
}
