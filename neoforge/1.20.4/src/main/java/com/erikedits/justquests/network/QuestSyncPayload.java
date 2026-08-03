package com.erikedits.justquests.network;

import com.erikedits.justquests.JustQuests;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client quest sync (Phase 3 multiplayer). Carries one JSON blob:
 * {@code { "quests": {id: questJson}, "progress": <PlayerQuestData json> }}.
 * 1.20.4 predates the StreamCodec payload API, so this uses the older
 * {@code write(FriendlyByteBuf)} / {@code id()} form.
 */
public record QuestSyncPayload(String json) implements CustomPacketPayload {
    public static final ResourceLocation ID =
        new ResourceLocation(JustQuests.MOD_ID, "quest_sync");

    public QuestSyncPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(1048576));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(json, 1048576);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
