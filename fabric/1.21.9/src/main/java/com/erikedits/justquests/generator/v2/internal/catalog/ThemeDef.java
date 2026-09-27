package com.erikedits.justquests.generator.v2.internal.catalog;

import java.util.List;
import java.util.Set;

/**
 * A coherent multi-objective archetype (themes.json and profile extensions).
 *
 * @param profile       owning profile (the theme is only used when it is active)
 * @param key           unique key
 * @param names         title pool
 * @param descriptions  description templates ({@code {list}} = joined objective phrases)
 * @param slots         objective slots in order
 * @param requiresProfiles every listed profile must be active
 * @param minDifficulty lowest difficulty index allowed
 * @param maxDifficulty highest difficulty index allowed
 * @param weight        selection weight
 */
public record ThemeDef(String profile, String key, List<String> names, List<String> descriptions, List<Slot> slots,
                       Set<String> requiresProfiles, int minDifficulty, int maxDifficulty, double weight) {

    /**
     * One objective slot. Empty filter sets mean "any".
     *
     * @param types    allowed objective types
     * @param keys     allowed entry keys
     * @param families allowed families
     * @param profiles allowed candidate profiles
     * @param optional slot may be left out (theme then yields fewer objectives)
     */
    public record Slot(Set<ObjectiveType> types, Set<String> keys, Set<String> families, Set<String> profiles,
                       boolean optional) {
    }

    public int requiredSlots() {
        int n = 0;
        for (Slot s : slots) {
            if (!s.optional()) {
                n++;
            }
        }
        return n;
    }
}
