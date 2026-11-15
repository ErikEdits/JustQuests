package com.erikedits.justquests.data.reward;

import com.erikedits.justquests.badge.Badges;
import com.erikedits.justquests.data.Badge;
import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Gives a badge (0.6.0): { "type": "justquests:badge", "badge": "mypack:chapter_1" }. Once per player. */
public record BadgeReward(ResourceLocation badge) implements QuestReward {
    public static final String TYPE_ID = "justquests:badge";

    public static final MapCodec<BadgeReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("badge").forGetter(BadgeReward::badge)
    ).apply(instance, BadgeReward::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void grant(ServerPlayer player) {
        Badges.give(player, badge);
    }

    @Override
    public String displayName() {
        Badge b = Badges.get(badge);
        return "Badge " + (b != null ? b.name().getDefault() : badge.toString());
    }

    @Override
    public Component display() {
        Badge b = Badges.get(badge);
        return Msg.tr("justquests.reward.badge", b != null ? b.name().getDefault() : badge.toString());
    }
}
