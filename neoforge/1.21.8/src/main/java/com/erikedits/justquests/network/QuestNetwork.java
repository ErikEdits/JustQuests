package com.erikedits.justquests.network;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Server -> client quest sync so the quest book works on dedicated servers.
 * The whole quest list + the player's progress are serialized to JSON (reusing
 * the existing codecs) and sent as one payload. Fires on join, on that player's
 * progress change, and on quest reload/reroll.
 */
public final class QuestNetwork {
    private static final Gson GSON = new Gson();

    private QuestNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(QuestSyncPayload.TYPE, QuestSyncPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> ClientQuestData.accept(payload.json())));
    }

    /** Send the full quest list + this player's progress to one player. */
    public static void syncPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new QuestSyncPayload(buildJson(player)));
    }

    /** Resend to everyone (e.g. after a quest reload/reroll changes the list). */
    public static void syncAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) syncPlayer(p);
    }

    private static String buildJson(ServerPlayer player) {
        JsonObject root = new JsonObject();
        JsonObject quests = new JsonObject();
        QuestManager.INSTANCE.getQuests().forEach((id, quest) ->
            Quest.CODEC.encodeStart(JsonOps.INSTANCE, quest).result()
                .ifPresent(j -> quests.add(id.toString(), j)));
        root.add("quests", quests);
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store != null ? store.peek(player.getUUID()) : null;
        if (data != null) {
            PlayerQuestData.CODEC.encodeStart(JsonOps.INSTANCE, data).result()
                .ifPresent(j -> root.add("progress", j));
        }
        return GSON.toJson(root);
    }
}
