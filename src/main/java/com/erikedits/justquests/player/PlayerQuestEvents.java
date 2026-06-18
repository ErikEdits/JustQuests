package com.erikedits.justquests.player;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.objective.CollectItemObjective;
import com.erikedits.justquests.data.objective.KillMobObjective;
import com.erikedits.justquests.progress.QuestProgressService;
import com.erikedits.justquests.registry.ModAttachments;
import com.erikedits.justquests.storage.WorldQuestStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerQuestEvents {
    /**
     * One-time migration from the v0.1 per-player NBT attachment into the
     * new per-world progress file. Runs on login; skips players who already
     * have an entry in the store, so it never re-imports or overwrites.
     */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
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
            var type = event.getEntity().getType();
            QuestProgressService.advance(killer, obj ->
                (obj instanceof KillMobObjective k && k.matches(type)) ? 1 : 0);
        }
    }
}
