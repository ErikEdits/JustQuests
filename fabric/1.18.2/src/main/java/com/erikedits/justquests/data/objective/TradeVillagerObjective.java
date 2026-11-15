package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Trade X times with a villager or the wandering trader (0.6.0). "item" checks what the player gets,
 * "profession" who trades: a villager profession like minecraft:librarian, or minecraft:wandering_trader.
 */
public record TradeVillagerObjective(Optional<ItemMatcher> item, Optional<String> profession, int count, boolean distinct)
        implements QuestObjective {
    public static final String TYPE_ID = "justquests:trade_villager";
    public static final String WANDERING_TRADER = "minecraft:wandering_trader";

    public static final MapCodec<TradeVillagerObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(TradeVillagerObjective::item),
        Codec.STRING.xmap(TradeVillagerObjective::ns, s -> s).optionalFieldOf("profession").forGetter(TradeVillagerObjective::profession),
        Codec.INT.fieldOf("count").forGetter(TradeVillagerObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(TradeVillagerObjective::distinct)
    ).apply(instance, TradeVillagerObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** A trade that gave the player bought, with a trader of that profession (or minecraft:wandering_trader). */
    public boolean accepts(ItemStack bought, String trader) {
        return (item.isEmpty() || item.get().matches(bought)) && (profession.isEmpty() || profession.get().equals(trader));
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Trade " + count + "x " + item.map(ItemMatcher::label).orElse("anything") + profession.map(p -> " with " + p).orElse("");
    }

    /** "Librarian", "Wandering Trader": the game's own names. */
    private Component trader() {
        String p = profession.orElse(WANDERING_TRADER);
        if (p.equals(WANDERING_TRADER)) return new net.minecraft.network.chat.TranslatableComponent("entity.minecraft.wandering_trader");
        return new net.minecraft.network.chat.TranslatableComponent("entity.minecraft.villager." + p.substring(p.indexOf(':') + 1));
    }

    @Override
    public Component display() {
        if (profession.isPresent()) {
            if (item.isPresent()) return Msg.tr("justquests.goal.trade_with_count", count, item.get().name(), trader());
            return Msg.tr(count == 1 ? "justquests.goal.trade_with_one" : "justquests.goal.trade_with_many", trader(), count);
        }
        if (item.isPresent()) return Msg.tr("justquests.goal.trade_count", count, item.get().name());
        return Msg.tr(count == 1 ? "justquests.goal.trade_one" : "justquests.goal.trade_many", count);
    }

    @Override
    public Component goal() {
        if (profession.isPresent()) {
            return item.isPresent() ? Msg.tr("justquests.goal.trade_with", item.get().name(), trader())
                : Msg.tr("justquests.goal.trade_with_any", trader());
        }
        return item.isPresent() ? Msg.tr("justquests.goal.trade", item.get().name()) : Msg.tr("justquests.goal.trade_any");
    }

    private static String ns(String id) {
        return id.indexOf(':') >= 0 ? id : "minecraft:" + id;
    }
}
