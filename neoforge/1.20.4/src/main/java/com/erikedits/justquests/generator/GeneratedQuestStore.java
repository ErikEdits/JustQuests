package com.erikedits.justquests.generator;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.storage.WorldSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Per-world generated-quest state (Phase 6 v1). Holds the current generated
 * set, the last-refresh timestamp and a 6-day dedupe history, persisted to
 * {@code <world>/justquests/generated.json}. Rotates every 12 hours based on
 * the real clock (so closing the game and returning later still rotates).
 * Feeds the set into {@link QuestManager} as a third quest source.
 */
public final class GeneratedQuestStore {
    private static final long CYCLE_MS = 12L * 60 * 60 * 1000;   // 12 hours
    private static final long HISTORY_MS = 6L * 24 * 60 * 60 * 1000; // 6 days
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static MinecraftServer server;
    private static long lastRefresh;
    private static final Map<String, Long> history = new HashMap<>();
    private static Map<ResourceLocation, Quest> current = new HashMap<>();

    private GeneratedQuestStore() {}

    public static void init(MinecraftServer srv) {
        server = srv;
        lastRefresh = 0L;
        history.clear();
        current = new HashMap<>();

        if (!WorldSettings.generatedQuests()) {
            QuestManager.INSTANCE.setGeneratedQuests(Map.of());
            return;
        }
        load();
        long now = System.currentTimeMillis();
        if (current.isEmpty() || now - lastRefresh >= CYCLE_MS) {
            regenerate(now);
        } else {
            QuestManager.INSTANCE.setGeneratedQuests(current);
            JustQuests.LOG.info("Loaded {} generated quest(s); next rotation when 12h elapse", current.size());
        }
    }

    /** Called periodically from the server tick: rotate if a 12h cycle has passed. */
    public static void tickCheck() {
        if (server == null || !WorldSettings.generatedQuests()) return;
        if (System.currentTimeMillis() - lastRefresh >= CYCLE_MS) {
            regenerate(System.currentTimeMillis());
        }
    }

    /** OP-triggered manual reroll. */
    public static int reroll() {
        if (server == null || !WorldSettings.generatedQuests()) return -1;
        regenerate(System.currentTimeMillis());
        return current.size();
    }

    public static void clear() {
        server = null;
        current = new HashMap<>();
        history.clear();
    }

    private static void regenerate(long now) {
        pruneHistory(now);
        int count = Math.max(0, WorldSettings.generatedCount());
        var generated = QuestGenerator.generate(count, history.keySet(), new Random(now), now);

        Map<ResourceLocation, Quest> map = new HashMap<>();
        Map<String, JsonObject> jsonById = new HashMap<>();
        for (QuestGenerator.GenQuest g : generated) {
            ResourceLocation id = new ResourceLocation(JustQuests.MOD_ID, "gen/" + g.idPath());
            Quest.CODEC.parse(JsonOps.INSTANCE, g.json())
                .resultOrPartial(err -> JustQuests.LOG.error("Generated quest {} failed to parse: {}", g.idPath(), err))
                .ifPresent(q -> {
                    map.put(id, q);
                    jsonById.put(g.idPath(), g.json());
                    history.put(g.signature(), now);
                });
        }
        current = map;
        lastRefresh = now;
        QuestManager.INSTANCE.setGeneratedQuests(current);
        save(jsonById);
        JustQuests.LOG.info("Generated {} quest(s); next rotation in 12h", map.size());
    }

    private static void pruneHistory(long now) {
        history.entrySet().removeIf(e -> now - e.getValue() > HISTORY_MS);
    }

    private static Path file() {
        return server.getWorldPath(LevelResource.ROOT).resolve("justquests").resolve("generated.json");
    }

    private static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonElement root = JsonParser.parseString(Files.readString(f));
            if (!root.isJsonObject()) return;
            JsonObject o = root.getAsJsonObject();
            if (o.has("lastRefresh")) lastRefresh = o.get("lastRefresh").getAsLong();
            if (o.has("history") && o.get("history").isJsonObject()) {
                for (var e : o.getAsJsonObject("history").entrySet()) {
                    history.put(e.getKey(), e.getValue().getAsLong());
                }
            }
            if (o.has("quests") && o.get("quests").isJsonObject()) {
                Map<ResourceLocation, Quest> loaded = new HashMap<>();
                for (var e : o.getAsJsonObject("quests").entrySet()) {
                    ResourceLocation id = new ResourceLocation(JustQuests.MOD_ID, "gen/" + e.getKey());
                    Quest.CODEC.parse(JsonOps.INSTANCE, e.getValue())
                        .result().ifPresent(q -> loaded.put(id, q));
                }
                current = loaded;
            }
        } catch (Exception e) {
            JustQuests.LOG.error("Could not load generated.json", e);
        }
    }

    private static void save(Map<String, JsonObject> jsonById) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("lastRefresh", lastRefresh);
            JsonObject hist = new JsonObject();
            history.forEach(hist::addProperty);
            root.add("history", hist);
            JsonObject quests = new JsonObject();
            jsonById.forEach(quests::add);
            root.add("quests", quests);
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(root));
        } catch (Exception e) {
            JustQuests.LOG.error("Could not save generated.json", e);
        }
    }
}
