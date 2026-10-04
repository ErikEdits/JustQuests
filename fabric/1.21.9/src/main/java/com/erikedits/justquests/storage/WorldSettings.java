package com.erikedits.justquests.storage;

import com.erikedits.justquests.JustQuests;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-world settings loaded from {@code <world>/justquests/settings.json}.
 * The single home for server-owner toggles (Q35 groundwork). A template with
 * defaults is written on first run, and {@link #save} persists runtime changes
 * (e.g. {@code /quest mainquests off}, {@code /quest difficulty hard}). Works in
 * singleplayer and on servers (singleplayer uses the integrated server's world path).
 */
public final class WorldSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean discordWelcome = true;
    private static boolean announceCompletions = true;
    private static boolean completionSound = true;
    private static boolean completionToast = true;
    private static boolean claimRewards = true;
    private static boolean mainQuests = true;
    private static boolean generatedQuests = true;
    private static int generatedCount = 5;
    // generator v2 (0.3.0)
    private static String difficulty = "normal";
    private static boolean generatorExclusiveClaims = true;
    private static boolean generatorOneActivePerPlayer = true;
    private static boolean generatorReleaseOnAbandon = true;
    private static int generatorClaimExpiryHours = 0;
    private static int generatorCycleHours = 12;
    private static int generatorCycleAnchorHour = 0;
    private static double generatorModdedShare = 0.35;
    private static List<String> generatorDisabledProfiles = new ArrayList<>();
    private static boolean generatorAdaptiveBalancing = false;
    private static boolean generatorStats = true;

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
                    if (o.has("claimRewards")) claimRewards = o.get("claimRewards").getAsBoolean();
                    if (o.has("mainQuests")) mainQuests = o.get("mainQuests").getAsBoolean();
                    if (o.has("generatedQuests")) generatedQuests = o.get("generatedQuests").getAsBoolean();
                    if (o.has("generatedCount")) generatedCount = o.get("generatedCount").getAsInt();
                    if (o.has("difficulty")) difficulty = o.get("difficulty").getAsString();
                    if (o.has("generatorExclusiveClaims")) generatorExclusiveClaims = o.get("generatorExclusiveClaims").getAsBoolean();
                    if (o.has("generatorOneActivePerPlayer")) generatorOneActivePerPlayer = o.get("generatorOneActivePerPlayer").getAsBoolean();
                    if (o.has("generatorReleaseOnAbandon")) generatorReleaseOnAbandon = o.get("generatorReleaseOnAbandon").getAsBoolean();
                    if (o.has("generatorClaimExpiryHours")) generatorClaimExpiryHours = o.get("generatorClaimExpiryHours").getAsInt();
                    if (o.has("generatorCycleHours")) generatorCycleHours = o.get("generatorCycleHours").getAsInt();
                    if (o.has("generatorCycleAnchorHour")) generatorCycleAnchorHour = o.get("generatorCycleAnchorHour").getAsInt();
                    if (o.has("generatorModdedShare")) generatorModdedShare = o.get("generatorModdedShare").getAsDouble();
                    if (o.has("generatorDisabledProfiles") && o.get("generatorDisabledProfiles").isJsonArray()) {
                        List<String> list = new ArrayList<>();
                        for (JsonElement e : o.getAsJsonArray("generatorDisabledProfiles")) list.add(e.getAsString());
                        generatorDisabledProfiles = list;
                    }
                    if (o.has("generatorAdaptiveBalancing")) generatorAdaptiveBalancing = o.get("generatorAdaptiveBalancing").getAsBoolean();
                    if (o.has("generatorStats")) generatorStats = o.get("generatorStats").getAsBoolean();
                    // options added by a newer version are written into the file, so owners see them
                    if (toJson().keySet().stream().anyMatch(k -> !o.has(k))) save(server);
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
        o.addProperty("claimRewards", claimRewards);
        o.addProperty("mainQuests", mainQuests);
        o.addProperty("generatedQuests", generatedQuests);
        o.addProperty("generatedCount", generatedCount);
        o.addProperty("difficulty", difficulty);
        o.addProperty("generatorExclusiveClaims", generatorExclusiveClaims);
        o.addProperty("generatorOneActivePerPlayer", generatorOneActivePerPlayer);
        o.addProperty("generatorReleaseOnAbandon", generatorReleaseOnAbandon);
        o.addProperty("generatorClaimExpiryHours", generatorClaimExpiryHours);
        o.addProperty("generatorCycleHours", generatorCycleHours);
        o.addProperty("generatorCycleAnchorHour", generatorCycleAnchorHour);
        o.addProperty("generatorModdedShare", generatorModdedShare);
        JsonArray profiles = new JsonArray();
        generatorDisabledProfiles.forEach(profiles::add);
        o.add("generatorDisabledProfiles", profiles);
        o.addProperty("generatorAdaptiveBalancing", generatorAdaptiveBalancing);
        o.addProperty("generatorStats", generatorStats);
        return o;
    }

    public static boolean discordWelcome() { return discordWelcome; }

    public static boolean announceCompletions() { return announceCompletions; }

    public static boolean completionSound() { return completionSound; }

    public static boolean completionToast() { return completionToast; }

    public static boolean claimRewards() { return claimRewards; }

    public static boolean mainQuests() { return mainQuests; }

    public static void setMainQuests(boolean value) { mainQuests = value; }

    public static boolean generatedQuests() { return generatedQuests; }

    public static int generatedCount() { return generatedCount; }

    public static String difficulty() { return difficulty; }

    public static void setDifficulty(String value) { difficulty = value; }

    public static boolean generatorExclusiveClaims() { return generatorExclusiveClaims; }

    public static boolean generatorOneActivePerPlayer() { return generatorOneActivePerPlayer; }

    public static boolean generatorReleaseOnAbandon() { return generatorReleaseOnAbandon; }

    public static int generatorClaimExpiryHours() { return generatorClaimExpiryHours; }

    public static int generatorCycleHours() { return generatorCycleHours; }

    public static int generatorCycleAnchorHour() { return generatorCycleAnchorHour; }

    public static double generatorModdedShare() { return generatorModdedShare; }

    public static List<String> generatorDisabledProfiles() { return generatorDisabledProfiles; }

    public static boolean generatorAdaptiveBalancing() { return generatorAdaptiveBalancing; }

    public static boolean generatorStats() { return generatorStats; }

    public static void reset() {
        discordWelcome = true;
        announceCompletions = true;
        completionSound = true;
        completionToast = true;
        claimRewards = true;
        mainQuests = true;
        generatedQuests = true;
        generatedCount = 5;
        difficulty = "normal";
        generatorExclusiveClaims = true;
        generatorOneActivePerPlayer = true;
        generatorReleaseOnAbandon = true;
        generatorClaimExpiryHours = 0;
        generatorCycleHours = 12;
        generatorCycleAnchorHour = 0;
        generatorModdedShare = 0.35;
        generatorDisabledProfiles = new ArrayList<>();
        generatorAdaptiveBalancing = false;
        generatorStats = true;
    }

    private static final String HELP =
        "JustQuests per-world settings (singleplayer + server). "
        + "discordWelcome: one-time clickable Discord invite on a player's first join. "
        + "announceCompletions: broadcast to everyone when a player finishes a quest. "
        + "completionSound: play a sound for the player on completion. "
        + "completionToast: show an action-bar toast on completion. "
        + "claimRewards: finished quests wait until the player claims their rewards (Claim button in the quest book, or /quest claim); false pays rewards out the moment a quest is finished. "
        + "mainQuests: the built-in quests bundled with the mod; set false to hide them from /quest list and the quest book (custom + generated quests stay). Toggle in-game with /quest mainquests on|off (OP). "
        + "generatedQuests: auto-generate a rotating set of quests (category 'generated'); set false to disable. "
        + "generatedCount: how many generated quests per cycle (1-20). "
        + "difficulty: easy, normal or hard - scales effort, content tiers, objectives and rewards of generated quests (OP: /quest difficulty). "
        + "generatorExclusiveClaims: a generated quest accepted by one player is locked for everyone else. "
        + "generatorOneActivePerPlayer: each player may hold at most one unfinished generated quest. "
        + "generatorReleaseOnAbandon: an abandoned generated quest becomes available again (false: it disappears). "
        + "generatorClaimExpiryHours: release accepted generated quests after this many hours (0 = never). "
        + "generatorCycleHours / generatorCycleAnchorHour: rotation period and the local hour of the first rotation of the day. "
        + "generatorModdedShare: share (0-1) of generated quests that use content from supported mods. "
        + "generatorDisabledProfiles: mod profiles to ignore, e.g. [\"create\"]. "
        + "generatorAdaptiveBalancing: let the generator tune its time estimates from how long quests really take. "
        + "generatorStats: record anonymous statistics in justquests/generator_v2_stats.json.";
}
