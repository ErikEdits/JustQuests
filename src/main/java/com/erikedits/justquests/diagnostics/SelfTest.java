package com.erikedits.justquests.diagnostics;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.LocalizedText;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.erikedits.justquests.player.QuestProgress;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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
        java.util.Set<String> objTypes = new java.util.TreeSet<>();
        java.util.Set<String> rewTypes = new java.util.TreeSet<>();
        quests.values().forEach(q -> {
            q.objectives().forEach(o -> objTypes.add(o.typeId()));
            q.rewards().forEach(r -> rewTypes.add(r.typeId()));
        });
        out.append("  Objective types in use: ").append(objTypes).append("\n");
        out.append("  Reward types in use: ").append(rewTypes).append("\n");
        out.append("  Quests (id [category/mode] obj/rew):\n");
        quests.forEach((id, q) -> out.append("    ").append(id)
            .append(" [").append(q.category()).append("/").append(q.mode()).append("]")
            .append(" obj=").append(q.objectives().size())
            .append(" rew=").append(q.rewards().size()).append("\n"));
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

        // every loaded quest survives a Quest.CODEC round-trip (exercises
        // all objective + reward codecs actually in use, incl. tags/mode)
        boolean questRtOk = true;
        String questRtMsg = "ok";
        try {
            for (Map.Entry<ResourceLocation, Quest> e : quests.entrySet()) {
                Quest q = e.getValue();
                JsonElement enc = Quest.CODEC.encodeStart(JsonOps.INSTANCE, q).getOrThrow();
                Quest back = Quest.CODEC.parse(JsonOps.INSTANCE, enc).getOrThrow();
                if (!back.title().equals(q.title())
                        || back.objectives().size() != q.objectives().size()
                        || back.rewards().size() != q.rewards().size()) {
                    questRtOk = false;
                    questRtMsg = e.getKey() + " mismatch";
                    break;
                }
            }
        } catch (Exception ex) { questRtOk = false; questRtMsg = ex.toString(); }
        check(results, tally, "Quest codec round-trip (all loaded)", questRtOk, questRtMsg);

        // every objective type parses from a minimal sample (incl. tag form)
        String[][] objSamples = {
            {"collect_item (id)", "{\"type\":\"justquests:collect_item\",\"item\":\"minecraft:stone\",\"count\":1}"},
            {"collect_item (tag)", "{\"type\":\"justquests:collect_item\",\"item\":\"#minecraft:logs\",\"count\":1}"},
            {"kill_mob", "{\"type\":\"justquests:kill_mob\",\"entity\":\"minecraft:zombie\",\"count\":1}"},
            {"place_block", "{\"type\":\"justquests:place_block\",\"block\":\"minecraft:stone\",\"count\":1}"},
            {"craft_item", "{\"type\":\"justquests:craft_item\",\"item\":\"minecraft:bread\",\"count\":1}"},
            {"tame_animal", "{\"type\":\"justquests:tame_animal\",\"entity\":\"minecraft:wolf\",\"count\":1}"},
            {"gain_advancement", "{\"type\":\"justquests:gain_advancement\",\"advancement\":\"minecraft:story/mine_stone\"}"},
            {"visit_dimension", "{\"type\":\"justquests:visit_dimension\",\"dimension\":\"minecraft:the_nether\"}"},
            {"reach_level", "{\"type\":\"justquests:reach_level\",\"level\":30}"},
            {"reach_location", "{\"type\":\"justquests:reach_location\",\"x\":0,\"y\":64,\"z\":0}"},
        };
        String objErr = parsesAll(objSamples, true);
        check(results, tally, "All objective types parse", objErr == null,
            objErr == null ? objSamples.length + " samples ok" : objErr);

        // every reward type parses from a minimal sample
        String[][] rewSamples = {
            {"give_item", "{\"type\":\"justquests:give_item\",\"item\":\"minecraft:bread\",\"count\":1}"},
            {"command", "{\"type\":\"justquests:command\",\"command\":\"say hi\"}"},
            {"loot_table", "{\"type\":\"justquests:loot_table\",\"loot_table\":\"minecraft:chests/simple_dungeon\"}"},
        };
        String rewErr = parsesAll(rewSamples, false);
        check(results, tally, "All reward types parse", rewErr == null,
            rewErr == null ? rewSamples.length + " samples ok" : rewErr);

        // localized title: plain string still works, per-language map resolves,
        // and an unknown language falls back to English (Q21)
        boolean l10nOk = true;
        String l10nMsg = "ok";
        try {
            String json = "{\"title\":{\"en_us\":\"Hello\",\"de_de\":\"Hallo\"},"
                + "\"objectives\":[{\"type\":\"justquests:reach_level\",\"level\":1}],\"rewards\":[]}";
            Quest q = Quest.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
            boolean de = "Hallo".equals(q.title().get("de_de"));
            boolean fallback = "Hello".equals(q.title().get("fr_fr"));   // unknown -> English
            LocalizedText plain = LocalizedText.CODEC.parse(
                JsonOps.INSTANCE, JsonParser.parseString("\"Hi\"")).getOrThrow();
            boolean str = "Hi".equals(plain.get("anything"));
            l10nOk = de && fallback && str;
            if (!l10nOk) l10nMsg = "de=" + de + " fallback=" + fallback + " string=" + str;
        } catch (Exception ex) { l10nOk = false; l10nMsg = ex.toString(); }
        check(results, tally, "Localized text (map + English fallback)", l10nOk, l10nMsg);

        // community hint: invite set and clickable messages build (0.1.5)
        boolean comOk = true;
        String comMsg = "ok";
        try {
            comOk = com.erikedits.justquests.community.CommunityHints.INVITE.startsWith("https://")
                && com.erikedits.justquests.community.CommunityHints.welcomeMessage() != null
                && com.erikedits.justquests.community.CommunityHints.discordMessage() != null
                && com.erikedits.justquests.community.CommunityHints.link() != null;
        } catch (Exception ex) { comOk = false; comMsg = ex.toString(); }
        check(results, tally, "Community hint (invite + messages)", comOk, comMsg);

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

    /** Parses each {label, json} sample; returns null if all parse, else the failing label+error. */
    private static String parsesAll(String[][] samples, boolean objective) {
        for (String[] s : samples) {
            try {
                JsonElement el = JsonParser.parseString(s[1]);
                if (objective) {
                    com.erikedits.justquests.data.objective.QuestObjective.CODEC.parse(JsonOps.INSTANCE, el).getOrThrow();
                } else {
                    com.erikedits.justquests.data.reward.QuestReward.CODEC.parse(JsonOps.INSTANCE, el).getOrThrow();
                }
            } catch (Exception ex) {
                return s[0] + ": " + ex.getMessage();
            }
        }
        return null;
    }
}
