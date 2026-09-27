package com.erikedits.justquests.generator.v2.internal.catalog;

import java.util.List;

/**
 * A resource of the content model ({@code iron}, {@code oak_wood}, {@code zombie}).
 *
 * @param profile    owning profile id ({@code vanilla}, {@code farmersdelight} …)
 * @param key        stable internal name, unique within the profile
 * @param name       display name of the resource (used in theme texts) or null
 * @param family     variety group; at most one quest per family per set
 * @param tier       0 surface · 1 stone/iron · 2 deep/diamond · 3 Nether · 4 End
 * @param dimension  where it is obtained
 * @param tool       minimum tool ({@code none}, {@code wood}, {@code stone}, {@code iron}, {@code diamond},
 *                   {@code netherite}, {@code shears}, {@code silk_touch}, {@code fishing_rod}, {@code knife} …)
 * @param hints      rarity/biome hints ({@code desert}, {@code rare}, {@code fortress} …)
 * @param requires   prerequisites: {@code nether}, {@code end}, {@code dim:<id>}, {@code key:<entry>}
 * @param since      informational MC version
 * @param notes      free text
 * @param exclusions documented excluded combinations ("type id: why"), shown by explain
 * @param targets    quest targets
 * @param weight     selection weight multiplier
 */
public record EntryDef(String profile, String key, String name, String family, int tier, String dimension,
                       String tool, List<String> hints, List<String> requires, String since, String notes,
                       List<String> exclusions, List<TargetDef> targets, double weight) {
}
