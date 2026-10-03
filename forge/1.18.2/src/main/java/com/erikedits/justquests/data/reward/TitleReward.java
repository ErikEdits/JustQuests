package com.erikedits.justquests.data.reward;

import com.erikedits.justquests.data.LocalizedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Shows the player a big on-screen title, like /title. title and subtitle can be a string or a
 * per-language map; fade_in, stay and fade_out are in ticks (vanilla defaults 10, 70, 20).
 */
public record TitleReward(LocalizedText title, Optional<LocalizedText> subtitle, int fadeIn, int stay, int fadeOut)
        implements QuestReward {
    public static final String TYPE_ID = "justquests:title";

    public static final MapCodec<TitleReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        LocalizedText.CODEC.fieldOf("title").forGetter(TitleReward::title),
        LocalizedText.CODEC.optionalFieldOf("subtitle").forGetter(TitleReward::subtitle),
        Codec.INT.optionalFieldOf("fade_in", 10).forGetter(TitleReward::fadeIn),
        Codec.INT.optionalFieldOf("stay", 70).forGetter(TitleReward::stay),
        Codec.INT.optionalFieldOf("fade_out", 20).forGetter(TitleReward::fadeOut)
    ).apply(instance, TitleReward::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void grant(ServerPlayer player) {
        String lang = LocalizedText.DEFAULT_LANG;
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        subtitle.ifPresent(s -> player.connection.send(new ClientboundSetSubtitleTextPacket(new net.minecraft.network.chat.TextComponent(s.get(lang)))));
        player.connection.send(new ClientboundSetTitleTextPacket(new net.minecraft.network.chat.TextComponent(title.get(lang))));
    }

    @Override
    public String displayName() {
        return "Title: " + title.getDefault();
    }

    @Override
    public Component display() {
        return new net.minecraft.network.chat.TextComponent("Title: " + title.getDefault());
    }
}
