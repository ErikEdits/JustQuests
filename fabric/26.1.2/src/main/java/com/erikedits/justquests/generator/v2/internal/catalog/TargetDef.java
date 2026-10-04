package com.erikedits.justquests.generator.v2.internal.catalog;

import java.util.List;

/**
 * One way to quest a resource: an objective type plus the exact target id(s).
 *
 * @param type       objective type
 * @param id         primary target id (item/block/entity/dimension)
 * @param alts       alternative ids tried in order when {@code id} does not exist (renames)
 * @param tagConcept optional tag concept key (tags.json); preferred when the host supports tags
 * @param effort     minutes per unit for an average player at that tier (per visit for dimensions)
 * @param min        minimum count
 * @param max        maximum count
 * @param weight     selection weight multiplier
 * @param tier       tier override or -1
 * @param tool       tool override or null
 * @param hints      extra hint keys (biome/rarity) for this target
 * @param name       display name override or null
 * @param plural     plural display name or null
 * @param hint       free-text hint for descriptions or null
 * @param stack      fallback stack size if the host cannot tell (0 = unknown)
 * @param dimension  dimension override or null
 * @param minDifficulty lowest difficulty index (0 EASY … 2 HARD) this target may appear on
 * @param tamable    catalog flag for {@code tame_animal}
 * @param note       free-text note (explain/MODS)
 * @param since      first Minecraft release with this target, or null (entry value applies)
 * @param potion     optional potion id for item objectives: the quest asks for
 *                   {@code {"id": <id>, "potion": <potion>}} (for example a Potion of Swiftness)
 */
public record TargetDef(ObjectiveType type, String id, List<String> alts, String tagConcept, double effort,
                        int min, int max, double weight, int tier, String tool, List<String> hints, String name,
                        String plural, String hint, int stack, String dimension, int minDifficulty,
                        boolean tamable, String note, String since, String potion) {
}
