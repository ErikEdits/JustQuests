package com.erikedits.justquests.data;

import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record Quest(String title, String description, String category, QuestMode mode,
                    List<QuestObjective> objectives, List<QuestReward> rewards) {
    public static final Codec<Quest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("title").forGetter(Quest::title),
        Codec.STRING.optionalFieldOf("description", "").forGetter(Quest::description),
        Codec.STRING.optionalFieldOf("category", "datapack").forGetter(Quest::category),
        QuestMode.CODEC.optionalFieldOf("mode", QuestMode.ALL).forGetter(Quest::mode),
        QuestObjective.CODEC.listOf().fieldOf("objectives").forGetter(Quest::objectives),
        QuestReward.CODEC.listOf().fieldOf("rewards").forGetter(Quest::rewards)
    ).apply(instance, Quest::new));
}
