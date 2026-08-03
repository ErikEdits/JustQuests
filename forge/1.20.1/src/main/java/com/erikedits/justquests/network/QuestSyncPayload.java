package com.erikedits.justquests.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client quest sync (Phase 3 multiplayer). Carries one JSON blob:
 * {@code { "quests": {id: questJson}, "progress": <PlayerQuestData json> }}.
 * Forge 1.20.1 uses SimpleChannel, so this is a plain message with static
 * encode/decode/handle instead of a CustomPacketPayload.
 */
public record QuestSyncPayload(String json) {

    public static void encode(QuestSyncPayload msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.json, 1048576);
    }

    public static QuestSyncPayload decode(FriendlyByteBuf buf) {
        return new QuestSyncPayload(buf.readUtf(1048576));
    }

    public static void handle(QuestSyncPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientQuestData.accept(msg.json));
        ctx.get().setPacketHandled(true);
    }
}
