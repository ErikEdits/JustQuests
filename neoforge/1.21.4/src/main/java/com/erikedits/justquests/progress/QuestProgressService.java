package com.erikedits.justquests.progress;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.QuestMode;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.erikedits.justquests.player.QuestProgress;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.erikedits.justquests.storage.WorldSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Central place that advances quest progress from any game event. Each
 * event handler (item pickup, mob kill, …) calls {@link #advance} with a
 * small test that says how much a given objective should advance for that
 * event. This keeps the increment + completion + reward logic in one spot,
 * so adding new objective types only means a new handler + a new test.
 */
public final class QuestProgressService {

    private QuestProgressService() {}

    /** Returns how much the event contributes to this objective (0 = none). */
    @FunctionalInterface
    public interface ObjectiveTest {
        int amount(QuestObjective objective);
    }

    public static void advance(ServerPlayer player, ObjectiveTest test) {
        WorldQuestStore store = WorldQuestStore.get();
        if (store == null) return;

        // peek: don't create an entry for players with no quests
        PlayerQuestData data = store.peek(player.getUUID());
        if (data == null || data.active.isEmpty()) return;

        boolean changed = false;
        List<ResourceLocation> completing = new ArrayList<>();

        for (Map.Entry<ResourceLocation, QuestProgress> entry : data.active.entrySet()) {
            Quest quest = QuestManager.INSTANCE.get(entry.getKey());
            if (quest == null) continue;

            QuestProgress progress = entry.getValue();
            boolean allComplete = true;
            boolean anyComplete = false;

            for (int i = 0; i < quest.objectives().size(); i++) {
                QuestObjective obj = quest.objectives().get(i);
                int add = test.amount(obj);
                if (add > 0) {
                    int remaining = obj.requiredCount() - progress.get(i);
                    int real = Math.min(add, remaining);
                    if (real > 0) {
                        progress.increment(i, real);
                        changed = true;
                    }
                }
                if (progress.get(i) >= obj.requiredCount()) {
                    anyComplete = true;
                } else {
                    allComplete = false;
                }
            }

            boolean done = quest.mode() == QuestMode.ANY ? anyComplete : allComplete;
            if (done) {
                completing.add(entry.getKey());
            }
        }

        for (ResourceLocation questId : completing) {
            Quest quest = QuestManager.INSTANCE.get(questId);
            if (quest == null) continue; // quest vanished mid-tick (e.g. custom reload)
            data.complete(questId);
            for (QuestReward reward : quest.rewards()) {
                reward.grant(player);
            }
            String questTitle = quest.title().get(player.clientInformation().language());
            player.sendSystemMessage(Component.literal("§a✓ Quest completed: " + questTitle));
            // completion sound + action-bar toast (Q12), each toggleable
            if (WorldSettings.completionSound()) {
                player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
            }
            if (WorldSettings.completionToast()) {
                player.displayClientMessage(Component.literal("§a✓ " + questTitle), true);
            }
            // optional server-wide announcement (Q53), default on
            if (WorldSettings.announceCompletions()) {
                MinecraftServer server = player.level().getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(Component.literal(
                        "§e" + player.getName().getString() + " §7completed §f"
                        + quest.title().getDefault() + "§7!"), false);
                }
            }
            changed = true;
        }

        if (changed) {
            store.markDirty();
        }
    }
}
