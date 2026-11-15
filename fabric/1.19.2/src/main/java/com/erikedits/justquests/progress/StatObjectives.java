package com.erikedits.justquests.progress;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.objective.CraftItemObjective;
import com.erikedits.justquests.data.objective.EnchantItemObjective;
import com.erikedits.justquests.data.objective.HaveItemObjective;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.objective.UseItemObjective;
import com.erikedits.justquests.storage.WorldQuestStore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.StonecutterMenu;
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
 *
 * <p>craft_item is counted by the crafting events, which don't fire for the stonecutter and the
 * smithing table. While one of them is open its results are read from the "Times Crafted"
 * statistic instead; that menu is the only thing that can raise it then. The anvil, grindstone,
 * loom and cartography table are watched by {@link Workstations}.
 */
public final class StatObjectives {
    private static final Object ENCHANT = new Object();

    /** Key for an item's "Times Crafted" statistic (watched only while a stonecutter or smithing table is open). */
    private record Crafted(Item item) {}
    /** Key for a statistic of the stat objective (0.6.0); unit raw points make one counted unit. */
    private record Custom(net.minecraft.resources.ResourceLocation stat, int unit) {}
    private static final Map<UUID, Map<Object, Integer>> SEEN = new HashMap<>();

    private StatObjectives() {}

    /** Every server tick, per player. */
    public static void tick(ServerPlayer player) {
        Workstations.tick(player);
        FarmersDelight.tick(player);   // #22: what its blocks did, cooking, rich soil
        boolean enchanting = player.containerMenu instanceof EnchantmentMenu;
        boolean cutting = player.containerMenu instanceof StonecutterMenu
            || player.containerMenu instanceof net.minecraft.world.inventory.SmithingMenu;
        boolean full = player.tickCount % 20 == 0;
        if (!enchanting && !cutting && !full) return;
        // have_item looks into the inventory, wherever the items came from (machines, chests, trades)
        if (full) QuestProgressService.advance(player, obj -> obj instanceof HaveItemObjective h ? h.held(player) : 0);

        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());
        // the player's own quests and their team's: a team quest counts every member's statistics
        Set<ResourceLocation> ids = new HashSet<>(com.erikedits.justquests.team.TeamQuests.activeIds(player));
        if (data != null) ids.addAll(data.active.keySet());
        Set<Object> watched = new HashSet<>();
        boolean structures = false, biomes = false;
        if (!ids.isEmpty()) {
            for (ResourceLocation id : ids) {
                Quest quest = QuestManager.INSTANCE.get(id);
                if (quest == null) continue;
                for (QuestObjective obj : quest.objectives()) {
                    if (obj instanceof EnchantItemObjective e && e.table()) watched.add(ENCHANT);
                    else if (obj instanceof UseItemObjective u) watched.addAll(u.item().items());
                    else if (obj instanceof com.erikedits.justquests.data.objective.StatObjective st) {
                        net.minecraft.resources.ResourceLocation own = registered(st.stat());
                        if (own != null) watched.add(new Custom(own, st.unit()));
                    }
                    else if (GoalEvents.structure(obj)) structures = true;
                    else if (GoalEvents.biome(obj)) biomes = true;
                    else if (cutting && obj instanceof CraftItemObjective c) {
                        for (Item item : c.item().items()) watched.add(new Crafted(item));
                    }
                }
            }
        }
        // find_structure and find_biome: where the player stands, once a second
        if (full && (structures || biomes)) GoalEvents.where(player, structures, biomes);
        if (watched.isEmpty()) {
            SEEN.remove(player.getUUID());
            return;
        }

        Map<Object, Integer> seen = SEEN.getOrDefault(player.getUUID(), Map.of());
        Map<Object, Integer> next = new HashMap<>();
        Map<Object, Integer> gained = new HashMap<>();
        for (Object key : watched) {
            if ((key instanceof Item || key instanceof Custom) && !full) {   // "used" statistics only once a second
                Integer old = seen.get(key);
                if (old != null) next.put(key, old);
                continue;
            }
            if (key instanceof Custom cu) {
                // whole units only (blocks, minutes): the rest waits for the next poll
                int now = player.getStats().getValue(Stats.CUSTOM.get(cu.stat()));
                Integer old = seen.get(key);
                int units = old == null || now <= old ? 0 : (now - old) / cu.unit();
                next.put(key, old == null || now < old ? now : old + units * cu.unit());
                if (units > 0) gained.put(key, units);
                continue;
            }
            int now = player.getStats().getValue(key == ENCHANT ? Stats.CUSTOM.get(Stats.ENCHANT_ITEM)
                : key instanceof Crafted c ? Stats.ITEM_CRAFTED.get(c.item()) : Stats.ITEM_USED.get((Item) key));
            Integer old = seen.get(key);
            next.put(key, now);
            if (old != null && now > old) gained.put(key, now - old);
        }
        SEEN.put(player.getUUID(), next);
        if (gained.isEmpty()) return;

        ItemStack enchanted = enchanting ? player.containerMenu.getSlot(0).getItem() : ItemStack.EMPTY;
        // one event per statistic that went up, so a distinct objective can count each kind once
        for (Map.Entry<Object, Integer> entry : gained.entrySet()) {
            Object key = entry.getKey();
            int n = entry.getValue();
            Item item = key instanceof Crafted made ? made.item() : key instanceof Item used ? used : null;
            String kind = item != null ? QuestProgressService.kind(new ItemStack(item))
                : enchanted.isEmpty() ? null : QuestProgressService.kind(enchanted);
            QuestProgressService.advance(player, kind, obj -> {
                if (key == ENCHANT && obj instanceof EnchantItemObjective e) return e.table() && e.accepts(enchanted) ? n : 0;
                if (key instanceof Item it && obj instanceof UseItemObjective u) return u.item().items().contains(it) ? n : 0;
                if (key instanceof Crafted cf && obj instanceof CraftItemObjective c) return c.matches(new ItemStack(cf.item())) ? n : 0;
                if (key instanceof Custom cu && obj instanceof com.erikedits.justquests.data.objective.StatObjective st) return st.stat().equals(cu.stat()) ? n : 0;
                return 0;
            });
        }
    }

    /** The game's own objects of the custom statistics, by id (a statistic is found by its object, not its id). */
    private static final Map<net.minecraft.resources.ResourceLocation, net.minecraft.resources.ResourceLocation> REGISTERED = new HashMap<>();

    /** The game's own object of a custom statistic id, or null when there is no such statistic. */
    private static net.minecraft.resources.ResourceLocation registered(net.minecraft.resources.ResourceLocation id) {
        if (REGISTERED.isEmpty()) {
            for (net.minecraft.resources.ResourceLocation own : net.minecraft.core.Registry.CUSTOM_STAT) REGISTERED.put(own, own);
        }
        return REGISTERED.get(id);
    }

    /** Server stop. */
    public static void clear() {
        SEEN.clear();
        Workstations.clear();
        GoalEvents.clear();
        FarmersDelight.clear();
    }
}
