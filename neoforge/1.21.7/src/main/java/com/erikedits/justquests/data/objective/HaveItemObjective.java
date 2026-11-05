package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Hold X of an item (or any item in a tag) in the inventory, wherever it came from: a machine of
 * another mod, a chest, a trade. The most the player has held at once counts, so moving items in
 * and out does not add up, and the progress does not go back when they are used.
 */
public record HaveItemObjective(ItemMatcher item, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:have_item";

    public static final MapCodec<HaveItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.fieldOf("item").forGetter(HaveItemObjective::item),
        Codec.INT.fieldOf("count").forGetter(HaveItemObjective::count)
    ).apply(instance, HaveItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** How many matching items the player has in the whole inventory right now. */
    public int held(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && item.matches(stack)) total += stack.getCount();
        }
        return total;
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Have " + count + "x " + item.label();
    }

    @Override
    public Component display() {
        return Msg.tr("justquests.goal.have_count", count, item.name());
    }
}
