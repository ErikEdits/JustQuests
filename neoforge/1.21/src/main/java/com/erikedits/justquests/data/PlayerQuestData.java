package com.erikedits.justquests.data;

import com.erikedits.justquests.player.QuestProgress;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * All quest data for a single player. Stored per-world in progress.json
 * (see WorldQuestStore). Forward-compatible fields are reserved now so the
 * format never needs a migration later:
 *  - teamId: optional group id for future team-quest integration (Q14)
 *  - pendingClaim: completed-but-unclaimed rewards for the future GUI claim
 *    flow (Q48) — unused in v0.1 (rewards are still instant)
 *  - completed: quest id -> last completion timestamp (epoch millis), used
 *    for the 6-day no-repeat window and repeatable quests (Q26)
 */
public class PlayerQuestData {
    public Optional<String> teamId = Optional.empty();
    public final Map<ResourceLocation, QuestProgress> active = new HashMap<>();
    public final Map<ResourceLocation, Long> pendingClaim = new HashMap<>();
    public final Map<ResourceLocation, Long> completed = new HashMap<>();

    public static final Codec<PlayerQuestData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("teamId").forGetter(d -> d.teamId),
        Codec.unboundedMap(ResourceLocation.CODEC, QuestProgress.CODEC).optionalFieldOf("active", Map.of()).forGetter(d -> d.active),
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).optionalFieldOf("pendingClaim", Map.of()).forGetter(d -> d.pendingClaim),
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).optionalFieldOf("completed", Map.of()).forGetter(d -> d.completed)
    ).apply(instance, PlayerQuestData::new));

    public PlayerQuestData() {}

    public PlayerQuestData(Optional<String> teamId,
                           Map<ResourceLocation, QuestProgress> active,
                           Map<ResourceLocation, Long> pendingClaim,
                           Map<ResourceLocation, Long> completed) {
        this.teamId = teamId;
        this.active.putAll(active);
        this.pendingClaim.putAll(pendingClaim);
        this.completed.putAll(completed);
    }

    public boolean isActive(ResourceLocation id) {
        return active.containsKey(id);
    }

    public boolean isCompleted(ResourceLocation id) {
        return completed.containsKey(id);
    }

    public void accept(ResourceLocation id) {
        if (!completed.containsKey(id)) {
            active.putIfAbsent(id, new QuestProgress());
        }
    }

    public void abandon(ResourceLocation id) {
        active.remove(id);
    }

    /** Marks a quest finished: removes it from active and records the timestamp. */
    public void complete(ResourceLocation id) {
        active.remove(id);
        completed.put(id, System.currentTimeMillis());
    }

    public boolean isEmpty() {
        return active.isEmpty() && pendingClaim.isEmpty() && completed.isEmpty();
    }
}
