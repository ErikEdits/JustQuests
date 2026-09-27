package com.erikedits.justquests.generator.v2.api;

import java.util.Set;

/**
 * Read-only view of the running game's content. Ids are full namespaced strings
 * ({@code minecraft:oak_log}); tag ids are passed <b>without</b> the leading {@code #}.
 *
 * <p>Contract for implementers:
 * <ul>
 *   <li>Never throw; answer {@code false}, an empty set, {@link TriState#UNKNOWN}, {@code -1} or
 *       {@code null} when unsure or when the id is malformed.</li>
 *   <li>Existence checks are called often (hundreds of times per rotation) and should be cheap
 *       ({@code Registry.containsKey}).</li>
 *   <li>Tag lookups are called at most once per tag per rotation; the core caches results for
 *       the rotation. They are only called at or after {@code start()}, i.e. after tags bind.</li>
 * </ul>
 *
 * <p>The core produces good results even if every {@link TriState} answer is
 * {@link TriState#UNKNOWN} and {@link #englishName} always returns {@code null}.
 */
public interface ContentView {
    /**
     * Loader name, used for information and profile filters.
     *
     * @return {@code "neoforge"}, {@code "fabric"} or {@code "forge"}
     */
    String loaderName();

    /**
     * Minecraft version, used for information and profile filters.
     *
     * @return e.g. {@code "1.21.1"}
     */
    String minecraftVersion();

    /**
     * Loader mod-list check.
     *
     * @param modId mod id, e.g. {@code "farmersdelight"}
     * @return true if the mod is loaded
     */
    boolean isModLoaded(String modId);

    /**
     * Item registry existence check (cheap, called often).
     *
     * @param id item id
     * @return true if the item registry contains the key
     */
    boolean itemExists(String id);

    /**
     * Block registry existence check (cheap, called often).
     *
     * @param id block id
     * @return true if the block registry contains the key
     */
    boolean blockExists(String id);

    /**
     * Entity type registry existence check (cheap, called often).
     *
     * @param id entity type id
     * @return true if the entity type registry contains the key
     */
    boolean entityTypeExists(String id);

    /**
     * Dimension keys known to the RUNNING server (e.g. {@code "minecraft:the_nether"}).
     *
     * @param id dimension key
     * @return true if a level with that key exists
     */
    boolean dimensionExists(String id);

    /**
     * Resolved members of an item tag; empty (never null) if unknown or empty.
     * Tags bind after datapack load — the core only calls this at/after {@code start()}.
     *
     * @param tagId tag id without {@code '#'}
     * @return member item ids
     */
    Set<String> itemTagMembers(String tagId);

    /**
     * Resolved members of a block tag; empty (never null) if unknown or empty.
     *
     * @param tagId tag id without {@code '#'}
     * @return member block ids
     */
    Set<String> blockTagMembers(String tagId);

    /**
     * Resolved members of an entity type tag; empty (never null) if unknown or empty.
     *
     * @param tagId tag id without {@code '#'}
     * @return member entity type ids
     */
    Set<String> entityTypeTagMembers(String tagId);

    /**
     * Whether a crafting-grid recipe (crafting table or 2x2) outputs this item.
     *
     * @param itemId output item id
     * @return YES/NO if known, UNKNOWN if the build cannot answer cheaply
     */
    TriState hasCraftingRecipe(String itemId);

    /**
     * Whether a furnace / blast furnace / smoker recipe outputs this item.
     *
     * @param outputItemId output item id
     * @return YES/NO if known, UNKNOWN otherwise
     */
    TriState hasSmeltingRecipe(String outputItemId);

    /**
     * Default max stack size.
     *
     * @param itemId item id
     * @return 64, 16, 1 …; {@code -1} if unknown
     */
    int maxStackSize(String itemId);

    /**
     * Entity type is an {@code Animal} subclass (breedable by players).
     *
     * @param entityTypeId entity type id
     * @return YES/NO if known, UNKNOWN otherwise
     */
    TriState isBreedableAnimal(String entityTypeId);

    /**
     * Entity type is a {@code TamableAnimal} subclass (wolf/cat/parrot style).
     *
     * @param entityTypeId entity type id
     * @return YES/NO if known, UNKNOWN otherwise
     */
    TriState isTamableAnimal(String entityTypeId);

    /**
     * Item finishes a "use" action (food, drink, potion, milk).
     *
     * @param itemId item id
     * @return YES/NO if known, UNKNOWN otherwise
     */
    TriState isConsumable(String itemId);

    /**
     * English display name if resolvable on this side; {@code null} otherwise.
     * NOTE: dedicated servers only know vanilla names — modded names are usually {@code null}.
     * The core falls back to catalog names, then prettifies ids.
     *
     * @param kind what the id refers to
     * @param id   namespaced id
     * @return the English name or null
     */
    String englishName(ContentKind kind, String id);
}
