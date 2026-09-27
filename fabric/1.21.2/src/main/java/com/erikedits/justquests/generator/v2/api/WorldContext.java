package com.erikedits.justquests.generator.v2.api;

/**
 * Facts about the world the set is generated for. Called at start and at every rotation/reroll
 * (never from the cheap {@code tick()} path unless a rotation is due).
 */
public interface WorldContext {
    /**
     * The world seed (mixed into every cycle seed).
     *
     * @return the world seed
     */
    long worldSeed();

    /**
     * Overworld game time / 24000 (in-game days since world creation).
     *
     * @return in-game day number, &ge; 0
     */
    long gameDay();

    /**
     * Players online right now.
     *
     * @return number of players online right now
     */
    int onlinePlayerCount();

    /**
     * Players that have JustQuests data in this world (&ge; online).
     *
     * @return known player count
     */
    int knownPlayerCount();

    /**
     * Share 0..1 of ONLINE players that have the advancement; {@code -1} if no player is online
     * or the advancement is unknown.
     *
     * @param advancementId e.g. {@code "minecraft:story/enter_the_nether"}
     * @return share in [0,1] or -1
     */
    double onlineShareWithAdvancement(String advancementId);

    /**
     * Singleplayer flag (claims still apply in singleplayer; informational only).
     *
     * @return true in singleplayer / LAN host worlds
     */
    boolean isSingleplayer();
}
