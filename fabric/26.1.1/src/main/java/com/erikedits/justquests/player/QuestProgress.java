package com.erikedits.justquests.player;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class QuestProgress {
    private final Map<Integer, Integer> objectiveProgress = new HashMap<>();
    /** Distinct objectives: the kinds (item, block or mob ids) already counted, per objective. */
    private final Map<Integer, Set<String>> seen = new HashMap<>();

    /**
     * Codec for JSON/NBT storage. Objective indices are stored as string
     * keys (JSON objects require string keys); converted back to ints.
     */
    public static final Codec<QuestProgress> CODEC =
        Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(
            QuestProgress::fromStringMap,
            QuestProgress::toStringMap
        );

    public int get(int objectiveIndex) {
        return objectiveProgress.getOrDefault(objectiveIndex, 0);
    }

    public void increment(int objectiveIndex, int amount) {
        objectiveProgress.merge(objectiveIndex, amount, Integer::sum);
    }

    /** Remembers a kind for a distinct objective; false if it was counted before. */
    public boolean see(int objectiveIndex, String kind) {
        return seen.computeIfAbsent(objectiveIndex, k -> new HashSet<>()).add(kind);
    }

    /** Whether anything has been counted yet. */
    public boolean hasProgress() {
        return !objectiveProgress.isEmpty() || !seen.isEmpty();
    }

    /** Back to zero (reset_on_death). */
    public void reset() {
        objectiveProgress.clear();
        seen.clear();
    }

    private static QuestProgress fromStringMap(Map<String, Integer> map) {
        QuestProgress qp = new QuestProgress();
        map.forEach((key, value) -> {
            try {
                // "2/minecraft:apple": a kind objective 2 (distinct) has counted
                int slash = key.indexOf('/');
                if (slash > 0) qp.see(Integer.parseInt(key.substring(0, slash)), key.substring(slash + 1));
                else qp.objectiveProgress.put(Integer.parseInt(key), value);
            } catch (NumberFormatException ignored) {
                // skip invalid keys
            }
        });
        return qp;
    }

    private Map<String, Integer> toStringMap() {
        Map<String, Integer> map = new HashMap<>();
        objectiveProgress.forEach((idx, count) -> map.put(String.valueOf(idx), count));
        seen.forEach((idx, kinds) -> kinds.forEach(kind -> map.put(idx + "/" + kind, 1)));
        return map;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        objectiveProgress.forEach((idx, count) -> tag.putInt(String.valueOf(idx), count));
        return tag;
    }

    public static QuestProgress fromNbt(CompoundTag tag) {
        QuestProgress qp = new QuestProgress();
        for (String key : tag.keySet()) {                      // getAllKeys() -> keySet() in 1.21.5
            try {
                qp.objectiveProgress.put(Integer.parseInt(key), tag.getIntOr(key, 0));
            } catch (NumberFormatException ignored) {
                // skip invalid keys
            }
        }
        return qp;
    }
}
