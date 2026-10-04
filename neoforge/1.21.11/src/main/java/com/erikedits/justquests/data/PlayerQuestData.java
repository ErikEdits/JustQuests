package com.erikedits.justquests.data;

import com.erikedits.justquests.player.QuestProgress;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * All quest data for a single player. Stored per-world in progress.json
 * (see WorldQuestStore). Forward-compatible fields are reserved now so the
 * format never needs a migration later:
 *  - teamId: optional group id for future team-quest integration (Q14)
 *  - pendingClaim: finished quests whose rewards wait to be claimed (Claim
 *    button in the quest book or /quest claim): quest id -> completion time (Q48)
 *  - completed: quest id -> last completion timestamp (epoch millis), used
 *    for the 6-day no-repeat window and repeatable quests (Q26)
 */
public class PlayerQuestData {
    public Optional<String> teamId = Optional.empty();
    public final Map<Identifier, QuestProgress> active = new HashMap<>();
    public final Map<Identifier, Long> pendingClaim = new HashMap<>();
    public final Map<Identifier, Long> completed = new HashMap<>();

    public static final Codec<PlayerQuestData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("teamId").forGetter(d -> d.teamId),
        Codec.unboundedMap(Identifier.CODEC, QuestProgress.CODEC).optionalFieldOf("active", Map.of()).forGetter(d -> d.active),
        Codec.unboundedMap(Identifier.CODEC, Codec.LONG).optionalFieldOf("pendingClaim", Map.of()).forGetter(d -> d.pendingClaim),
        Codec.unboundedMap(Identifier.CODEC, Codec.LONG).optionalFieldOf("completed", Map.of()).forGetter(d -> d.completed)
    ).apply(instance, PlayerQuestData::new));

    public PlayerQuestData() {}

    public PlayerQuestData(Optional<String> teamId,
                           Map<Identifier, QuestProgress> active,
                           Map<Identifier, Long> pendingClaim,
                           Map<Identifier, Long> completed) {
        this.teamId = teamId;
        this.active.putAll(active);
        this.pendingClaim.putAll(pendingClaim);
        this.completed.putAll(completed);
    }

    public boolean isActive(Identifier id) {
        return active.containsKey(id);
    }

    public boolean isCompleted(Identifier id) {
        return completed.containsKey(id);
    }

    /** A finished quest whose rewards still wait to be claimed. */
    public boolean isClaimable(Identifier id) {
        return pendingClaim.containsKey(id);
    }

    /**
     * Adds the quest to active. Eligibility (already completed / not
     * repeatable / cooldown / prerequisites) is enforced by the caller
     * (QuestCommand.accept), which has the Quest definition; a re-accepted
     * repeatable quest keeps its old completion timestamp until re-completed.
     */
    public void accept(Identifier id) {
        active.putIfAbsent(id, new QuestProgress());
    }

    public void abandon(Identifier id) {
        active.remove(id);
    }

    /** Marks a quest finished: removes it from active and records the timestamp. */
    public void complete(Identifier id) {
        active.remove(id);
        completed.put(id, System.currentTimeMillis());
    }

    public boolean isEmpty() {
        return active.isEmpty() && pendingClaim.isEmpty() && completed.isEmpty();
    }
}
