package com.erikedits.justquests.data;

import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record Quest(LocalizedText title, LocalizedText description, String category, QuestMode mode,
                    List<ResourceLocation> requires, boolean repeatable, Optional<Integer> cooldownHours,
                    List<QuestObjective> objectives, List<QuestReward> rewards) {
    public static final Codec<Quest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        LocalizedText.CODEC.fieldOf("title").forGetter(Quest::title),
        LocalizedText.CODEC.optionalFieldOf("description", LocalizedText.EMPTY).forGetter(Quest::description),
        Codec.STRING.optionalFieldOf("category", "datapack").forGetter(Quest::category),
        QuestMode.CODEC.optionalFieldOf("mode", QuestMode.ALL).forGetter(Quest::mode),
        // quest ids that must be completed before this one can be accepted (Q28)
        ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(Quest::requires),
        // can be done again after completion; optional cooldown in hours (Q26)
        Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(Quest::repeatable),
        Codec.INT.optionalFieldOf("cooldown_hours").forGetter(Quest::cooldownHours),
        QuestObjective.CODEC.listOf().fieldOf("objectives").forGetter(Quest::objectives),
        QuestReward.CODEC.listOf().fieldOf("rewards").forGetter(Quest::rewards)
    ).apply(instance, Quest::new));
}
