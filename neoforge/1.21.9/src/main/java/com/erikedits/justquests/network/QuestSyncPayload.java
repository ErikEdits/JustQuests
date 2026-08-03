package com.erikedits.justquests.network;

import com.erikedits.justquests.JustQuests;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client quest sync (Phase 3 multiplayer). Carries one JSON blob:
 * {@code { "quests": {id: questJson}, "progress": <PlayerQuestData json> }}.
 * Sent on join, on the player's progress change, and on quest reload/reroll so
 * the client quest book works on dedicated servers (it read data directly before,
 * which only worked in singleplayer).
 */
public record QuestSyncPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QuestSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JustQuests.MOD_ID, "quest_sync"));

    public static final StreamCodec<FriendlyByteBuf, QuestSyncPayload> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.stringUtf8(1048576), QuestSyncPayload::json, QuestSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<QuestSyncPayload> type() {
        return TYPE;
    }
}
