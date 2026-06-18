package com.erikedits.justquests.diagnostics;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.erikedits.justquests.player.QuestProgress;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Diagnostics self-test. `/quest test` runs a battery of checks on the mod
 * AND collects Minecraft environment/state, then appends a full report to
 * justquests-diagnostics.log in the game (instance) folder.
 */
public final class SelfTest {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SelfTest() {}

    /** Runs the self-test, writes the report, returns a short summary line. */
    public static String run(CommandSourceStack src) {
        StringBuilder out = new StringBuilder();
        List<String> results = new ArrayList<>();
        int[] tally = {0, 0}; // passed, failed

        MinecraftServer server = src.getServer();
        ServerPlayer player = src.getPlayer();

        out.append("======== JustQuests Self-Test - ").append(LocalDateTime.now().format(TS)).append(" ========\n");

        // --- Environment (Minecraft) ---
        out.append("[ENVIRONMENT]\n");
        safe(out, "Minecraft", () -> server.getServerVersion());
        safe(out, "Server type", () -> server.isDedicatedServer() ? "dedicated" : "integrated");
        safe(out, "Players online", () -> String.valueOf(server.getPlayerCount()));
        safe(out, "Worlds loaded", () -> {
            int n = 0; for (var ignored : server.getAllLevels()) n++; return String.valueOf(n);
        });
        safe(out, "Game dir", () -> FMLPaths.GAMEDIR.get().toString());
        safe(out, "Java", () -> System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        safe(out, "OS", () -> System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        safe(out, "Memory", () -> {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) / 1048576L;
            long max = r.maxMemory() / 1048576L;
            return used + " MB used / " + max + " MB max";
        });
        safe(out, "Loaded mods", () -> {
            StringBuilder m = new StringBuilder();
            var mods = ModList.get().getMods();
            m.append(mods.size()).append(": ");
            mods.forEach(mod -> m.append(mod.getModId()).append(" ").append(mod.getVersion()).append(", "));
            return m.toString();
        });

        // --- Mod state ---
        out.append("[MOD STATE]\n");
        Map<ResourceLocation, Quest> quests = QuestManager.INSTANCE.getQuests();
        out.append("  Quests loaded: ").append(quests.size()).append("\n");
        out.append("  Quest ids: ").append(quests.keySet()).append("\n");
        WorldQuestStore store = WorldQuestStore.get();
        out.append("  Store loaded: ").append(store != null ? "yes" : "no").append("\n");
        if (player != null) {
            PlayerQuestData d = store != null ? store.peek(player.getUUID()) : null;
            if (d != null) {
                out.append("  Requester ").append(player.getGameProfile().getName())
                   .append(": active=").append(d.active.keySet())
                   .append(", completed=").append(d.completed.keySet()).append("\n");
            } else {
                out.append("  Requester ").append(player.getGameProfile().getName()).append(": no data\n");
            }
        }

        // --- Tests ---
        out.append("[TESTS]\n");

        check(results, tally, "Quests loaded (>=1)", quests.size() >= 1,
            quests.size() + " quests");

        // every quest valid: objectives present, counts > 0, displayName ok
        boolean questsValid = true;
        String questIssue = "";
        try {
            for (Map.Entry<ResourceLocation, Quest> e : quests.entrySet()) {
                Quest q = e.getValue();
                if (q.objectives().isEmpty()) { questsValid = false; questIssue = e.getKey() + " has no objectives"; break; }
                for (QuestObjective o : q.objectives()) {
                    if (o.requiredCount() <= 0) { questsValid = false; questIssue = e.getKey() + " count<=0"; break; }
                    o.displayName(); // must not throw
                }
                for (QuestReward r : q.rewards()) r.displayName();
                if (!questsValid) break;
            }
        } catch (Exception ex) { questsValid = false; questIssue = ex.toString(); }
        check(results, tally, "All quests valid", questsValid, questsValid ? "ok" : questIssue);

        // codec round-trip
        boolean codecOk;
        String codecMsg;
        try {
            PlayerQuestData sample = new PlayerQuestData();
            QuestProgress p = new QuestProgress();
            p.increment(0, 7);
            sample.active.put(ResourceLocation.parse("justquests:_selftest"), p);
            sample.completed.put(ResourceLocation.parse("justquests:_done"), 123L);
            JsonElement enc = PlayerQuestData.CODEC.encodeStart(JsonOps.INSTANCE, sample).getOrThrow();
            PlayerQuestData dec = PlayerQuestData.CODEC.parse(JsonOps.INSTANCE, enc).getOrThrow();
            codecOk = dec.active.containsKey(ResourceLocation.parse("justquests:_selftest"))
                   && dec.completed.getOrDefault(ResourceLocation.parse("justquests:_done"), -1L) == 123L
                   && dec.active.get(ResourceLocation.parse("justquests:_selftest")).get(0) == 7;
            codecMsg = codecOk ? "encode/decode equal" : "round-trip mismatch";
        } catch (Exception ex) { codecOk = false; codecMsg = ex.toString(); }
        check(results, tally, "Codec round-trip (PlayerQuestData)", codecOk, codecMsg);

        // storage writable: temp write/read/delete in the game dir
        boolean storageOk;
        String storageMsg;
        try {
            Path tmp = FMLPaths.GAMEDIR.get().resolve("justquests-selftest.tmp");
            Files.writeString(tmp, "ok");
            String back = Files.readString(tmp);
            Files.deleteIfExists(tmp);
            storageOk = "ok".equals(back);
            storageMsg = storageOk ? "write/read/delete ok" : "readback mismatch";
        } catch (Exception ex) { storageOk = false; storageMsg = ex.toString(); }
        check(results, tally, "Storage writable", storageOk, storageMsg);

        // store present
        check(results, tally, "World quest store loaded", store != null,
            store != null ? "ok" : "store is null");

        for (String r : results) out.append("  ").append(r).append("\n");
        out.append("SUMMARY: ").append(tally[0]).append(" passed, ").append(tally[1]).append(" failed\n");
        out.append("========================================================\n\n");

        // write report (append) to the instance/game folder
        Path report = FMLPaths.GAMEDIR.get().resolve("justquests-diagnostics.log");
        try {
            Files.writeString(report, out.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex) {
            JustQuests.LOG.error("Could not write diagnostics log", ex);
            return "Self-test ran but the report could not be written: " + ex.getMessage();
        }

        // also echo to the server log
        JustQuests.LOG.info("Self-test: {} passed, {} failed (report: {})", tally[0], tally[1], report);
        return tally[1] == 0
            ? "§aSelf-test: all " + tally[0] + " checks passed. Report: justquests-diagnostics.log"
            : "§eSelf-test: " + tally[0] + " passed, §c" + tally[1] + " failed§e. See justquests-diagnostics.log";
    }

    private interface Probe { String get() throws Exception; }

    private static void safe(StringBuilder out, String label, Probe p) {
        try { out.append("  ").append(label).append(": ").append(p.get()).append("\n"); }
        catch (Exception e) { out.append("  ").append(label).append(": <error: ").append(e.getMessage()).append(">\n"); }
    }

    private static void check(List<String> results, int[] tally, String name, boolean pass, String detail) {
        results.add((pass ? "[PASS] " : "[FAIL] ") + name + " - " + detail);
        if (pass) tally[0]++; else tally[1]++;
    }
}
