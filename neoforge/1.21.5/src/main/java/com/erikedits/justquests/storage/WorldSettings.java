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
 * defaults is written on first run.
 */
public final class WorldSettings {
    private static boolean discordWelcome = true;
    private static boolean announceCompletions = true;

    private WorldSettings() {}

    public static void load(MinecraftServer server) {
        // reset to defaults first, in case a previous world set them
        discordWelcome = true;
        announceCompletions = true;
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("justquests").resolve("settings.json");
        try {
            if (Files.exists(file)) {
                JsonElement root = JsonParser.parseString(Files.readString(file));
                if (root.isJsonObject()) {
                    JsonObject o = root.getAsJsonObject();
                    if (o.has("discordWelcome")) discordWelcome = o.get("discordWelcome").getAsBoolean();
                    if (o.has("announceCompletions")) announceCompletions = o.get("announceCompletions").getAsBoolean();
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

    public static void reset() {
        discordWelcome = true;
        announceCompletions = true;
    }

    private static final String TEMPLATE = """
        {
          "_help": "JustQuests per-world settings. discordWelcome: show a one-time clickable Discord invite the first time each player joins. announceCompletions: broadcast a chat message to everyone when a player completes a quest.",
          "discordWelcome": true,
          "announceCompletions": true
        }
        """;
}
