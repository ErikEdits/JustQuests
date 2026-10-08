package com.erikedits.justquests.generator;

import com.erikedits.justquests.text.Msg;
import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.generator.v2.QuestGeneratorV2;
import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.ClaimState;
import com.erikedits.justquests.generator.v2.api.ClaimView;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.ExpiredClaim;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import com.erikedits.justquests.generator.v2.api.StartResult;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.erikedits.justquests.storage.WorldSettings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Glue between this build (fabric 26.1.2) and the generator v2 core (core INTEGRATION.md
 * sections 3-4). Holds the one generator of the running server; everything runs on the server
 * thread. Failures are logged and never take the server down: without a generator, generated
 * quests are simply absent.
 */
public final class GenV2 {
    private static MinecraftServer server;
    private static QuestGeneratorV2 gen;

    private GenV2() {}

    public static QuestGeneratorV2 get() { return gen; }

    /** Server start, after WorldQuestStore, WorldSettings and CustomQuestLoader are loaded. */
    public static void start(MinecraftServer srv) {
        try {
            server = srv;
            gen = new QuestGeneratorV2(new GenV2Host(srv), configFromSettings());
            Map<String, Set<UUID>> active = new HashMap<>();
            Map<String, Set<UUID>> waiting = new HashMap<>();
            WorldQuestStore store = WorldQuestStore.get();
            if (store != null) {
                store.allPlayers().forEach((uuid, data) -> {
                    data.active.keySet().forEach(id -> {
                        if (gen.isGenerated(id.toString())) active.computeIfAbsent(id.toString(), k -> new HashSet<>()).add(uuid);
                    });
                    // finished quests whose rewards wait: the generator keeps them until they are claimed
                    data.pendingClaim.keySet().forEach(id -> {
                        if (gen.isGenerated(id.toString())) waiting.computeIfAbsent(id.toString(), k -> new HashSet<>()).add(uuid);
                    });
                });
            }
            StartResult r = gen.startWithHolders(active, waiting);
            removeFromPlayers(r.deadQuestIds());
            applyExpired(r.releasedClaims());
            registerServed();
        } catch (RuntimeException e) {
            JustQuests.LOG.error("[GenV2] the quest generator failed to start; generated quests are off", e);
            gen = null;
        }
    }

    /** Every 6000 ticks (5 minutes). Rotates when a cycle boundary passed. */
    public static void tick() {
        if (gen == null) return;
        try {
            RotationResult r = gen.tick();
            applyExpired(r.expiredClaims());
            if (r.changed()) registerServed();
        } catch (RuntimeException e) {
            JustQuests.LOG.error("[GenV2] generator tick failed", e);
        }
    }

    /** Server stop. */
    public static void stop() {
        try {
            if (gen != null) gen.stop();
        } catch (RuntimeException e) {
            JustQuests.LOG.error("[GenV2] generator stop failed", e);
        }
        gen = null;
        server = null;
    }

    /** After /quest reload and every settings change. */
    public static void reloadConfig() {
        if (gen == null) return;
        boolean wasEnabled = gen.config().enabled();
        boolean wasExclusive = gen.config().exclusiveClaims();
        long rev = gen.servedRevision();
        gen.updateConfig(configFromSettings());
        if (wasEnabled != gen.config().enabled() || rev != gen.servedRevision()) registerServed();
        else if (wasExclusive != gen.config().exclusiveClaims()) syncClaims(null);
    }

    /** OP reroll. @return -1 if generated quests are disabled, else how many are offered now */
    public static int reroll() {
        if (gen == null || !gen.config().enabled()) return -1;
        gen.reroll();
        registerServed();
        return gen.servedQuests().size();
    }

    /** Claim before accepting. @return null to proceed, otherwise the message for the player */
    public static net.minecraft.network.chat.Component claim(Identifier id, UUID player) {
        if (gen == null) return null;
        ClaimResult r = gen.tryClaim(id.toString(), player);
        if (r == ClaimResult.OK) syncClaims(player);   // the other quest books now show it as taken
        if (r == ClaimResult.CLAIMED_BY_OTHER) {
            String name = playerName(gen.claim(id.toString()).holder());
            if (name != null) return Msg.tr("justquests.claim.taken_by", name);
        }
        return r.proceed() ? null : Msg.tr("justquests.claim." + r.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** The player abandoned the quest (or an admin reset it). */
    public static void abandoned(Identifier id, UUID player) {
        if (gen == null || !gen.isGenerated(id.toString())) return;
        long rev = gen.servedRevision();
        gen.onAbandon(id.toString(), player);
        if (rev != gen.servedRevision()) registerServed();
        else syncClaims(player);
    }

    /** The player completed the quest; {@code rewardsWait}: its rewards wait to be claimed (kept until then). */
    public static void completed(Identifier id, UUID player, boolean rewardsWait) {
        if (gen == null || !gen.isGenerated(id.toString())) return;
        gen.onComplete(id.toString(), player, rewardsWait);
        syncClaims(player);
    }

    /** The player claimed a finished quest's rewards (or an admin reset dropped them). */
    public static void rewardsClaimed(Identifier id, UUID player) {
        if (gen == null || !gen.isGenerated(id.toString())) return;
        long rev = gen.servedRevision();
        gen.onRewardsClaimed(id.toString(), player);
        if (rev != gen.servedRevision()) registerServed();   // a finished quest from an older cycle left
    }

    /** Admin reset of a player's data. */
    public static void releaseAllFor(UUID player) {
        if (gen == null) return;
        long rev = gen.servedRevision();
        gen.releaseAllFor(player);
        if (rev != gen.servedRevision()) registerServed();
        else syncClaims(player);
    }

    /** /quest generator release: free a claim and take the quest from its holder. */
    public static String forceRelease(Identifier id) {
        if (gen == null) return "§cThe quest generator is not running.";
        if (!gen.isGenerated(id.toString())) return "§c" + id + " is not a generated quest.";
        Optional<UUID> former = gen.forceRelease(id.toString());
        if (former.isEmpty()) return "§7" + id + " was not claimed by anyone.";
        WorldQuestStore store = WorldQuestStore.get();
        if (store != null) {
            PlayerQuestData data = store.peek(former.get());
            if (data != null) {
                data.abandon(id);
                store.markDirty();
            }
        }
        ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(former.get());
        if (p != null) {
            notify(p, Msg.tr("justquests.claim.released_by_op", title(id)));
            syncPlayer(p);
        }
        registerServed();
        return "§aReleased " + id + ".";
    }

    public static List<String> servedIds() {
        return gen == null ? List.of() : new ArrayList<>(gen.servedQuests().keySet());
    }

    public static String status() {
        return gen == null ? "§cThe quest generator is not running." : gen.status();
    }

    public static String statsText() {
        return gen == null ? "§cThe quest generator is not running." : gen.stats().toText();
    }

    public static String explain(Identifier id) {
        return gen == null ? "§cThe quest generator is not running." : gen.explain(id.toString());
    }

    /** /quest generator preview: the next cycle, nothing changes. */
    public static String preview(int count) {
        if (gen == null) return "§cThe quest generator is not running.";
        List<JsonObject> quests = gen.preview(count);
        StringBuilder sb = new StringBuilder("§7Preview of the next cycle (" + quests.size() + " quests, nothing changes):");
        for (JsonObject q : quests) {
            sb.append("\n§f").append(text(q.get("title"))).append(" §8- §7").append(objectives(q));
        }
        return sb.toString();
    }

    /** /quest test: running, served == registered, claims match player data. Empty = healthy. */
    public static List<String> selfTest() {
        List<String> problems = new ArrayList<>();
        if (gen == null) {
            problems.add("generator not running");
            return problems;
        }
        problems.addAll(gen.selfTest());
        Set<String> served = new LinkedHashSet<>(gen.servedQuests().keySet());
        Set<String> registered = new LinkedHashSet<>();
        QuestManager.INSTANCE.getQuests().keySet().forEach(id -> {
            if (gen.isGenerated(id.toString())) registered.add(id.toString());
        });
        if (!registered.equals(served)) {
            problems.add("registered generated quests (" + registered.size() + ") differ from the served set (" + served.size() + ")");
        }
        WorldQuestStore store = WorldQuestStore.get();
        if (store != null) {
            gen.claims().forEach((id, view) -> {
                if (view.state() != ClaimState.CLAIMED) return;
                Identifier rl = Identifier.tryParse(id);
                for (UUID holder : gen.holders(id)) {
                    PlayerQuestData d = store.peek(holder);
                    if (rl == null || d == null || !d.active.containsKey(rl)) {
                        problems.add("claim " + id + " is held by a player who does not have it active");
                    }
                }
            });
        }
        return problems;
    }

    public static String selfTestSummary() {
        if (gen == null) return "generator not running";
        long claimed = gen.claims().values().stream().filter(v -> v.state() == ClaimState.CLAIMED).count();
        return gen.servedQuests().size() + " served, " + claimed + " claimed, difficulty "
            + gen.config().difficulty().settingsValue();
    }

    /**
     * Claims for the quest book (exclusive claims only): quest id -> {"state": "claimed" or
     * "completed", "mine": true if {@code viewer} holds it, "by": the holder's name if known}.
     * Free quests are left out.
     */
    public static JsonObject claimsJson(UUID viewer) {
        JsonObject out = new JsonObject();
        if (gen == null || !gen.config().exclusiveClaims()) return out;
        gen.claims().forEach((id, view) -> {
            if (view.state() == ClaimState.AVAILABLE || view.holder() == null) return;
            JsonObject c = new JsonObject();
            c.addProperty("state", view.state() == ClaimState.COMPLETED ? "completed" : "claimed");
            c.addProperty("mine", view.holder().equals(viewer));
            String name = playerName(view.holder());
            if (name != null) c.addProperty("by", name);
            out.add(id, c);
        });
        return out;
    }

    /** /quest list suffix: " [yours]", " [taken by X]", " [completed by X]" or "" (free or not generated). */
    public static net.minecraft.network.chat.MutableComponent claimTag(Identifier id, UUID viewer) {
        if (gen == null || !gen.config().exclusiveClaims() || !gen.isGenerated(id.toString())) return Msg.empty();
        ClaimView view = gen.claim(id.toString());
        if (view.state() == ClaimState.AVAILABLE || view.holder() == null) return Msg.empty();
        if (view.holder().equals(viewer)) return view.state() == ClaimState.CLAIMED ? Msg.tr("justquests.claim.tag_yours") : Msg.empty();
        String name = playerName(view.holder());
        Object who = name != null ? name : Msg.tr("justquests.another_player");
        return Msg.tr(view.state() == ClaimState.COMPLETED ? "justquests.claim.tag_completed" : "justquests.claim.tag_taken", who);
    }

    // ------------------------------------------------------------------ helpers

    /** Parse servedQuests() with the mod's codec, hand them to the QuestManager, sync clients. */
    public static void registerServed() {
        Map<Identifier, Quest> map = new LinkedHashMap<>();
        if (gen != null) {
            gen.servedQuests().forEach((id, json) -> {
                Identifier rl = Identifier.tryParse(id);
                if (rl != null) Quest.CODEC.parse(JsonOps.INSTANCE, json).result().ifPresent(q -> map.put(rl, q));
            });
        }
        QuestManager.INSTANCE.setGeneratedQuests(map);
        syncAll();
    }

    private static void applyExpired(List<ExpiredClaim> expired) {
        WorldQuestStore store = WorldQuestStore.get();
        if (expired == null || expired.isEmpty() || store == null) return;
        for (ExpiredClaim e : expired) {
            Identifier id = Identifier.tryParse(e.questId());
            PlayerQuestData data = store.peek(e.holder());
            if (data != null && id != null) data.abandon(id);
            ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(e.holder());
            if (p != null) notify(p, Msg.tr("justquests.claim.expired", title(id)));
        }
        store.markDirty();
        syncClaims(null);   // the holders' progress and everyone's claims changed
    }

    private static void removeFromPlayers(List<String> ids) {
        WorldQuestStore store = WorldQuestStore.get();
        if (store == null || ids == null || ids.isEmpty()) return;
        for (String s : ids) {
            Identifier id = Identifier.tryParse(s);
            if (id != null) store.allPlayers().values().forEach(d -> d.abandon(id));
        }
        store.markDirty();
        JustQuests.LOG.info("[GenV2] removed " + ids.size() + " generated quest(s) that no longer exist from player data");
    }

    private static String title(Identifier id) {
        Quest q = id == null ? null : QuestManager.INSTANCE.get(id);
        return q == null ? String.valueOf(id) : q.title().getDefault();
    }

    private static String text(JsonElement e) {
        if (e == null || e.isJsonNull()) return "?";
        if (e.isJsonPrimitive()) return e.getAsString();
        if (e.isJsonObject() && e.getAsJsonObject().has("en_us")) return e.getAsJsonObject().get("en_us").getAsString();
        return e.toString();
    }

    private static String objectives(JsonObject q) {
        List<String> parts = new ArrayList<>();
        if (q.has("objectives") && q.get("objectives").isJsonArray()) {
            for (JsonElement el : q.getAsJsonArray("objectives")) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String type = o.has("type") ? o.get("type").getAsString().replace("justquests:", "") : "?";
                String target = o.has("item") ? o.get("item").getAsString()
                    : o.has("block") ? o.get("block").getAsString()
                    : o.has("entity") ? o.get("entity").getAsString()
                    : o.has("dimension") ? o.get("dimension").getAsString() : "";
                String count = o.has("count") ? " x" + o.get("count").getAsInt() : "";
                parts.add(type + " " + target + count);
            }
        }
        return String.join(", ", parts);
    }

    /** settings.json -> GeneratorConfig (the core clamps and logs out-of-range values). */
    public static GeneratorConfig configFromSettings() {
        return GeneratorConfig.builder()
            .enabled(WorldSettings.generatedQuests())
            .questsPerCycle(WorldSettings.generatedCount())
            .difficulty(Difficulty.parse(WorldSettings.difficulty()).orElse(Difficulty.NORMAL))
            .exclusiveClaims(WorldSettings.generatorExclusiveClaims())
            .oneActivePerPlayer(WorldSettings.generatorOneActivePerPlayer())
            .releaseOnAbandon(WorldSettings.generatorReleaseOnAbandon())
            .claimExpiryHours(WorldSettings.generatorClaimExpiryHours())
            .cycleHours(WorldSettings.generatorCycleHours())
            .cycleAnchorHour(WorldSettings.generatorCycleAnchorHour())
            .moddedShare(WorldSettings.generatorModdedShare())
            .disabledProfiles(new LinkedHashSet<>(WorldSettings.generatorDisabledProfiles()))
            .adaptiveBalancing(WorldSettings.generatorAdaptiveBalancing())
            .statsEnabled(WorldSettings.generatorStats())
            .build();
    }

    /** A player's name: online, else from the server's name cache; null if unknown. */
    private static String playerName(UUID id) {
        if (server == null || id == null) return null;
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) return p.getName().getString();
        return server.services().nameToIdCache().get(id).map(n -> n.name()).orElse(null);
    }

    /** Claims changed: resend progress (it carries the claims) to everyone online except {@code except}. */
    private static void syncClaims(UUID except) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.getUUID().equals(except)) syncPlayer(p);
        }
    }

    private static void notify(ServerPlayer p, net.minecraft.network.chat.Component msg) {
        p.sendSystemMessage(msg);
    }

    private static void syncAll() {
        if (server != null) com.erikedits.justquests.network.QuestNetwork.syncAll(server);
    }

    private static void syncPlayer(ServerPlayer p) {
        com.erikedits.justquests.network.QuestNetwork.syncProgress(p);
    }
}
