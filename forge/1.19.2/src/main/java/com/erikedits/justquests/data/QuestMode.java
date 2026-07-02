package com.erikedits.justquests.data;

import com.mojang.serialization.Codec;

/** Whether a quest needs ALL objectives done, or ANY one of them (Q40). */
public enum QuestMode {
    ALL,
    ANY;

    public static final Codec<QuestMode> CODEC = Codec.STRING.xmap(
        s -> "any".equalsIgnoreCase(s) ? ANY : ALL,
        m -> m == ANY ? "any" : "all"
    );
}
