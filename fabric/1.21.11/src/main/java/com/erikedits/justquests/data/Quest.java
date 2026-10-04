package com.erikedits.justquests.data;

import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record Quest(LocalizedText title, LocalizedText description, String category, QuestMode mode,
                    List<Identifier> requires, boolean repeatable, Optional<Integer> cooldownHours,
                    int sort, List<QuestObjective> objectives, List<QuestReward> rewards,
                    Optional<Identifier> icon) {
    public static final Codec<Quest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        LocalizedText.CODEC.fieldOf("title").forGetter(Quest::title),
        LocalizedText.CODEC.optionalFieldOf("description", LocalizedText.EMPTY).forGetter(Quest::description),
        Codec.STRING.optionalFieldOf("category", "datapack").forGetter(Quest::category),
        QuestMode.CODEC.optionalFieldOf("mode", QuestMode.ALL).forGetter(Quest::mode),
        // quest ids that must be completed before this one can be accepted (Q28)
        Identifier.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(Quest::requires),
        // can be done again after completion; optional cooldown in hours (Q26)
        Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(Quest::repeatable),
        Codec.INT.optionalFieldOf("cooldown_hours").forGetter(Quest::cooldownHours),
        // ordering within a category in /quest list (lower = first); default 0
        Codec.INT.optionalFieldOf("sort", 0).forGetter(Quest::sort),
        QuestObjective.CODEC.listOf().fieldOf("objectives").forGetter(Quest::objectives),
        QuestReward.CODEC.listOf().fieldOf("rewards").forGetter(Quest::rewards),
        // item shown in the quest book and the HUD; picked from the first objective when absent
        Identifier.CODEC.optionalFieldOf("icon").forGetter(Quest::icon)
    ).apply(instance, Quest::new));
}
