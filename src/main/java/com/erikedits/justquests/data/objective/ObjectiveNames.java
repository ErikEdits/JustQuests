package com.erikedits.justquests.data.objective;

import net.minecraft.core.HolderSet;

/** Builds a readable label for a HolderSet (tag, single entry, or many). */
public final class ObjectiveNames {
    private ObjectiveNames() {}

    public static <T> String describe(HolderSet<T> set) {
        return set.unwrapKey()
            .map(tag -> "#" + tag.location())
            .orElseGet(() -> {
                if (set.size() == 1) {
                    return set.get(0).unwrapKey().map(k -> k.location().toString()).orElse("?");
                }
                return set.size() + " types";
            });
    }
}
