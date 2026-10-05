package com.erikedits.justquests.perm;

import com.erikedits.justquests.data.Quest;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.Predicate;

/**
 * Permission nodes for LuckPerms and other permission mods. Player commands have
 * justquests.command.<name> (everyone by default), operator commands justquests.admin.<name>
 * (operators, level 2, by default), and a quest can name its own node in "permission" (operators by
 * default). Without a permission mod the defaults apply.
 * On Fabric the checks go through fabric-permissions-api (bundled).
 */
public final class Perms {
    /** Subcommands everyone may use unless a permission mod says otherwise. */
    public static final List<String> PLAYER = List.of("list", "categories", "stats", "leaderboard", "progress",
        "accept", "abandon", "claim", "team", "discord");
    /** Subcommands for operators (level 2) unless a permission mod says otherwise. */
    public static final List<String> ADMIN = List.of("reload", "reroll", "mainquests", "difficulty", "generator",
        "test", "admin");

    private Perms() {}

    /** requires() of a player subcommand: justquests.command.<name>. */
    public static Predicate<CommandSourceStack> command(String name) {
        return src -> check(src, "justquests.command." + name, false);
    }

    /** requires() of an operator subcommand: justquests.admin.<name>. */
    public static Predicate<CommandSourceStack> admin(String name) {
        return src -> check(src, "justquests.admin." + name, true);
    }

    /** Whether the player may see and take the quest; quests without a permission are open to everyone. */
    public static boolean quest(ServerPlayer player, Quest quest) {
        return quest.permission().isEmpty() || check(player, quest.permission().get(), true);
    }

    /** The console and command blocks keep their own level; players ask the permission mod. */
    public static boolean check(CommandSourceStack src, String node, boolean opOnly) {
        if (src.getEntity() instanceof ServerPlayer player) return check(player, node, opOnly);
        return !opOnly || op(src);
    }

    private static boolean op(CommandSourceStack src) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(src);
    }

    private static boolean op(ServerPlayer player) {
        return op(player.createCommandSourceStack());
    }

    /** fabric-permissions-api (bundled) asks LuckPerms or another permission mod; without one the default applies. */
    public static boolean check(ServerPlayer player, String node, boolean opOnly) {
        return Permissions.check(player, node, !opOnly || op(player));
    }
}
