package com.erikedits.justquests.data.reward;

import com.erikedits.justquests.data.LocalizedText;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Sends the player a chat message. The text can be a string or a per-language map. */
public record MessageReward(LocalizedText message) implements QuestReward {
    public static final String TYPE_ID = "justquests:message";

    public static final MapCodec<MessageReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        LocalizedText.CODEC.fieldOf("message").forGetter(MessageReward::message)
    ).apply(instance, MessageReward::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void grant(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(message.get(com.erikedits.justquests.data.LocalizedText.DEFAULT_LANG)));
    }

    @Override
    public String displayName() {
        return "Message";
    }

    @Override
    public Component display() {
        return Component.literal("Message");
    }
}
