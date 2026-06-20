package com.erikedits.justquests.community;

import com.erikedits.justquests.JustQuests;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Points players to the community Discord (0.1.5). The upcoming v0.2 GUI is
 * shaped by community polls, so growing the Discord is what unblocks it.
 *
 * <p>Shows a single, clickable welcome the first time each player joins a
 * world (persisted in {@code <world>/justquests/seen-players.json}, so it
 * never repeats), plus a {@code /quest discord} command for anytime. Server
 * owners can switch the welcome off in
 * {@code <world>/justquests/settings.json} ({@code "discordWelcome": false}).
 */
public final class CommunityHints {
    public static final String INVITE = "https://discord.gg/cMTGE9QCja";

    private static Path seenFile;
    private static Path settingsFile;
    private static final Set<UUID> seen = new HashSet<>();
    private static boolean welcomeEnabled = true;

    private CommunityHints() {}

    public static void init(MinecraftServer server) {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("justquests");
        seenFile = dir.resolve("seen-players.json");
        settingsFile = dir.resolve("settings.json");
        seen.clear();
        welcomeEnabled = true;
        try {
            Files.createDirectories(dir);
            if (Files.exists(settingsFile)) {
                JsonElement root = JsonParser.parseString(Files.readString(settingsFile));
                if (root.isJsonObject() && root.getAsJsonObject().has("discordWelcome")) {
                    welcomeEnabled = root.getAsJsonObject().get("discordWelcome").getAsBoolean();
                }
            } else {
                Files.writeString(settingsFile, SETTINGS_TEMPLATE);
            }
            if (Files.exists(seenFile)) {
                JsonElement root = JsonParser.parseString(Files.readString(seenFile));
                if (root.isJsonArray()) {
                    for (JsonElement e : root.getAsJsonArray()) {
                        try { seen.add(UUID.fromString(e.getAsString())); } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            JustQuests.LOG.error("Could not load community settings", e);
        }
    }

    /** Sends the one-time welcome on a player's first ever join to this world. */
    public static void onLogin(ServerPlayer player) {
        if (!welcomeEnabled || seenFile == null) return;
        if (!seen.add(player.getUUID())) return;   // already welcomed before
        save();
        player.sendSystemMessage(welcomeMessage());
    }

    public static void clear() {
        seenFile = null;
        settingsFile = null;
        seen.clear();
        welcomeEnabled = true;
    }

    private static void save() {
        try {
            JsonArray arr = new JsonArray();
            for (UUID id : seen) arr.add(id.toString());
            Files.writeString(seenFile, arr.toString());
        } catch (Exception e) {
            JustQuests.LOG.error("Could not save seen-players.json", e);
        }
    }

    /** Clickable invite, reused by the welcome and the /quest discord command. */
    public static Component link() {
        return Component.literal("§9§n" + INVITE).setStyle(Style.EMPTY
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, INVITE))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.literal("Click to open the Discord invite"))));
    }

    public static Component welcomeMessage() {
        MutableComponent m = Component.literal("§6> JustQuests §7- thanks for playing!\n");
        m.append(Component.literal("§fJoin the community Discord to:\n"));
        m.append(Component.literal("§b  - Vote on the upcoming in-game GUI (v0.2)\n"));
        m.append(Component.literal("§b  - Get help, report bugs & follow updates\n"));
        m.append(Component.literal("§b  - See sneak peeks and early builds\n"));
        m.append(Component.literal("§7  ")).append(link());
        m.append(Component.literal("  §8(/quest discord anytime)"));
        return m;
    }

    public static Component discordMessage() {
        MutableComponent m = Component.literal(
            "§6JustQuests Discord §7- vote on the v0.2 GUI, get support & sneak peeks:\n§7");
        m.append(link());
        return m;
    }

    private static final String SETTINGS_TEMPLATE = """
        {
          "_help": "JustQuests per-world settings. discordWelcome: show a one-time clickable Discord invite the first time each player joins (set to false to turn it off).",
          "discordWelcome": true
        }
        """;
}
