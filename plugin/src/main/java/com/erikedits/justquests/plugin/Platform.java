package com.erikedits.justquests.plugin;

import org.bukkit.Bukkit;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Which kind of server a jar is made for and which one it runs on (0.6.1). There is one plugin jar per
 * server type and Minecraft range (JustQuests-plugin-&lt;type&gt;-&lt;range&gt;); each knows its type from
 * justquests-platform.txt. A jar on the wrong type says so in the log, and a jar that needs Spigot's chat
 * components on plain Bukkit stops with a clear message instead of an error nobody understands.
 */
public final class Platform {
    /** From the most special to the most general: the first that matches names the server. */
    public static final List<String> TYPES = List.of("leaves", "leaf", "purpur", "paper", "spigot", "bukkit");

    private Platform() {}

    /** The type this jar is made for ("paper"), or "" for a jar built without one. */
    public static String jar() {
        try (InputStream in = Platform.class.getResourceAsStream("/justquests-platform.txt")) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    /** The type of the running server. */
    public static String server() {
        if (has("org.leavesmc.leaves.LeavesConfig")) return "leaves";
        String name = Bukkit.getName().toLowerCase(Locale.ROOT);
        if (name.contains("leaves")) return "leaves";
        if (name.contains("leaf")) return "leaf";
        if (name.contains("purpur") || has("org.purpurmc.purpur.PurpurConfig")) return "purpur";
        if (name.contains("paper") || has("io.papermc.paper.configuration.Configuration") || has("com.destroystokyo.paper.PaperConfig")) return "paper";
        if (has("org.spigotmc.SpigotConfig")) return "spigot";
        return "bukkit";
    }

    /** Whether this server has the BungeeCord chat components (all but plain Bukkit; the Bukkit jar brings its own). */
    public static boolean chat() {
        return has("net.md_5.bungee.api.chat.BaseComponent");
    }

    /**
     * Checks the jar against the server at the start of onEnable; false when it cannot run here (the
     * plugin then turns itself off after saying why).
     */
    public static boolean check(Logger log, String range) {
        String made = jar(), runs = server();
        log.info("Server type: " + runs + " (" + Bukkit.getName() + " " + Bukkit.getBukkitVersion() + "), this jar: "
            + (made.isEmpty() ? "any" : made));
        if (!chat()) {
            log.severe("This server is plain Bukkit (CraftBukkit) without the chat components of Spigot. This JustQuests jar ("
                + made + ") needs them: use JustQuests-plugin-bukkit-" + range + ".jar, made for Bukkit.");
            return false;
        }
        if (!made.isEmpty() && !made.equals(runs)) {
            log.warning("This JustQuests jar is made for " + made + " servers, but this one is " + runs
                + ". It may work, but please use JustQuests-plugin-" + runs + "-" + range + ".jar.");
        }
        return true;
    }

    private static boolean has(String cls) {
        try {
            Class.forName(cls, false, Platform.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
