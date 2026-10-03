package com.erikedits.justquests.progress;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.objective.EnchantItemObjective;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.objective.UseItemObjective;
import com.erikedits.justquests.storage.WorldQuestStore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * use_item and enchant_item count vanilla statistics ("Times Used" per item, "Items Enchanted"),
 * so they mean exactly what the statistics screen shows and need no loader event or mixin.
 * Polled once a second per player, and every tick while an enchanting table is open so the
 * enchanted item can still be read from the table for an item filter. Only the change since the
 * last poll counts; a statistic is baselined the first time an active quest needs it, and
 * forgotten as soon as none does.
 */
public final class StatObjectives {
    private static final Object ENCHANT = new Object();
    private static final Map<UUID, Map<Object, Integer>> SEEN = new HashMap<>();

    private StatObjectives() {}

    /** Every server tick, per player. */
    public static void tick(ServerPlayer player) {
        boolean enchanting = player.containerMenu instanceof EnchantmentMenu;
        boolean full = player.tickCount % 20 == 0;
        if (!enchanting && !full) return;

        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());
        Set<Object> watched = new HashSet<>();
        if (data != null) {
            for (ResourceLocation id : data.active.keySet()) {
                Quest quest = QuestManager.INSTANCE.get(id);
                if (quest == null) continue;
                for (QuestObjective obj : quest.objectives()) {
                    if (obj instanceof EnchantItemObjective) watched.add(ENCHANT);
                    else if (obj instanceof UseItemObjective u) watched.addAll(u.item().items());
                }
            }
        }
        if (watched.isEmpty()) {
            SEEN.remove(player.getUUID());
            return;
        }

        Map<Object, Integer> seen = SEEN.getOrDefault(player.getUUID(), Map.of());
        Map<Object, Integer> next = new HashMap<>();
        Map<Object, Integer> gained = new HashMap<>();
        for (Object key : watched) {
            if (key != ENCHANT && !full) {   // item statistics only once a second
                Integer old = seen.get(key);
                if (old != null) next.put(key, old);
                continue;
            }
            int now = player.getStats().getValue(key == ENCHANT
                ? Stats.CUSTOM.get(Stats.ENCHANT_ITEM) : Stats.ITEM_USED.get((Item) key));
            Integer old = seen.get(key);
            next.put(key, now);
            if (old != null && now > old) gained.put(key, now - old);
        }
        SEEN.put(player.getUUID(), next);
        if (gained.isEmpty()) return;

        ItemStack enchanted = enchanting ? player.containerMenu.getSlot(0).getItem() : ItemStack.EMPTY;
        QuestProgressService.advance(player, obj -> {
            if (obj instanceof EnchantItemObjective e) {
                return e.accepts(enchanted) ? gained.getOrDefault(ENCHANT, 0) : 0;
            }
            if (obj instanceof UseItemObjective u) {
                int sum = 0;
                for (Item item : u.item().items()) sum += gained.getOrDefault(item, 0);
                return sum;
            }
            return 0;
        });
    }

    /** Server stop. */
    public static void clear() {
        SEEN.clear();
    }
}
