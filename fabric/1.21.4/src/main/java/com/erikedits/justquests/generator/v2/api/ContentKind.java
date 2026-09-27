package com.erikedits.justquests.generator.v2.api;

/** Kind of registry entry, used for name lookups ({@link ContentView#englishName}). */
public enum ContentKind {
    /** An item id ({@code minecraft:raw_iron}). */
    ITEM,
    /** A block id ({@code minecraft:iron_ore}). */
    BLOCK,
    /** An entity type id ({@code minecraft:zombie}). */
    ENTITY,
    /** A dimension key ({@code minecraft:the_nether}). */
    DIMENSION
}
