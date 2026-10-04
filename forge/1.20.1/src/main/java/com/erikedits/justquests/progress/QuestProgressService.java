package com.erikedits.justquests.progress;

import com.erikedits.justquests.text.Msg;
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
            boolean claim = finish(player, data, questId, quest);
            String questTitle = quest.title().get(com.erikedits.justquests.data.LocalizedText.DEFAULT_LANG);
            player.sendSystemMessage(completeChat(questTitle, questId, claim));
            // completion sound + action-bar toast (Q12), each toggleable
            if (WorldSettings.completionSound()) {
                player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
            }
            if (WorldSettings.completionToast()) {
                player.displayClientMessage(Msg.tr("justquests.complete.toast", questTitle), true);
            }
            // optional server-wide announcement (Q53), default on
            if (WorldSettings.announceCompletions()) {
                MinecraftServer server = player.level().getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(Msg.tr("justquests.complete.broadcast", player.getName().getString(), quest.title().getDefault()), false);
                }
            }
            changed = true;
        }

        if (changed) {
            store.markDirty();
            com.erikedits.justquests.network.QuestNetwork.syncProgress(player);
        }
    }

    /**
     * Marks a quest finished for the player. With claimRewards on (the default) its rewards wait in
     * pendingClaim for the quest book's Claim button or /quest claim; otherwise they are granted now.
     *
     * @return true when the rewards wait to be claimed
     */
    public static boolean finish(ServerPlayer player, PlayerQuestData data, ResourceLocation id, Quest quest) {
        data.complete(id);
        com.erikedits.justquests.generator.GenV2.completed(id, player.getUUID());
        if (WorldSettings.claimRewards()) {
            data.pendingClaim.put(id, System.currentTimeMillis());
            return true;
        }
        grant(player, quest);
        return false;
    }

    /** Pays out a finished quest's waiting rewards; false if none wait (or the quest was removed since). */
    public static boolean claim(ServerPlayer player, PlayerQuestData data, ResourceLocation id) {
        if (data.pendingClaim.remove(id) == null) return false;
        Quest quest = QuestManager.INSTANCE.get(id);
        if (quest == null) return false;
        grant(player, quest);
        return true;
    }

    private static void grant(ServerPlayer player, Quest quest) {
        for (QuestReward reward : quest.rewards()) {
            reward.grant(player);
        }
    }

    /** A clickable [Claim rewards] that runs /quest claim for the quest. */
    public static net.minecraft.network.chat.MutableComponent claimButton(ResourceLocation id) {
        return Msg.button(Msg.tr("justquests.rewards.button"), "/quest claim " + id, Msg.tr("justquests.rewards.button_hover"));
    }

    /** "Quest completed: X", followed by the claim button while the rewards wait. */
    private static net.minecraft.network.chat.MutableComponent completeChat(String title, ResourceLocation id, boolean claim) {
        net.minecraft.network.chat.MutableComponent m = Msg.tr("justquests.complete.chat", title);
        return claim ? m.append(Msg.lit(" ")).append(claimButton(id)) : m;
    }
}
