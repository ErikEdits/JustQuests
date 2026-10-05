package com.erikedits.justquests.perm;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Permission nodes for LuckPerms and other permission mods. Player commands have
 * justquests.command.<name> (everyone by default), operator commands justquests.admin.<name>
 * (operators, level 2, by default), and a quest can name its own node in "permission" (operators by
 * default). Without a permission mod the defaults apply.
 * The nodes are registered with the loader's PermissionAPI, which LuckPerms answers.
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
        return src.hasPermission(2);
    }

    private static boolean op(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    private static final Map<String, PermissionNode<Boolean>> NODES = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();

    /**
     * PermissionGatherEvent.Nodes (server start): the command nodes and the permissions of the quests
     * known now (datapacks and custom-quests.json). A quest permission added while the server runs
     * uses the default (operators) until the next start, since nodes cannot be added later.
     */
    public static void onGatherNodes(PermissionGatherEvent.Nodes event) {
        NODES.clear();
        PLAYER.forEach(n -> node("justquests.command." + n, false));
        ADMIN.forEach(n -> node("justquests.admin." + n, true));
        QuestManager.INSTANCE.getQuests().values().forEach(q -> q.permission().ifPresent(p -> node(p, true)));
        customQuestPermissions().forEach(p -> node(p, true));
        List<PermissionNode<?>> nodes = new ArrayList<>(NODES.values());
        event.addNodes(nodes);
    }

    private static void node(String name, boolean opOnly) {
        int dot = name.indexOf('.');
        if (dot <= 0 || dot == name.length() - 1 || NODES.containsKey(name)) return;
        NODES.put(name, new PermissionNode<>(name.substring(0, dot), name.substring(dot + 1), PermissionTypes.BOOLEAN,
            (player, uuid, context) -> !opOnly || (player != null && op(player))));
    }

    /** "permission" values in <world>/justquests/custom-quests.json, read before the custom quests load. */
    private static Set<String> customQuestPermissions() {
        Set<String> out = new TreeSet<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return out;
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("justquests").resolve("custom-quests.json");
        try {
            if (Files.exists(file)) {
                Matcher m = Pattern.compile("\"permission\"\\s*:\\s*\"([^\"]+)\"").matcher(Files.readString(file));
                while (m.find()) out.add(m.group(1));
            }
        } catch (Exception e) {
            JustQuests.LOG.warn("Could not read quest permissions from {}", file, e);
        }
        return out;
    }

    /** The active permission handler (LuckPerms, or the loader's default) decides; unknown nodes use the default. */
    public static boolean check(ServerPlayer player, String node, boolean opOnly) {
        PermissionNode<Boolean> n = NODES.get(node);
        if (n != null) {
            try {
                return PermissionAPI.getPermission(player, n);
            } catch (RuntimeException e) {
                // the permission handler started before this node existed
            }
        }
        if (WARNED.add(node)) {
            JustQuests.LOG.info("Permission node {} is new since the server started: until a restart only the default applies", node);
        }
        return !opOnly || op(player);
    }
}
