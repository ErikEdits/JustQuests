package com.erikedits.justquests.storage;

import com.erikedits.justquests.JustQuests;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Per-world settings loaded from {@code <world>/justquests/settings.json}.
 * The single home for server-owner toggles (Q35 groundwork). A template with
 * defaults is written on first run. Works in singleplayer and on servers
 * (singleplayer uses the integrated server's world path).
 */
public final class WorldSettings {
    private static boolean discordWelcome = true;
    private static boolean announceCompletions = true;
    private static boolean completionSound = true;
    private static boolean completionToast = true;

    private WorldSettings() {}

    public static void load(MinecraftServer server) {
        reset(); // defaults first, in case a previous world set them
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("justquests").resolve("settings.json");
        try {
            if (Files.exists(file)) {
                JsonElement root = JsonParser.parseString(Files.readString(file));
                if (root.isJsonObject()) {
                    JsonObject o = root.getAsJsonObject();
                    if (o.has("discordWelcome")) discordWelcome = o.get("discordWelcome").getAsBoolean();
                    if (o.has("announceCompletions")) announceCompletions = o.get("announceCompletions").getAsBoolean();
                    if (o.has("completionSound")) completionSound = o.get("completionSound").getAsBoolean();
                    if (o.has("completionToast")) completionToast = o.get("completionToast").getAsBoolean();
                }
            } else {
                Files.createDirectories(file.getParent());
                Files.writeString(file, TEMPLATE);
            }
        } catch (Exception e) {
            JustQuests.LOG.error("Could not load settings.json", e);
        }
    }

    public static boolean discordWelcome() { return discordWelcome; }

    public static boolean announceCompletions() { return announceCompletions; }

    public static boolean completionSound() { return completionSound; }

    public static boolean completionToast() { return completionToast; }

    public static void reset() {
        discordWelcome = true;
        announceCompletions = true;
        completionSound = true;
        completionToast = true;
    }

    private static final String TEMPLATE = """
        {
          "_help": "JustQuests per-world settings (singleplayer + server). discordWelcome: one-time clickable Discord invite on a player's first join. announceCompletions: broadcast to everyone when a player finishes a quest. completionSound: play a sound for the player on completion. completionToast: show an action-bar toast on completion.",
          "discordWelcome": true,
          "announceCompletions": true,
          "completionSound": true,
          "completionToast": true
        }
        """;
}
