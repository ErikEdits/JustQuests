package com.erikedits.justquests.plugin.quest;

import org.bukkit.World;

import java.util.Map;
import java.util.Set;

/**
 * The vanilla biome tags the server API cannot read (0.6.0, find_biome): their members as of 1.21.
 * Another tag matches the biomes whose name holds the tag's name ("#mymod:is_volcanic" -> "volcanic").
 */
final class BiomeTags {
    private static final Map<String, Set<String>> TAGS = Map.ofEntries(
        Map.entry("minecraft:is_ocean", Set.of("deep_frozen_ocean", "deep_cold_ocean", "deep_ocean", "deep_lukewarm_ocean",
            "frozen_ocean", "ocean", "cold_ocean", "lukewarm_ocean", "warm_ocean")),
        Map.entry("minecraft:is_deep_ocean", Set.of("deep_frozen_ocean", "deep_cold_ocean", "deep_ocean", "deep_lukewarm_ocean")),
        Map.entry("minecraft:is_beach", Set.of("beach", "snowy_beach")),
        Map.entry("minecraft:is_river", Set.of("river", "frozen_river")),
        Map.entry("minecraft:is_mountain", Set.of("meadow", "frozen_peaks", "jagged_peaks", "stony_peaks", "snowy_slopes", "cherry_grove")),
        Map.entry("minecraft:is_hill", Set.of("windswept_hills", "windswept_forest", "windswept_gravelly_hills")),
        Map.entry("minecraft:is_taiga", Set.of("taiga", "snowy_taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga")),
        Map.entry("minecraft:is_jungle", Set.of("bamboo_jungle", "jungle", "sparse_jungle")),
        Map.entry("minecraft:is_forest", Set.of("forest", "flower_forest", "birch_forest", "old_growth_birch_forest", "dark_forest",
            "grove", "pale_garden")),
        Map.entry("minecraft:is_badlands", Set.of("badlands", "eroded_badlands", "wooded_badlands")),
        Map.entry("minecraft:is_savanna", Set.of("savanna", "savanna_plateau", "windswept_savanna")),
        Map.entry("minecraft:is_nether", Set.of("nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas")),
        Map.entry("minecraft:is_end", Set.of("the_end", "end_highlands", "end_midlands", "small_end_islands", "end_barrens")));

    private BiomeTags() {}

    /** Whether the biome (an id like minecraft:plains) is in the tag (without its #). */
    static boolean has(String tag, String biome, World.Environment environment) {
        if (tag.equals("minecraft:is_overworld")) return environment == World.Environment.NORMAL;
        String path = biome.substring(biome.indexOf(':') + 1);
        Set<String> members = TAGS.get(tag);
        if (members != null) return biome.startsWith("minecraft:") && members.contains(path);
        String name = tag.substring(tag.indexOf(':') + 1);
        if (name.startsWith("is_")) name = name.substring(3);
        return path.contains(name);
    }
}
