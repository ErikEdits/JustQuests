package com.erikedits.justquests.player;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.PlayerQuestData;
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
import com.erikedits.justquests.registry.ModAttachments;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.erikedits.justquests.storage.WorldSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.VersionChecker;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class PlayerQuestEvents {
    /**
     * One-time migration from the v0.1 per-player NBT attachment into the
     * new per-world progress file. Runs on login; skips players who already
     * have an entry in the store, so it never re-imports or overwrites.
     */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // One-time, clickable Discord welcome (0.1.5). Independent of the
        // migration below, which may early-return.
        com.erikedits.justquests.community.CommunityHints.onLogin(player);
        // Outdated-version notice for OPs / the singleplayer host (0.1.10).
        notifyIfOutdated(player);
        WorldQuestStore store = WorldQuestStore.get();
        if (store == null || store.has(player.getUUID())) return;

        PlayerQuests old = player.getData(ModAttachments.PLAYER_QUESTS);
        if (old.active().isEmpty() && old.completed().isEmpty()) return;

        PlayerQuestData data = store.get(player.getUUID());
        old.active().forEach(data.active::put);
        old.completed().forEach(id -> data.completed.put(id, 0L));
        store.markDirty();
        // Clear the old attachment so the migrated data doesn't linger in
        // playerdata and can't be imported a second time.
        player.removeData(ModAttachments.PLAYER_QUESTS);
        JustQuests.LOG.info("Migrated v0.1 quest data for {}", player.getGameProfile().getName());
    }

    /**
     * Tells OPs (and the singleplayer host) when a newer version is on
     * Modrinth. Uses NeoForge's built-in version checker, which reads the
     * updateJSONURL (Modrinth's forge_updates.json) — a public file, no
     * tokens. Toggle: updateNotice in settings.json (Q36).
     */
    private void notifyIfOutdated(ServerPlayer player) {
        if (!WorldSettings.updateNotice()) return;
        MinecraftServer server = player.getServer();
        boolean canUpdate = player.hasPermissions(2) || (server != null && server.isSingleplayer());
        if (!canUpdate) return;
        ModList.get().getModContainerById(JustQuests.MOD_ID).ifPresent(mc -> {
            VersionChecker.CheckResult result = VersionChecker.getResult(mc.getModInfo());
            if (result.status() == VersionChecker.Status.OUTDATED
                    || result.status() == VersionChecker.Status.BETA_OUTDATED) {
                String target = result.target() != null ? result.target().toString() : "a newer version";
                player.sendSystemMessage(Component.literal(
                    "§e[JustQuests] §fVersion " + target + " is available. "
                    + "§7Update via the Modrinth app or §9§nhttps://modrinth.com/mod/justquests"));
            }
        });
    }

    /** collect_item: advance objectives that match the picked-up item. */
    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack picked = event.getOriginalStack();
        if (picked.isEmpty()) return;

        QuestProgressService.advance(serverPlayer, obj ->
            (obj instanceof CollectItemObjective c && c.matches(picked)) ? picked.getCount() : 0);
    }

    /** kill_mob: advance objectives that match the killed entity type. */
    @SubscribeEvent
    public void onMobKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            EntityType<?> type = event.getEntity().getType();
            QuestProgressService.advance(killer, obj ->
                (obj instanceof KillMobObjective k && k.matches(type)) ? 1 : 0);
        }
    }

    /** place_block */
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
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
    @SubscribeEvent
    public void onTame(AnimalTameEvent event) {
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
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            Block block = event.getState().getBlock();
            QuestProgressService.advance(player, obj ->
                (obj instanceof MineBlockObjective m && m.matches(block)) ? 1 : 0);
        }
    }

    /** breed_animal: matched on the species of the breeding parent. */
    @SubscribeEvent
    public void onBreed(BabyEntitySpawnEvent event) {
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
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            QuestProgressService.advance(player, obj -> {
                if (obj instanceof ReachLocationObjective r && r.isAt(player)) return 1;
                if (obj instanceof ReachLevelObjective l && l.reached(player)) return 1;
                return 0;
            });
        }
    }
}
