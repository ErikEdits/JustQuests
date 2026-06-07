package com.erikedits.justquests.player;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class QuestProgress {
    private final Map<Integer, Integer> objectiveProgress = new HashMap<>();

    public int get(int objectiveIndex) {
        return objectiveProgress.getOrDefault(objectiveIndex, 0);
    }

    public void increment(int objectiveIndex, int amount) {
        objectiveProgress.merge(objectiveIndex, amount, Integer::sum);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        objectiveProgress.forEach((idx, count) -> tag.putInt(String.valueOf(idx), count));
        return tag;
    }

    public static QuestProgress fromNbt(CompoundTag tag) {
        QuestProgress qp = new QuestProgress();
        for (String key : tag.getAllKeys()) {
            try {
                qp.objectiveProgress.put(Integer.parseInt(key), tag.getInt(key));
            } catch (NumberFormatException ignored) {
                // skip invalid keys
            }
        }
        return qp;
    }
}
