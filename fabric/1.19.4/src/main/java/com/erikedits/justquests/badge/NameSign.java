package com.erikedits.justquests.badge;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

import java.util.EnumSet;
import java.util.List;

/**
 * The sign of a player's shown badge before their name, in the chat and in the tab list (0.6.0). The
 * server puts it into the name itself, so every player sees it, with or without JustQuests. Fabric has no
 * name event: PlayerDisplayNameMixin and ServerPlayerTabNameMixin call {@link #decorate} and
 * {@link #tabName}. The world setting badgeInName turns it off.
 */
public final class NameSign {
    private NameSign() {}

    /** The badge sign before a name (the name itself when the player shows none). */
    public static Component decorate(ServerPlayer player, Component name) {
        MutableComponent sign = Badges.nameSign(player.getUUID());
        return sign == null ? name : sign.append(name);
    }

    /** The tab list name: the game's own (null: the team-coloured name) with the sign before it. */
    public static Component tabName(ServerPlayer player, Component name) {
        MutableComponent sign = Badges.nameSign(player.getUUID());
        if (sign == null) return name;
        return sign.append(name != null ? name : PlayerTeam.formatNameForTeam(player.getTeam(), player.getName()));
    }

    /** The sign changed: everyone's tab list shows the new one (the chat takes it from the next message). */
    public static void refresh(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (server == null) return;
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
            EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), List.of(player)));
    }
}
