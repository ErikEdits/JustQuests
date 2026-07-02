package com.erikedits.justquests.data.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record GiveItemReward(Item item, int count) implements QuestReward {
    public static final String TYPE_ID = "justquests:give_item";

    public static final MapCodec<GiveItemReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Registry.ITEM.byNameCodec().fieldOf("item").forGetter(GiveItemReward::item),
        Codec.INT.fieldOf("count").forGetter(GiveItemReward::count)
    ).apply(instance, GiveItemReward::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void grant(ServerPlayer player) {
        // Split into proper max-size stacks so large rewards (e.g. from
        // loot tables) are handed out correctly instead of as one
        // oversized stack. Anything that doesn't fit is dropped.
        int max = new ItemStack(item).getMaxStackSize();
        int remaining = count;
        while (remaining > 0) {
            int n = Math.min(remaining, max);
            ItemStack stack = new ItemStack(item, n);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            remaining -= n;
        }
    }

    @Override
    public String displayName() {
        return count + "x " + Registry.ITEM.getKey(item);
    }

    @Override
    public Component display() {
        return new net.minecraft.network.chat.TextComponent(count + "x ").append(new ItemStack(item).getHoverName());
    }
}
