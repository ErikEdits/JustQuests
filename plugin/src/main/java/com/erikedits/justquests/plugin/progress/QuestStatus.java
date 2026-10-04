package com.erikedits.justquests.plugin.progress;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.quest.Quest;

import java.util.UUID;

/** Where a quest stands for a player, in the order the quest book lists them. */
public enum QuestStatus {
    CLAIM("justquests.book.status.claim", "§6"),
    ACTIVE("justquests.book.status.active", "§e"),
    AVAILABLE("justquests.book.status.available", "§a"),
    LOCKED("justquests.book.status.locked", "§8"),
    COMPLETED("justquests.book.status.completed", "§7");

    public final String key;
    public final String color;

    QuestStatus(String key, String color) {
        this.key = key;
        this.color = color;
    }

    public static QuestStatus of(JustQuestsPlugin plugin, UUID player, PlayerData data, Quest quest) {
        String id = quest.id();
        if (data != null && data.isClaimable(id)) return CLAIM;
        if (data != null && data.isActive(id)) return ACTIVE;
        if (data != null && data.isCompleted(id) && !quest.repeatable()) return COMPLETED;
        if (plugin.progress().missingRequirement(data, quest) != null) return LOCKED;
        if (plugin.generated().takenByOther(id, player)) return LOCKED;
        if (data != null && data.isCompleted(id)) {
            return plugin.progress().cooldownLeft(data, quest) > 0 ? COMPLETED : AVAILABLE;
        }
        return AVAILABLE;
    }
}
