package com.erikedits.justquests.data.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Gives the player experience points. */
public record XpReward(int amount) implements QuestReward {
    public static final String TYPE_ID = "justquests:xp";

    public static final MapCodec<XpReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("amount").forGetter(XpReward::amount)
    ).apply(instance, XpReward::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void grant(ServerPlayer player) {
        player.giveExperiencePoints(amount);
    }

    @Override
    public String displayName() {
        return amount + " XP";
    }

    @Override
    public Component display() {
        return new net.minecraft.network.chat.TextComponent(displayName());
    }
}
