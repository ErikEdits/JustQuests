package com.erikedits.justquests.data.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/** Applies a potion effect to the player (duration in seconds, amplifier 0 = level I). */
public record EffectReward(Holder<MobEffect> effect, int seconds, int amplifier) implements QuestReward {
    public static final String TYPE_ID = "justquests:effect";

    public static final MapCodec<EffectReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(EffectReward::effect),
        Codec.INT.optionalFieldOf("seconds", 30).forGetter(EffectReward::seconds),
        Codec.INT.optionalFieldOf("amplifier", 0).forGetter(EffectReward::amplifier)
    ).apply(instance, EffectReward::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void grant(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(effect, seconds * 20, amplifier));
    }

    @Override
    public String displayName() {
        return "Effect " + effect.getRegisteredName() + " (" + seconds + "s)";
    }

    @Override
    public Component display() {
        return Component.literal("Effect ").append(effect.value().getDisplayName())
            .append(Component.literal(" (" + seconds + "s)"));
    }
}
