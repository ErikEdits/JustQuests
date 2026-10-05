package com.erikedits.justquests.plugin.progress;

import com.erikedits.justquests.plugin.quest.Quest;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * What the quest generator has to say about its quests (who took one, who may see it, bonuses) and
 * the events it listens to. Quests that are not generated pass through untouched; {@link #NONE} is
 * used while the generator is off.
 */
public interface GeneratedQuests {
    GeneratedQuests NONE = new GeneratedQuests() {};

    /** Called before a player accepts a quest: null to allow it, else the reason it is refused. */
    default BaseComponent claim(String id, UUID player, String lang) {
        return null;
    }

    default void abandoned(String id, UUID player) {}

    /** {@code rewardsWait}: the rewards wait to be claimed, so the quest must stay until then. */
    default void completed(String id, UUID player, boolean rewardsWait) {}

    /** The player claimed a finished quest's rewards (or an admin reset dropped them). */
    default void rewardsClaimed(String id, UUID player) {}

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

    /** Whether the viewer may see the quest at all (personal quests are their owner's only). */
    default boolean visible(String id, UUID viewer) {
        return true;
    }

    /** After a quest's rewards were paid: extras such as the lucky bonus. */
    default void bonus(Player player, Quest quest) {}

    /** Every game event that can move quests forward (for the server goal). */
    default void progress(Player player, ProgressService.Test test) {}

    default void releaseAll(UUID player) {}
}
