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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerQuestEvents {
    /** One-time, clickable Discord welcome (0.1.5); no v0.1 data to migrate on Forge. */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        com.erikedits.justquests.community.CommunityHints.onLogin(player);
    }

    /** collect_item */
    @SubscribeEvent
    public void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        ItemStack picked = event.getStack();
        if (picked.isEmpty()) return;
        QuestProgressService.advance(serverPlayer, obj ->
            (obj instanceof CollectItemObjective c && c.matches(picked)) ? picked.getCount() : 0);
    }

    /** kill_mob */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMobKill(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            EntityType<?> type = event.getEntity().getType();
            QuestProgressService.advance(killer, obj ->
                (obj instanceof KillMobObjective k && k.matches(type)) ? 1 : 0);
        }
    }

    /** place_block */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            Block block = event.getPlacedBlock().getBlock();
            QuestProgressService.advance(player, obj ->
                (obj instanceof PlaceBlockObjective p && p.matches(block)) ? 1 : 0);
        }
    }

    /** craft_item */
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
        if (event.isCanceled()) return;
        if (event.getTamer() instanceof ServerPlayer player) {
            EntityType<?> type = event.getAnimal().getType();
            QuestProgressService.advance(player, obj ->
                (obj instanceof TameAnimalObjective t && t.matches(type)) ? 1 : 0);
        }
    }

    /** gain_advancement */
    @SubscribeEvent
    public void onAdvancement(AdvancementEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation id = event.getAdvancement().getId();
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

    /** mine_block */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) return;
        if (event.getPlayer() instanceof ServerPlayer player) {
            Block block = event.getState().getBlock();
            QuestProgressService.advance(player, obj ->
                (obj instanceof MineBlockObjective m && m.matches(block)) ? 1 : 0);
        }
    }

    /** breed_animal */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreed(BabyEntitySpawnEvent event) {
        if (event.isCanceled()) return;
        if (event.getCausedByPlayer() instanceof ServerPlayer player && event.getParentA() != null) {
            EntityType<?> type = event.getParentA().getType();
            QuestProgressService.advance(player, obj ->
                (obj instanceof BreedAnimalObjective b && b.matches(type)) ? 1 : 0);
        }
    }

    /** consume_item */
    @SubscribeEvent
    public void onConsume(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack item = event.getItem();
            if (item.isEmpty()) return;
            QuestProgressService.advance(player, obj ->
                (obj instanceof ConsumeItemObjective c && c.matches(item)) ? 1 : 0);
        }
    }

    /** smelt_item */
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
