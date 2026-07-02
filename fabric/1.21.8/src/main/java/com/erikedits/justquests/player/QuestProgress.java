package com.erikedits.justquests.player;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class QuestProgress {
    private final Map<Integer, Integer> objectiveProgress = new HashMap<>();

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

    private static QuestProgress fromStringMap(Map<String, Integer> map) {
        QuestProgress qp = new QuestProgress();
        map.forEach((key, value) -> {
            try {
                qp.objectiveProgress.put(Integer.parseInt(key), value);
            } catch (NumberFormatException ignored) {
                // skip invalid keys
            }
        });
        return qp;
    }

    private Map<String, Integer> toStringMap() {
        Map<String, Integer> map = new HashMap<>();
        objectiveProgress.forEach((idx, count) -> map.put(String.valueOf(idx), count));
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
