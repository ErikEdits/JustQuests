// REFERENCE ONLY — NOT COMPILED
// Glue between the Forge 1.20.1 tree and the generator v2 core (INTEGRATION.md §3/§4).
package com.erikedits.justquests.generator;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.LocalizedText;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.generator.v2.QuestGeneratorV2;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.ExpiredClaim;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import com.erikedits.justquests.generator.v2.api.StartResult;
import com.erikedits.justquests.network.QuestNetwork;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.erikedits.justquests.storage.WorldSettings;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Holds the one generator instance of the running server. */
public final class GenV2 {
    private static QuestGeneratorV2 gen;   // cleared on stop; a new server creates a new instance
    private static GenV2Host host;

    private GenV2() {
    }

    public static QuestGeneratorV2 get() {
        return gen;
    }

    /** ServerStartingEvent, after WorldQuestStore.load, WorldSettings.load, CustomQuestLoader.init. */
    public static void start(MinecraftServer server) {
        host = new GenV2Host(server);
        gen = new QuestGeneratorV2(host, configFromSettings());
        Map<String, Set<UUID>> active = new HashMap<>();
        WorldQuestStore store = WorldQuestStore.get();
        if (store != null) {
            store.allPlayers().forEach((uuid, data) -> data.active.keySet().forEach(id -> {
                if (gen.isGenerated(id.toString())) {
                    active.computeIfAbsent(id.toString(), k -> new HashSet<>()).add(uuid);
                }
            }));
        }
        StartResult r = gen.startWithHolders(active);
        if (store != null && !r.deadQuestIds().isEmpty()) {
            for (String dead : r.deadQuestIds()) {
                ResourceLocation id = new ResourceLocation(dead);
                store.allPlayers().values().forEach(d -> d.abandon(id));
            }
            store.markDirty();
            JustQuests.LOG.info("[GenV2] removed {} dead generated quest(s) from player data", r.deadQuestIds().size());
        }
        registerServed(server);
    }

    /** Every 6000 ticks from ServerStorageEvents.onServerTick. */
    public static void tick(MinecraftServer server) {
        if (gen == null) return;
        RotationResult r = gen.tick();
        applyExpired(server, r.expiredClaims());
        if (r.changed()) registerServed(server);
    }

    /** ServerStoppingEvent. */
    public static void stop() {
        if (gen != null) gen.stop();
        gen = null;
        host = null;
    }

    /** /quest reload and every settings change. */
    public static void reloadConfig(MinecraftServer server) {
        if (gen == null) return;
        if (host != null) host.invalidateCaches();
        boolean wasEnabled = gen.config().enabled();
        long rev = gen.servedRevision();
        gen.updateConfig(configFromSettings());
        if (wasEnabled != gen.config().enabled() || rev != gen.servedRevision()) registerServed(server);
    }

    /** Parses servedQuests() with the mod's codec and hands them to the QuestManager, then syncs. */
    public static void registerServed(MinecraftServer server) {
        Map<ResourceLocation, Quest> map = new LinkedHashMap<>();
        if (gen != null) {
            gen.servedQuests().forEach((id, json) -> Quest.CODEC.parse(JsonOps.INSTANCE, json).result()
                .ifPresent(q -> map.put(new ResourceLocation(id), q)));
        }
        QuestManager.INSTANCE.setGeneratedQuests(map);
        QuestNetwork.syncAll(server);
    }

    /** Removes expired claims from the players (as if abandoned) and tells online players. */
    public static void applyExpired(MinecraftServer server, List<ExpiredClaim> expired) {
        WorldQuestStore store = WorldQuestStore.get();
        if (expired.isEmpty() || store == null) return;
        for (ExpiredClaim e : expired) {
            PlayerQuestData data = store.peek(e.holder());
            ResourceLocation id = new ResourceLocation(e.questId());
            if (data != null) data.abandon(id);
            ServerPlayer p = server.getPlayerList().getPlayer(e.holder());
            if (p != null) {
                Quest q = QuestManager.INSTANCE.get(id);
                // 1.20.1 has no clientInformation() on ServerPlayer; the tree uses English here as well
                String title = q == null ? e.questId() : q.title().get(LocalizedText.DEFAULT_LANG);
                p.sendSystemMessage(Component.literal("§eYour generated quest expired and was released: §f" + title));
                QuestNetwork.syncProgress(p);
            }
        }
        store.markDirty();
    }

    /**
     * Builds the config from settings.json. The getters below the two existing ones
     * (generatedQuests, generatedCount) are new WorldSettings fields — see INTEGRATION.md §4.
     */
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
            .disabledProfiles(Set.copyOf(WorldSettings.generatorDisabledProfiles()))
            .adaptiveBalancing(WorldSettings.generatorAdaptiveBalancing())
            .statsEnabled(WorldSettings.generatorStats())
            .build();
    }
}
