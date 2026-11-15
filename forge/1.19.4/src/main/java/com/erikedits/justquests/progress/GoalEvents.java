package com.erikedits.justquests.progress;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.objective.BrewPotionObjective;
import com.erikedits.justquests.data.objective.FindBiomeObjective;
import com.erikedits.justquests.data.objective.FindStructureObjective;
import com.erikedits.justquests.data.objective.FishItemObjective;
import com.erikedits.justquests.data.objective.InteractEntityObjective;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.objective.SubmitItemObjective;
import com.erikedits.justquests.data.objective.TradeVillagerObjective;
import com.erikedits.justquests.data.objective.UseBlockObjective;
import com.erikedits.justquests.player.QuestProgress;
import com.erikedits.justquests.storage.WorldQuestStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The objective types of 0.6.0, the same on every loader: the loader's event or mixin calls one of
 * these (fishing, trading, brewing, right-clicks), the tick looks where the player stands (structures,
 * biomes; statistics are counted in {@link StatObjectives}), and /quest submit hands items in.
 */
public final class GoalEvents {
    /** The last mob or block each player right-clicked, and when: the same one counts again after a second. */
    private static final Map<UUID, Object[]> LAST = new HashMap<>();

    private GoalEvents() {}

    /** fish_item: what a fishing rod pulled in. */
    public static void fished(ServerPlayer player, Collection<ItemStack> caught) {
        for (ItemStack stack : caught) {
            if (stack.isEmpty()) continue;
            int n = stack.getCount();
            QuestProgressService.advance(player, QuestProgressService.kind(stack), obj ->
                obj instanceof FishItemObjective f && f.accepts(stack) ? n : 0);
        }
    }

    /** trade_villager: a trade that gave the player bought. With an item the items count, else the trades. */
    public static void traded(ServerPlayer player, AbstractVillager trader, ItemStack bought) {
        if (bought.isEmpty()) return;
        String who = profession(trader);
        int n = bought.getCount();
        QuestProgressService.advance(player, QuestProgressService.kind(bought), obj -> {
            if (!(obj instanceof TradeVillagerObjective t) || !t.accepts(bought, who)) return 0;
            return t.item().isPresent() ? n : 1;
        });
    }

    /** A villager's profession (minecraft:librarian), else the trader's own id (minecraft:wandering_trader). */
    static String profession(AbstractVillager trader) {
        if (trader instanceof Villager v) {
            ResourceLocation id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession());
            if (id != null) return id.toString();
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(trader.getType()).toString();
    }

    /** brew_potion: a potion taken out of a brewing stand. */
    public static void brewed(ServerPlayer player, ItemStack potion) {
        if (potion.isEmpty()) return;
        String which = BrewPotionObjective.potionOf(potion);
        int n = potion.getCount();
        QuestProgressService.advance(player, which != null ? which : QuestProgressService.kind(potion), obj ->
            obj instanceof BrewPotionObjective b && b.accepts(potion) ? n : 0);
    }

    /** interact_entity: a right-click on a mob with the main hand. */
    public static void interacted(ServerPlayer player, Entity target, ItemStack held) {
        if (again(player, target.getUUID())) return;
        EntityType<?> type = target.getType();
        QuestProgressService.advance(player, QuestProgressService.kind(type), obj ->
            obj instanceof InteractEntityObjective i && i.accepts(type, held) ? 1 : 0);
    }

    /** use_block: a right-click on a block with the main hand. */
    public static void usedBlock(ServerPlayer player, BlockPos pos, Block block, ItemStack held) {
        if (again(player, pos.immutable())) return;
        QuestProgressService.advance(player, QuestProgressService.kind(block), obj ->
            obj instanceof UseBlockObjective u && u.accepts(block, held) ? 1 : 0);
    }

    /** Whether the player clicked the same mob or block less than a second ago (holding the button repeats clicks). */
    private static boolean again(ServerPlayer player, Object what) {
        long now = player.level.getGameTime();
        Object[] last = LAST.get(player.getUUID());
        if (last != null && last[0].equals(what) && now - (Long) last[1] < 20) return true;
        LAST.put(player.getUUID(), new Object[] {what, now});
        return false;
    }

    /**
     * find_structure and find_biome, once a second while an active quest asks for one: where the
     * player stands. A structure counts by its id and where it starts, so three villages are three.
     */
    static void where(ServerPlayer player, boolean structures, boolean biomes) {
        ServerLevel level = (ServerLevel) player.level;
        BlockPos pos = player.blockPosition();
        if (biomes) {
            Holder<Biome> biome = level.getBiome(pos);
            biome.unwrapKey().ifPresent(key -> {
                ResourceLocation id = key.location();
                QuestProgressService.advance(player, id.toString(), obj ->
                    obj instanceof FindBiomeObjective f && f.accepts(biome, id) ? 1 : 0);
            });
        }
        if (structures) {
            Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            for (Structure structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
                StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
                if (start == null || !start.isValid()) continue;
                ResourceLocation id = registry.getKey(structure);
                if (id == null) continue;
                Holder<Structure> holder = registry.wrapAsHolder(structure);
                String kind = id + "@" + start.getChunkPos().x + "," + start.getChunkPos().z;
                QuestProgressService.advance(player, kind, obj ->
                    obj instanceof FindStructureObjective f && f.accepts(holder, id) ? 1 : 0);
            }
        }
    }

    /** Whether an objective is looked for by {@link #where}. */
    static boolean structure(QuestObjective obj) {
        return obj instanceof FindStructureObjective;
    }

    static boolean biome(QuestObjective obj) {
        return obj instanceof FindBiomeObjective;
    }

    /**
     * submit_item: hands in the matching items the player has, for one quest or (only null) every
     * active quest, own and team, up to what each goal still needs. Returns how many items it took.
     */
    public static int submit(ServerPlayer player, ResourceLocation only) {
        WorldQuestStore store = WorldQuestStore.get();
        if (store == null) return 0;
        PlayerQuestData view = com.erikedits.justquests.team.TeamQuests.view(player, store.peek(player.getUUID()));
        if (view == null) return 0;
        int total = 0;
        for (Map.Entry<ResourceLocation, QuestProgress> e : new ArrayList<>(view.active.entrySet())) {
            if (only != null && !only.equals(e.getKey())) continue;
            Quest quest = QuestManager.INSTANCE.get(e.getKey());
            if (quest == null) continue;
            List<QuestObjective> goals = quest.objectives();
            for (int i = 0; i < goals.size(); i++) {
                if (!(goals.get(i) instanceof SubmitItemObjective s)) continue;
                int missing = s.requiredCount() - e.getValue().get(i);
                if (missing <= 0) continue;
                int taken = s.take(player, missing);
                if (taken <= 0) continue;
                total += taken;
                // only this goal of this quest: the same goal in another quest needs its own items
                QuestProgressService.advance(player, obj -> obj == s ? taken : 0);
            }
        }
        if (total > 0) player.containerMenu.broadcastChanges();
        return total;
    }

    /** The active quests (own and team) with a submit_item goal that still needs items. */
    public static List<ResourceLocation> submittable(ServerPlayer player) {
        List<ResourceLocation> out = new ArrayList<>();
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData view = store == null ? null : com.erikedits.justquests.team.TeamQuests.view(player, store.peek(player.getUUID()));
        if (view == null) return out;
        for (Map.Entry<ResourceLocation, QuestProgress> e : view.active.entrySet()) {
            Quest quest = QuestManager.INSTANCE.get(e.getKey());
            if (quest == null) continue;
            for (int i = 0; i < quest.objectives().size(); i++) {
                if (quest.objectives().get(i) instanceof SubmitItemObjective s && e.getValue().get(i) < s.requiredCount()) {
                    out.add(e.getKey());
                    break;
                }
            }
        }
        return out;
    }

    /** The "Traded with Villagers" statistic of each player while a trade screen is open. */
    private static final Map<UUID, Integer> TRADES = new HashMap<>();

    /**
     * trade_villager on Forge before 1.20, which has no trade event: every tick, the statistic
     * "Traded with Villagers" while a trade screen is open; the trade is the offer picked in it.
     */
    public static void pollTrades(ServerPlayer player) {
        if (!(player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu menu)) {
            TRADES.remove(player.getUUID());
            return;
        }
        int now = player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.TRADED_WITH_VILLAGER));
        Integer old = TRADES.put(player.getUUID(), now);
        if (old == null || now <= old) return;
        net.minecraft.world.item.trading.MerchantOffer offer =
            ((net.minecraft.world.inventory.MerchantContainer) menu.getSlot(0).container).getActiveOffer();
        AbstractVillager trader = player.level.getEntitiesOfClass(AbstractVillager.class, player.getBoundingBox().inflate(16),
            v -> v.getTradingPlayer() == player).stream().findFirst().orElse(null);
        if (offer == null || trader == null) return;
        for (int i = 0; i < now - old; i++) traded(player, trader, offer.getResult());
    }

    /** Server stop or a player leaving. */
    public static void forget(UUID player) {
        LAST.remove(player);
    }

    public static void clear() {
        LAST.clear();
    }
}
