package com.erikedits.justquests.generator.v2.internal.gen;

import com.erikedits.justquests.generator.v2.internal.catalog.EntryDef;
import com.erikedits.justquests.generator.v2.internal.catalog.ObjectiveType;
import com.erikedits.justquests.generator.v2.internal.catalog.TargetDef;

import java.util.List;

/**
 * A (type, target) pair that passed achievability steps 1–5 for this generation run, with all
 * derived numbers the composer needs.
 *
 * @param profile        owning profile id
 * @param entry          catalog entry
 * @param def            catalog target
 * @param type           objective type
 * @param target         resolved target (id or {@code #tag})
 * @param isTag          true for tag targets
 * @param tier           effective tier
 * @param family         family
 * @param dimension      dimension where it is obtained
 * @param tool           minimum tool
 * @param hints          merged hint keys (entry + target)
 * @param baseEffort     catalog minutes per unit
 * @param multiplier     combined modifier (hints × tool × calibration)
 * @param modifierNote   explain text for the modifier
 * @param overhead       travel overhead minutes (added once per quest per dimension)
 * @param min            min count
 * @param max            max count
 * @param stack          stack size for count rounding (0 for non-items)
 * @param name           singular display name
 * @param plural         plural display name
 * @param weight         base selection weight
 * @param tagNoun        tag noun for "any X counts" hints (null unless tag)
 */
public record Candidate(String profile, EntryDef entry, TargetDef def, ObjectiveType type, String target,
                        boolean isTag, int tier, String family, String dimension, String tool, List<String> hints,
                        double baseEffort, double multiplier, String modifierNote, double overhead, int min, int max,
                        int stack, String name, String plural, double weight, String tagNoun) {

    /** Minutes per unit including modifiers. */
    public double effortPerUnit() {
        return baseEffort * multiplier;
    }

    /** Signature part: {@code type:target}, plus {@code {potion}} for a potion filter. */
    public String signaturePart() {
        return type.shortName() + ":" + target + (def.potion() != null ? "{" + def.potion() + "}" : "");
    }

    public boolean modded() {
        return !"vanilla".equals(profile);
    }

    /** Minutes at the minimum count (plus overhead). */
    public double minMinutes() {
        return type.counted() ? min * effortPerUnit() + overhead : effortPerUnit() + overhead;
    }

    /** Minutes at the maximum count (plus overhead). */
    public double maxMinutes() {
        return type.counted() ? max * effortPerUnit() + overhead : effortPerUnit() + overhead;
    }
}
