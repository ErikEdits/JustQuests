package com.erikedits.justquests.generator.v2.api;

/**
 * Everything the core may ask the running game. Implemented once per build by the integrator
 * (a thin adapter). The core calls these methods only from the thread that calls the facade
 * (the server thread).
 *
 * <p>Every getter must return the same object for the lifetime of one server/world; the core may
 * cache the returned views.
 */
public interface GeneratorHost {
    /**
     * Registries, tags, mod list, recipes.
     *
     * @return the view, never null
     */
    ContentView content();

    /**
     * World seed, game day, online players, advancement shares.
     *
     * @return the view, never null
     */
    WorldContext world();

    /**
     * Reads/writes files in {@code <world>/justquests/}.
     *
     * @return the view, never null
     */
    StateStore store();

    /**
     * Runs the mod's real quest codec.
     *
     * @return the view, never null
     */
    QuestValidator validator();

    /**
     * Objective/reward types and tag support of this build.
     *
     * @return the view, never null
     */
    HostCapabilities capabilities();

    /**
     * Logging sink (the mod's logger).
     *
     * @return the view, never null
     */
    GenLog log();

    /**
     * Real wall-clock time in epoch milliseconds (injectable for tests).
     * Production adapters return {@link System#currentTimeMillis()}.
     *
     * @return current epoch milliseconds
     */
    long currentTimeMillis();
}
