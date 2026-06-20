package com.erikedits.justquests.player;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerQuests implements INBTSerializable<CompoundTag> {
    private final Map<ResourceLocation, QuestProgress> active = new HashMap<>();
    private final Set<ResourceLocation> completed = new HashSet<>();

    public Map<ResourceLocation, QuestProgress> active() {
        return active;
    }

    public Set<ResourceLocation> completed() {
        return completed;
    }

    public boolean isActive(ResourceLocation id) {
        return active.containsKey(id);
    }

    public boolean isCompleted(ResourceLocation id) {
        return completed.contains(id);
    }

    public void accept(ResourceLocation id) {
        if (!completed.contains(id)) {
            active.putIfAbsent(id, new QuestProgress());
        }
    }

    public void abandon(ResourceLocation id) {
        active.remove(id);
    }

    public void complete(ResourceLocation id) {
        active.remove(id);
        completed.add(id);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        CompoundTag activeTag = new CompoundTag();
        active.forEach((id, progress) -> activeTag.put(id.toString(), progress.toNbt()));
        tag.put("active", activeTag);

        ListTag completedTag = new ListTag();
        completed.forEach(id -> completedTag.add(StringTag.valueOf(id.toString())));
        tag.put("completed", completedTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        active.clear();
        completed.clear();

        // 1.21.5 NBT API: getCompound/getList return Optional; use the *OrEmpty
        // helpers, and getAllKeys() was renamed to keySet().
        CompoundTag activeTag = tag.getCompoundOrEmpty("active");
        for (String key : activeTag.keySet()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                active.put(id, QuestProgress.fromNbt(activeTag.getCompoundOrEmpty(key)));
            }
        }

        ListTag completedTag = tag.getListOrEmpty("completed");
        for (int i = 0; i < completedTag.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(completedTag.getStringOr(i, ""));
            if (id != null) {
                completed.add(id);
            }
        }
    }
}
