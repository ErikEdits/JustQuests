package com.erikedits.justquests.plugin.progress;

import net.md_5.bungee.api.chat.BaseComponent;

import java.util.UUID;

/**
 * What the quest generator has to say about its quests (who took one, whether it can still be
 * taken). Quests that are not generated pass through untouched; {@link #NONE} is used while the
 * generator is off.
 */
public interface GeneratedQuests {
    GeneratedQuests NONE = new GeneratedQuests() {};

    /** Called before a player accepts a quest: null to allow it, else the reason it is refused. */
    default BaseComponent claim(String id, UUID player, String lang) {
        return null;
    }

    default void abandoned(String id, UUID player) {}

    default void completed(String id, UUID player) {}

    /** A short tag for the quest list ("[taken by X]"), or null. */
    default BaseComponent tag(String id, UUID viewer, String lang) {
        return null;
    }

    /** One line about the quest for the quest book ("Reserved for you"), or null. */
    default String bookLine(String id, UUID viewer, String lang) {
        return null;
    }

    /** Whether someone else holds the quest, so the viewer cannot take it. */
    default boolean takenByOther(String id, UUID viewer) {
        return false;
    }

    default void releaseAll(UUID player) {}
}
