package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Hand in X of an item (or any item in a tag), 0.6.0: the quest book's Hand in link or
 * /quest submit takes them out of the inventory. Handing in part of them counts too.
 */
public record SubmitItemObjective(ItemMatcher item, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:submit_item";

    public static final MapCodec<SubmitItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.fieldOf("item").forGetter(SubmitItemObjective::item),
        Codec.INT.fieldOf("count").forGetter(SubmitItemObjective::count)
    ).apply(instance, SubmitItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** How many matching items the player has in the whole inventory (also asked by the quest book). */
    public int held(Player player) {
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && item.matches(stack)) total += stack.getCount();
        }
        return total;
    }

    /** Takes up to n matching items out of the inventory; returns how many it took. */
    public int take(Player player, int n) {
        Inventory inventory = player.getInventory();
        int taken = 0;
        for (int i = 0; i < inventory.getContainerSize() && taken < n; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !item.matches(stack)) continue;
            int k = Math.min(stack.getCount(), n - taken);
            stack.shrink(k);
            if (stack.isEmpty()) inventory.setItem(i, ItemStack.EMPTY);
            taken += k;
        }
        if (taken > 0) inventory.setChanged();
        return taken;
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Hand in " + count + "x " + item.label();
    }

    @Override
    public Component display() {
        return Msg.tr("justquests.goal.submit_count", count, item.name());
    }

    @Override
    public Component goal() {
        return Msg.tr("justquests.goal.submit", item.name());
    }
}
