package com.erikedits.justquests.player;

import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.erikedits.justquests.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerQuestEvents {
    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack picked = event.getOriginalStack();
        if (picked.isEmpty()) return;

        PlayerQuests data = serverPlayer.getData(ModAttachments.PLAYER_QUESTS);
        List<ResourceLocation> completing = new ArrayList<>();

        for (Map.Entry<ResourceLocation, QuestProgress> entry : data.active().entrySet()) {
            ResourceLocation questId = entry.getKey();
            Quest quest = QuestManager.INSTANCE.get(questId);
            if (quest == null) continue;

            QuestProgress progress = entry.getValue();
            boolean allComplete = true;

            for (int i = 0; i < quest.objectives().size(); i++) {
                QuestObjective obj = quest.objectives().get(i);
                if (obj.matches(picked)) {
                    int remaining = obj.requiredCount() - progress.get(i);
                    int add = Math.min(picked.getCount(), remaining);
                    if (add > 0) progress.increment(i, add);
                }
                if (progress.get(i) < obj.requiredCount()) {
                    allComplete = false;
                }
            }

            if (allComplete) {
                completing.add(questId);
            }
        }

        for (ResourceLocation questId : completing) {
            Quest quest = QuestManager.INSTANCE.get(questId);
            data.complete(questId);
            for (QuestReward reward : quest.rewards()) {
                reward.grant(serverPlayer);
            }
            serverPlayer.sendSystemMessage(Component.literal("§a✓ Quest completed: " + quest.title()));
        }
    }
}
