package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

/**
 * Take X results out of a grindstone (#25): taking enchantments off, or repairing two items into one.
 * "item" checks what comes out, "disenchant": true counts only when enchantments came off.
 */
public record GrindstoneItemObjective(Optional<ItemMatcher> item, boolean disenchant, int count, boolean distinct)
        implements QuestObjective {
    public static final String TYPE_ID = "justquests:grindstone_item";

    public static final MapCodec<GrindstoneItemObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(GrindstoneItemObjective::item),
        Codec.BOOL.optionalFieldOf("disenchant", false).forGetter(GrindstoneItemObjective::disenchant),
        Codec.INT.fieldOf("count").forGetter(GrindstoneItemObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(GrindstoneItemObjective::distinct)
    ).apply(instance, GrindstoneItemObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** Whether taking result out of a grindstone that held top and bottom counts. */
    public boolean accepts(ItemStack result, ItemStack top, ItemStack bottom) {
        return (item.isEmpty() || item.get().matches(result)) && (!disenchant || removed(result, top, bottom));
    }

    /** Whether an enchantment of the items put in is gone (or lower) on the result. */
    public static boolean removed(ItemStack result, ItemStack top, ItemStack bottom) {
        Map<String, Integer> after = ItemExtras.enchantments(result);
        for (ItemStack in : new ItemStack[] {top, bottom}) {
            if (in.isEmpty()) continue;
            for (Map.Entry<String, Integer> e : ItemExtras.enchantments(in).entrySet()) {
                if (after.getOrDefault(e.getKey(), 0) < e.getValue()) return true;
            }
        }
        return false;
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return (disenchant ? "Disenchant " : "Grindstone ") + count + "x " + item.map(ItemMatcher::label).orElse("any item");
    }

    @Override
    public Component display() {
        if (item.isPresent()) {
            return Msg.tr(disenchant ? "justquests.goal.disenchant_count" : "justquests.goal.grindstone_count", count, item.get().name());
        }
        String key = disenchant ? "justquests.goal.disenchant" : "justquests.goal.grindstone";
        return Msg.tr(key + (count == 1 ? "_one" : "_many"), count);
    }

    @Override
    public Component goal() {
        if (item.isPresent()) return Msg.tr(disenchant ? "justquests.goal.disenchant" : "justquests.goal.grindstone", item.get().name());
        return Msg.tr(disenchant ? "justquests.goal.disenchant_any" : "justquests.goal.grindstone_any");
    }
}
