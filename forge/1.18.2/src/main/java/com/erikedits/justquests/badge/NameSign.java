package com.erikedits.justquests.badge;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

/**
 * The sign of a player's shown badge before their name, in the chat and in the tab list (0.6.0). The
 * server puts it into the name itself (PlayerEvent.NameFormat and TabListNameFormat in
 * PlayerQuestEvents), so every player sees it, with or without JustQuests. The world setting badgeInName
 * turns it off.
 */
public final class NameSign {
    private NameSign() {}

    /** The tab list name: the game's own (null: the team-coloured name) with the sign before it. */
    public static Component tabName(ServerPlayer player, Component name) {
        MutableComponent sign = Badges.nameSign(player.getUUID());
        if (sign == null) return name;
        return sign.append(name != null ? name : PlayerTeam.formatNameForTeam(player.getTeam(), player.getName()));
    }

    /** The sign changed: the name is worked out again, and everyone's tab list shows the new one. */
    public static void refresh(ServerPlayer player) {
        player.refreshDisplayName();
        player.refreshTabListName();
    }
}
