package com.erikedits.justquests.player;

import com.erikedits.justquests.data.objective.BreedAnimalObjective;
import com.erikedits.justquests.data.objective.CollectItemObjective;
import com.erikedits.justquests.data.objective.ConsumeItemObjective;
import com.erikedits.justquests.data.objective.CraftItemObjective;
import com.erikedits.justquests.data.objective.GainAdvancementObjective;
import com.erikedits.justquests.data.objective.KillMobObjective;
import com.erikedits.justquests.data.objective.MineBlockObjective;
import com.erikedits.justquests.data.objective.PlaceBlockObjective;
import com.erikedits.justquests.data.objective.ReachLevelObjective;
import com.erikedits.justquests.data.objective.ReachLocationObjective;
import com.erikedits.justquests.data.objective.SmeltItemObjective;
import com.erikedits.justquests.data.objective.TameAnimalObjective;
import com.erikedits.justquests.data.objective.VisitDimensionObjective;
import com.erikedits.justquests.progress.QuestProgressService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class PlayerQuestEvents {
    /**
     * Runs on login: one-time, clickable Discord welcome (0.1.5). There is no
     * v0.1 data to migrate on 1.20.4 (it never shipped v0.1).
     */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        com.erikedits.justquests.community.CommunityHints.onLogin(player);
    }

    /** collect_item: advance objectives that match the picked-up item. */
    @SubscribeEvent
    public void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        ItemStack picked = event.getStack();
        if (picked.isEmpty()) return;

        QuestProgressService.advance(serverPlayer, obj ->
            (obj instanceof CollectItemObjective c && c.matches(picked)) ? picked.getCount() : 0);
    }

    /** kill_mob: advance objectives that match the killed entity type. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMobKill(LivingDeathEvent event) {
        if (event.isCanceled()) return; // death prevented (totem, other mods)
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            EntityType<?> type = event.getEntity().getType();
            QuestProgressService.advance(killer, obj ->
                (obj instanceof KillMobObjective k && k.matches(type)) ? 1 : 0);
        }
    }

    /** place_block */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()) return; // placement blocked (protection/claims)
        if (event.getEntity() instanceof ServerPlayer player) {
            Block block = event.getPlacedBlock().getBlock();
            QuestProgressService.advance(player, obj ->
                (obj instanceof PlaceBlockObjective p && p.matches(block)) ? 1 : 0);
        }
    }

    /** craft_item (also catches items that never fire a pickup event) */
    @SubscribeEvent
    public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack crafted = event.getCrafting();
            if (crafted.isEmpty()) return;
            QuestProgressService.advance(player, obj ->
                (obj instanceof CraftItemObjective c && c.matches(crafted)) ? crafted.getCount() : 0);
        }
    }

    /** tame_animal */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTame(AnimalTameEvent event) {
        if (event.isCanceled()) return; // taming prevented
        if (event.getTamer() instanceof ServerPlayer player) {
            EntityType<?> type = event.getAnimal().getType();
            QuestProgressService.advance(player, obj ->
                (obj instanceof TameAnimalObjective t && t.matches(type)) ? 1 : 0);
        }
    }

    /** gain_advancement */
    @SubscribeEvent
    public void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation id = event.getAdvancement().id();
            QuestProgressService.advance(player, obj ->
                (obj instanceof GainAdvancementObjective g && g.matches(id)) ? 1 : 0);
        }
    }

    /** visit_dimension */
    @SubscribeEvent
    public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation to = event.getTo().location();
            QuestProgressService.advance(player, obj ->
                (obj instanceof VisitDimensionObjective v && v.matches(to)) ? 1 : 0);
        }
    }

    /** mine_block: counts the block-break itself (distinct from collect_item). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) return; // break blocked (protection/claims)
        if (event.getPlayer() instanceof ServerPlayer player) {
            Block block = event.getState().getBlock();
            QuestProgressService.advance(player, obj ->
                (obj instanceof MineBlockObjective m && m.matches(block)) ? 1 : 0);
        }
    }

    /** breed_animal: matched on the species of the breeding parent. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreed(BabyEntitySpawnEvent event) {
        if (event.isCanceled()) return; // breeding prevented
        if (event.getCausedByPlayer() instanceof ServerPlayer player && event.getParentA() != null) {
            EntityType<?> type = event.getParentA().getType();
            QuestProgressService.advance(player, obj ->
                (obj instanceof BreedAnimalObjective b && b.matches(type)) ? 1 : 0);
        }
    }

    /** consume_item: eating/drinking (finish using) a matching item. */
    @SubscribeEvent
    public void onConsume(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack item = event.getItem();
            if (item.isEmpty()) return;
            QuestProgressService.advance(player, obj ->
                (obj instanceof ConsumeItemObjective c && c.matches(item)) ? 1 : 0);
        }
    }

    /** smelt_item: taking a matching result out of a furnace. */
    @SubscribeEvent
    public void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack smelted = event.getSmelting();
            if (smelted.isEmpty()) return;
            QuestProgressService.advance(player, obj ->
                (obj instanceof SmeltItemObjective s && s.matches(smelted)) ? smelted.getCount() : 0);
        }
    }

    /** reach_location + reach_level: checked once a second per player. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            QuestProgressService.advance(player, obj -> {
                if (obj instanceof ReachLocationObjective r && r.isAt(player)) return 1;
                if (obj instanceof ReachLevelObjective l && l.reached(player)) return 1;
                return 0;
            });
        }
    }
}
