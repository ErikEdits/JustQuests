package com.erikedits.justquests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared constants (mod id + logger) used across the quest domain code.
 * The loader entry points are {@link JustQuestsFabric} (main) and
 * {@link com.erikedits.justquests.client.JustQuestsFabricClient} (client).
 */
public final class JustQuests {
    public static final String MOD_ID = "justquests";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private JustQuests() {}
}
