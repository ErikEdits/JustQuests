package com.erikedits.justquests.data.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record GiveItemReward(Item item, int count) implements QuestReward {
    public static final String TYPE_ID = "justquests:give_item";

    public static final MapCodec<GiveItemReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(GiveItemReward::item),
        Codec.INT.fieldOf("count").forGetter(GiveItemReward::count)
    ).apply(instance, GiveItemReward::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void grant(ServerPlayer player) {
        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public String displayName() {
        return count + "x " + BuiltInRegistries.ITEM.getKey(item);
    }
}
