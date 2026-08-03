package com.erikedits.justquests.network;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server -> client quest sync so the quest book works on dedicated servers.
 * The whole quest list + the player's progress are serialized to JSON (reusing
 * the existing codecs) and sent as one payload. Fires on join, on that player's
 * progress change, and on quest reload/reroll. This MC version predates the
 * CustomPayload API, so it uses the raw channel + FriendlyByteBuf form; the
 * client receiver lives in the client initializer.
 */
public final class QuestNetwork {
    public static final ResourceLocation CHANNEL =
        new ResourceLocation(JustQuests.MOD_ID, "quest_sync");
    private static final Gson GSON = new Gson();

    private QuestNetwork() {}

    /** No payload type to register on this version (pre-CustomPayload API). */
    public static void register() {}

    /** Send the full quest list + this player's progress to one player. */
    public static void syncPlayer(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(buildJson(player), 1048576);
        ServerPlayNetworking.send(player, CHANNEL, buf);
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
