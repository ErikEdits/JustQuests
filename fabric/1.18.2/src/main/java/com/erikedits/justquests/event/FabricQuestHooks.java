package com.erikedits.justquests.event;

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

/**
 * Central routing from Fabric events / mixins into the shared
 * {@link QuestProgressService}. This is the Fabric equivalent of the NeoForge
 * {@code PlayerQuestEvents} handler — same objective-matching logic, different
 * event sources (Fabric API callbacks + mixins for events Fabric lacks).
 */
public final class FabricQuestHooks {
    private FabricQuestHooks() {}

    /** collect_item */
    public static void onItemPickup(ServerPlayer player, ItemStack picked, int amount) {
        if (picked.isEmpty() || amount <= 0) return;
        QuestProgressService.advance(player, obj ->
            (obj instanceof CollectItemObjective c && c.matches(picked)) ? amount : 0);
    }

    /** kill_mob */
    public static void onKill(ServerPlayer killer, EntityType<?> type) {
        QuestProgressService.advance(killer, obj ->
            (obj instanceof KillMobObjective k && k.matches(type)) ? 1 : 0);
    }

    /** place_block */
    public static void onBlockPlace(ServerPlayer player, Block block) {
        QuestProgressService.advance(player, obj ->
            (obj instanceof PlaceBlockObjective p && p.matches(block)) ? 1 : 0);
    }

    /** craft_item */
    public static void onCraft(ServerPlayer player, ItemStack crafted) {
        if (crafted.isEmpty()) return;
        QuestProgressService.advance(player, obj ->
            (obj instanceof CraftItemObjective c && c.matches(crafted)) ? crafted.getCount() : 0);
    }

    /** tame_animal */
    public static void onTame(ServerPlayer player, EntityType<?> type) {
        QuestProgressService.advance(player, obj ->
            (obj instanceof TameAnimalObjective t && t.matches(type)) ? 1 : 0);
    }

    /** gain_advancement */
    public static void onAdvancement(ServerPlayer player, ResourceLocation id) {
        QuestProgressService.advance(player, obj ->
            (obj instanceof GainAdvancementObjective g && g.matches(id)) ? 1 : 0);
    }

    /** visit_dimension */
    public static void onDimension(ServerPlayer player, ResourceLocation to) {
        QuestProgressService.advance(player, obj ->
            (obj instanceof VisitDimensionObjective v && v.matches(to)) ? 1 : 0);
    }

    /** mine_block */
    public static void onBlockBreak(ServerPlayer player, Block block) {
        QuestProgressService.advance(player, obj ->
            (obj instanceof MineBlockObjective m && m.matches(block)) ? 1 : 0);
    }

    /** breed_animal */
    public static void onBreed(ServerPlayer player, EntityType<?> type) {
        QuestProgressService.advance(player, obj ->
            (obj instanceof BreedAnimalObjective b && b.matches(type)) ? 1 : 0);
    }

    /** consume_item */
    public static void onConsume(ServerPlayer player, ItemStack item) {
        if (item.isEmpty()) return;
        QuestProgressService.advance(player, obj ->
            (obj instanceof ConsumeItemObjective c && c.matches(item)) ? 1 : 0);
    }

    /** smelt_item */
    public static void onSmelt(ServerPlayer player, ItemStack smelted) {
        if (smelted.isEmpty()) return;
        QuestProgressService.advance(player, obj ->
            (obj instanceof SmeltItemObjective s && s.matches(smelted)) ? smelted.getCount() : 0);
    }

    /** reach_location + reach_level, polled once a second per player from the server tick. */
    public static void onPlayerTickReach(ServerPlayer player) {
        QuestProgressService.advance(player, obj -> {
            if (obj instanceof ReachLocationObjective r && r.isAt(player)) return 1;
            if (obj instanceof ReachLevelObjective l && l.reached(player)) return 1;
            return 0;
        });
    }
}
