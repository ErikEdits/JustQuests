package com.erikedits.justquests.generator.v2.internal.catalog;

import com.erikedits.justquests.generator.v2.api.ContentKind;

import java.util.Locale;
import java.util.Set;

/**
 * Objective types the generator may emit (§6.2/§7). {@code gain_advancement}, {@code reach_level}
 * and {@code reach_location} are deliberately absent: they are unfair in a shared set.
 */
public enum ObjectiveType {
    COLLECT_ITEM("collect_item", "item", ContentKind.ITEM, true),
    MINE_BLOCK("mine_block", "block", ContentKind.BLOCK, true),
    CRAFT_ITEM("craft_item", "item", ContentKind.ITEM, true),
    SMELT_ITEM("smelt_item", "item", ContentKind.ITEM, true),
    KILL_MOB("kill_mob", "entity", ContentKind.ENTITY, true),
    BREED_ANIMAL("breed_animal", "entity", ContentKind.ENTITY, true),
    TAME_ANIMAL("tame_animal", "entity", ContentKind.ENTITY, true),
    CONSUME_ITEM("consume_item", "item", ContentKind.ITEM, true),
    PLACE_BLOCK("place_block", "block", ContentKind.BLOCK, true),
    VISIT_DIMENSION("visit_dimension", "dimension", ContentKind.DIMENSION, false),
    /** Enchanting-table enchants; the target is the enchanted RESULT (books become enchanted_book). */
    ENCHANT_ITEM("enchant_item", "item", ContentKind.ITEM, true),
    /** Uses that did something (vanilla "Times Used"): throwables, bone meal, rockets. */
    USE_ITEM("use_item", "item", ContentKind.ITEM, true);

    /** Types that must never be emitted. */
    public static final Set<String> FORBIDDEN = Set.of("gain_advancement", "reach_level", "reach_location");

    private final String shortName;
    private final String field;
    private final ContentKind kind;
    private final boolean counted;

    ObjectiveType(String shortName, String field, ContentKind kind, boolean counted) {
        this.shortName = shortName;
        this.field = field;
        this.kind = kind;
        this.counted = counted;
    }

    /** e.g. {@code collect_item}. */
    public String shortName() {
        return shortName;
    }

    /** e.g. {@code justquests:collect_item}. */
    public String typeId() {
        return "justquests:" + shortName;
    }

    /** JSON field holding the target ({@code item}, {@code block}, {@code entity}, {@code dimension}). */
    public String field() {
        return field;
    }

    public ContentKind kind() {
        return kind;
    }

    /** True if the objective has a {@code count} field. */
    public boolean counted() {
        return counted;
    }

    /** Item-valued objectives (stack sizes, item filters such as {@code potion}). */
    public boolean itemBased() {
        return kind == ContentKind.ITEM;
    }

    /**
     * Item, block and entity objectives may take a tag of their own kind if the host allows it
     * ({@code HostCapabilities.supportsTag}); dimensions never.
     */
    public boolean taggable() {
        return kind != ContentKind.DIMENSION;
    }

    /** Accepts {@code collect_item}, {@code justquests:collect_item} or enum names; null if unknown. */
    public static ObjectiveType parse(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("justquests:")) {
            t = t.substring("justquests:".length());
        }
        for (ObjectiveType o : values()) {
            if (o.shortName.equals(t)) {
                return o;
            }
        }
        return null;
    }
}
