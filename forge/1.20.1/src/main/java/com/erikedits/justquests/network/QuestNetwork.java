package com.erikedits.justquests.network;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Server -> client quest sync so the quest book works on dedicated servers.
 * The whole quest list + the player's progress are serialized to JSON (reusing
 * the existing codecs) and sent as one payload. Fires on join, on that player's
 * progress change, and on quest reload/reroll. Forge 1.20.1 uses SimpleChannel.
 */
public final class QuestNetwork {
    private static final String PROTOCOL = "1";
    private static final Gson GSON = new Gson();
    private static SimpleChannel channel;

    private QuestNetwork() {}

    /** Called from the mod constructor to create the channel + register the message. */
    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JustQuests.MOD_ID, "quest_sync"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        channel.registerMessage(0, QuestSyncPayload.class,
            QuestSyncPayload::encode, QuestSyncPayload::decode, QuestSyncPayload::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Send the full quest list + this player's progress to one player. */
    public static void syncPlayer(ServerPlayer player) {
        if (channel == null) return;
        channel.send(PacketDistributor.PLAYER.with(() -> player),
            new QuestSyncPayload(buildJson(player)));
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
