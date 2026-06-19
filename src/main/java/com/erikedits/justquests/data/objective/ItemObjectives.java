package com.erikedits.justquests.data.objective;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/** Shared codec for the item field of item objectives. */
public final class ItemObjectives {
    private ItemObjectives() {}

    /**
     * Accepts a single item id (`minecraft:oak_log`), a list of ids, or a
     * tag (`#minecraft:logs`). homogeneousList alone rejects a bare single
     * id, so a single id is handled by the left branch and wrapped in a
     * direct HolderSet.
     */
    public static final Codec<HolderSet<Item>> ITEM_OR_TAG = Codec.either(
        BuiltInRegistries.ITEM.holderByNameCodec(),
        RegistryCodecs.homogeneousList(Registries.ITEM)
    ).xmap(
        either -> either.map(holder -> HolderSet.direct(holder), set -> set),
        set -> (set.size() == 1 && set.unwrapKey().isEmpty())
            ? Either.left(set.get(0))
            : Either.right(set)
    );
}
