package com.erikedits.justquests.storage;

import com.erikedits.justquests.JustQuests;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
 * defaults is written on first run, and {@link #save} persists runtime changes
 * (e.g. {@code /quest mainquests off}). Works in singleplayer and on servers
 * (singleplayer uses the integrated server's world path).
 */
public final class WorldSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean discordWelcome = true;
    private static boolean announceCompletions = true;
    private static boolean completionSound = true;
    private static boolean completionToast = true;
    private static boolean mainQuests = true;
    private static boolean generatedQuests = true;
    private static int generatedCount = 5;

    private WorldSettings() {}

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("justquests").resolve("settings.json");
    }

    public static void load(MinecraftServer server) {
        reset(); // defaults first, in case a previous world set them
        Path file = file(server);
        try {
            if (Files.exists(file)) {
                JsonElement root = JsonParser.parseString(Files.readString(file));
                if (root.isJsonObject()) {
                    JsonObject o = root.getAsJsonObject();
                    if (o.has("discordWelcome")) discordWelcome = o.get("discordWelcome").getAsBoolean();
                    if (o.has("announceCompletions")) announceCompletions = o.get("announceCompletions").getAsBoolean();
                    if (o.has("completionSound")) completionSound = o.get("completionSound").getAsBoolean();
                    if (o.has("completionToast")) completionToast = o.get("completionToast").getAsBoolean();
                    if (o.has("mainQuests")) mainQuests = o.get("mainQuests").getAsBoolean();
                    if (o.has("generatedQuests")) generatedQuests = o.get("generatedQuests").getAsBoolean();
                    if (o.has("generatedCount")) generatedCount = o.get("generatedCount").getAsInt();
                }
            } else {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(toJson()));
            }
        } catch (Exception e) {
            JustQuests.LOG.error("Could not load settings.json", e);
        }
    }

    /** Persist the current settings back to the world file. */
    public static void save(MinecraftServer server) {
        Path file = file(server);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(toJson()));
        } catch (Exception e) {
            JustQuests.LOG.error("Could not save settings.json", e);
        }
    }

    private static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", HELP);
        o.addProperty("discordWelcome", discordWelcome);
        o.addProperty("announceCompletions", announceCompletions);
        o.addProperty("completionSound", completionSound);
        o.addProperty("completionToast", completionToast);
        o.addProperty("mainQuests", mainQuests);
        o.addProperty("generatedQuests", generatedQuests);
        o.addProperty("generatedCount", generatedCount);
        return o;
    }

    public static boolean discordWelcome() { return discordWelcome; }

    public static boolean announceCompletions() { return announceCompletions; }

    public static boolean completionSound() { return completionSound; }

    public static boolean completionToast() { return completionToast; }

    public static boolean mainQuests() { return mainQuests; }

    public static void setMainQuests(boolean value) { mainQuests = value; }

    public static boolean generatedQuests() { return generatedQuests; }

    public static int generatedCount() { return generatedCount; }

    public static void reset() {
        discordWelcome = true;
        announceCompletions = true;
        completionSound = true;
        completionToast = true;
        mainQuests = true;
        generatedQuests = true;
        generatedCount = 5;
    }

    private static final String HELP =
        "JustQuests per-world settings (singleplayer + server). "
        + "discordWelcome: one-time clickable Discord invite on a player's first join. "
        + "announceCompletions: broadcast to everyone when a player finishes a quest. "
        + "completionSound: play a sound for the player on completion. "
        + "completionToast: show an action-bar toast on completion. "
        + "mainQuests: the built-in quests bundled with the mod; set false to hide them from /quest list and the quest book (custom + generated quests stay). Toggle in-game with /quest mainquests on|off (OP). "
        + "generatedQuests: auto-generate a rotating set of quests every 12h (category 'generated'); set false to disable. "
        + "generatedCount: how many generated quests per 12h cycle.";
}
