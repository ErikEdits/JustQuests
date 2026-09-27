package com.erikedits.justquests.generator.v2.internal.catalog;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A content profile: vanilla or one mod (catalog/profiles/*.json).
 *
 * @param id          profile id ({@code vanilla}, {@code farmersdelight} …)
 * @param name        display name
 * @param source      where it was loaded from (bundled path or world file), for logs
 * @param requiresMod mod ids, any-of (empty for vanilla)
 * @param loaders     allowed loader names (empty = all)
 * @param mcMin       lowest MC version (inclusive) or null
 * @param mcMax       highest MC version (inclusive) or null
 * @param entries     resources
 * @param items       reward item extensions
 * @param effects     effect reward extensions
 * @param loot        loot table extensions
 * @param themes      theme extensions
 * @param dimensions  modded dimension unlock rules keyed by dimension id
 */
public record ProfileDef(String id, String name, String source, List<String> requiresMod, Set<String> loaders,
                         String mcMin, String mcMax, List<EntryDef> entries, List<RewardDefs.Item> items,
                         List<RewardDefs.Effect> effects, List<RewardDefs.Loot> loot, List<ThemeDef> themes,
                         Map<String, DimensionUnlock> dimensions) {

    /**
     * Unlock rule for a (modded) dimension.
     *
     * @param dimension   dimension id
     * @param advancement advancement id that signals access, or null
     * @param share       online share with the advancement needed to unlock
     * @param day         game day that unlocks it regardless (0 = never by day)
     * @param tier        tier of the dimension's content
     */
    public record DimensionUnlock(String dimension, String advancement, double share, long day, int tier) {
    }

    public boolean isVanilla() {
        return "vanilla".equals(id);
    }
}
