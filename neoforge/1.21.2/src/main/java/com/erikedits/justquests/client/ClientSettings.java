package com.erikedits.justquests.client;

import com.erikedits.justquests.JustQuests;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client options for the quest book and the HUD tracker, kept in
 * config/justquests-client.json. Local to this game install; never sent to a server.
 */
public final class ClientSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean loaded;

    /** HUD tracker visible. */
    public static boolean hud = true;
    /** HUD corner: top_left, top_right, bottom_left or bottom_right. */
    public static String hudCorner = "top_left";
    /** Most quests the HUD lists (1-5). */
    public static int hudMax = 3;
    /** Quest book groups by status instead of by category. */
    public static boolean byStatus = false;
    /** Quest book hides completed quests. */
    public static boolean hideCompleted = false;

    private ClientSettings() {}

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("justquests-client.json");
    }

    /** Read the file once; writes the defaults if it does not exist yet. */
    public static void load() {
        if (loaded) return;
        loaded = true;
        Path f = file();
        if (!Files.exists(f)) {
            save();
            return;
        }
        try {
            JsonObject o = GSON.fromJson(Files.readString(f), JsonObject.class);
            if (o == null) return;
            if (o.has("hud")) hud = o.get("hud").getAsBoolean();
            if (o.has("hudCorner")) hudCorner = o.get("hudCorner").getAsString();
            if (o.has("hudMax")) hudMax = Math.max(1, Math.min(5, o.get("hudMax").getAsInt()));
            if (o.has("byStatus")) byStatus = o.get("byStatus").getAsBoolean();
            if (o.has("hideCompleted")) hideCompleted = o.get("hideCompleted").getAsBoolean();
        } catch (Exception e) {
            JustQuests.LOG.warn("Could not read justquests-client.json, using defaults", e);
        }
    }

    public static void save() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "JustQuests client options. hud: show the quest tracker (toggle key H). "
            + "hudCorner: top_left, top_right, bottom_left or bottom_right. hudMax: quests shown (1-5). "
            + "byStatus / hideCompleted: quest book list options (also set by its buttons).");
        o.addProperty("hud", hud);
        o.addProperty("hudCorner", hudCorner);
        o.addProperty("hudMax", hudMax);
        o.addProperty("byStatus", byStatus);
        o.addProperty("hideCompleted", hideCompleted);
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(o));
        } catch (Exception e) {
            JustQuests.LOG.warn("Could not write justquests-client.json", e);
        }
    }
}
