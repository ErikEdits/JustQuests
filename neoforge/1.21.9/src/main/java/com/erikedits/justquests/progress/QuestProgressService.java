package com.erikedits.justquests.progress;

import com.erikedits.justquests.text.Msg;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.QuestMode;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.ChoiceReward;
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
        advance(player, null, test);
    }

    /**
     * As {@link #advance(ServerPlayer, ObjectiveTest)}, for an event about one kind of thing (the
     * id of an item, block or mob): a distinct objective counts every kind only once.
     */
    public static void advance(ServerPlayer player, String kind, ObjectiveTest test) {
        com.erikedits.justquests.team.TeamQuests.advance(player, kind, test);
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

            for (int i = 0; i < quest.objectives().size(); i++) {
                QuestObjective obj = quest.objectives().get(i);
                int add = counted(obj, progress, i, kind, test.amount(obj));
                if (add > 0) {
                    int remaining = obj.requiredCount() - progress.get(i);
                    int real = Math.min(add, remaining);
                    if (real > 0) {
                        progress.increment(i, real);
                        changed = true;
                    }
                }
            }

            if (quest.done(i -> progress.get(i) >= quest.objectives().get(i).requiredCount())) {
                completing.add(entry.getKey());
            }
        }

        for (ResourceLocation questId : completing) {
            Quest quest = QuestManager.INSTANCE.get(questId);
            if (quest == null) continue; // quest vanished mid-tick (e.g. custom reload)
            boolean claim = finish(player, data, questId, quest);
            String questTitle = quest.title().get(player.clientInformation().language());
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
        unlocks(player);

        if (changed) {
            store.markDirty();
            com.erikedits.justquests.network.QuestNetwork.syncProgress(player);
        }
    }

    /** Quests each player finished for the first time, until {@link #unlocks} looks at what they unlock. */
    private static final Map<java.util.UUID, List<ResourceLocation>> FRESH = new java.util.HashMap<>();

    /**
     * The quests that the player's newly finished quests unlocked: with auto-accept on (the default)
     * they are accepted at once, else the player gets an Accept button for each. Team quests only get
     * the button. Called after the "completed" messages, so the chat reads in order.
     */
    public static void unlocks(ServerPlayer player) {
        List<ResourceLocation> fresh = FRESH.remove(player.getUUID());
        WorldQuestStore store = WorldQuestStore.get();
        if (fresh == null || store == null) return;
        PlayerQuestData data = store.get(player.getUUID());
        String lang = player.clientInformation().language();
        boolean auto = data.autoAccept.orElse(WorldSettings.autoAccept());
        boolean accepted = false;
        for (Map.Entry<ResourceLocation, Quest> e : QuestManager.INSTANCE.getQuests().entrySet()) {
            ResourceLocation id = e.getKey();
            Quest quest = e.getValue();
            if (quest.requires().stream().noneMatch(fresh::contains) || !available(player, data, id, quest)) continue;
            if (quest.team() && com.erikedits.justquests.team.TeamQuests.activeIds(player).contains(id)) continue;
            String title = quest.title().get(lang);
            if (auto && !quest.team()) {
                data.accept(id);
                accepted = true;
                player.sendSystemMessage(Msg.tr("justquests.unlocked.accepted", title).append(Msg.lit(" "))
                    .append(Msg.button(Msg.tr("justquests.unlocked.auto_off"), "/quest autoaccept off", Msg.tr("justquests.unlocked.auto_off_hover"))));
            } else {
                player.sendSystemMessage(Msg.tr("justquests.unlocked", title).append(Msg.lit(" "))
                    .append(Msg.button(Msg.tr("justquests.unlocked.accept"), "/quest accept " + id, Msg.tr("justquests.unlocked.accept_hover"))));
            }
        }
        if (accepted) {
            store.markDirty();
            com.erikedits.justquests.network.QuestNetwork.syncProgress(player);
        }
    }

    /**
     * Whether the player may take the quest now - what the quest book shows as available: allowed,
     * not active or waiting to be claimed, unlocked, and never done or repeatable and off cooldown.
     */
    public static boolean available(ServerPlayer player, PlayerQuestData data, ResourceLocation id, Quest quest) {
        if (!com.erikedits.justquests.perm.Perms.quest(player, quest) || data.isActive(id) || data.isClaimable(id)) return false;
        if (!quest.unlocked(data::isCompleted)) return false;
        Long done = data.completed.get(id);
        return done == null || quest.repeatable() && System.currentTimeMillis() - done >= quest.cooldownHours().orElse(0) * 3_600_000L;
    }

    /**
     * What an event adds to an objective: the test's amount, or for a distinct objective 1 for a
     * kind it has not counted yet (remembered from then on) and 0 for one it has.
     */
    public static int counted(QuestObjective obj, QuestProgress progress, int index, String kind, int amount) {
        if (amount <= 0 || !obj.distinct()) return amount;
        if (kind == null || progress.get(index) >= obj.requiredCount()) return 0;
        return progress.see(index, kind) ? 1 : 0;
    }

    /** The kind a distinct objective counts once: the id of the item, block or mob. */
    public static String kind(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static String kind(net.minecraft.world.level.block.Block block) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    public static String kind(net.minecraft.world.entity.EntityType<?> type) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    /** reset_on_death: the player died, so their quests (and team quests) with that option start over. */
    public static void died(ServerPlayer player) {
        com.erikedits.justquests.team.TeamQuests.died(player);
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());
        if (data == null || data.active.isEmpty()) return;
        boolean changed = false;
        for (Map.Entry<ResourceLocation, QuestProgress> entry : data.active.entrySet()) {
            Quest quest = QuestManager.INSTANCE.get(entry.getKey());
            if (quest == null || !quest.resetOnDeath() || !entry.getValue().hasProgress()) continue;
            entry.getValue().reset();
            player.sendSystemMessage(Msg.tr("justquests.reset.death", quest.title().get(player.clientInformation().language())));
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
     * Quests with a choice reward always wait, since the player has to pick.
     *
     * @return true when the rewards wait to be claimed
     */
    public static boolean finish(ServerPlayer player, PlayerQuestData data, ResourceLocation id, Quest quest) {
        // what it unlocks is taken or offered by unlocks(), after the caller's messages
        if (!data.isCompleted(id)) FRESH.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(id);
        data.complete(id);
        // a quest without rewards has nothing to claim: it is simply done
        boolean waits = !quest.rewards().isEmpty() && (WorldSettings.claimRewards() || ChoiceReward.of(quest) != null);
        com.erikedits.justquests.generator.GenV2.completed(id, player.getUUID(), waits);
        if (waits) {
            data.pendingClaim.put(id, System.currentTimeMillis());
            return true;
        }
        grant(player, quest, -1);
        return false;
    }

    /** Pays out a finished quest's waiting rewards; false if none wait (or the quest was removed since). */
    public static boolean claim(ServerPlayer player, PlayerQuestData data, ResourceLocation id) {
        return claim(player, data, id, -1);
    }

    /** As {@link #claim(ServerPlayer, PlayerQuestData, ResourceLocation)}, with the picked option (0-based) of a choice. */
    public static boolean claim(ServerPlayer player, PlayerQuestData data, ResourceLocation id, int pick) {
        if (data.pendingClaim.remove(id) == null) return false;
        Quest quest = QuestManager.INSTANCE.get(id);
        if (quest != null) grant(player, quest, pick);
        com.erikedits.justquests.generator.GenV2.rewardsClaimed(id, player.getUUID());
        return quest != null;
    }

    private static void grant(ServerPlayer player, Quest quest, int pick) {
        ChoiceReward choice = ChoiceReward.of(quest);
        for (QuestReward reward : quest.rewards()) {
            if (reward == choice) choice.grant(player, pick);
            else reward.grant(player);
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
