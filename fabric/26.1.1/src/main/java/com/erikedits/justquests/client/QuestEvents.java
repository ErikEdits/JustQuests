package com.erikedits.justquests.client;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.network.ClientQuestData;
import com.erikedits.justquests.player.QuestProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * What changed with the last quest sync, as sounds and popups (0.5.0, #21): a quest accepted,
 * abandoned, unlocked or completed, a reward claimed, a goal counting up. Every sound is a JustQuests
 * sound ID (justquests:quest.accept ...); where the mod has no sound, the ID is empty and a resource
 * pack can give it one. The completion sound itself comes from the server, so its world setting
 * still turns it off.
 */
public final class QuestEvents {
    private static final Map<String, Identifier> SOUNDS = new HashMap<>();
    private static int seenSync = -1;
    private static Snapshot last;

    private QuestEvents() {}

    /** The state of the last sync: what was active (with progress), completed, waiting and unlocked. */
    private record Snapshot(Set<Identifier> quests, Map<Identifier, int[]> active,
                            Map<Identifier, Long> completed, Set<Identifier> waiting,
                            Set<Identifier> unlocked) {
        static Snapshot now() {
            PlayerQuestData d = ClientQuestData.getData();
            Map<Identifier, int[]> active = new HashMap<>();
            for (Map.Entry<Identifier, QuestProgress> e : d.active.entrySet()) {
                Quest q = ClientQuestData.get(e.getKey());
                int n = q != null ? q.objectives().size() : 0;
                int[] values = new int[n];
                for (int i = 0; i < n; i++) values[i] = e.getValue().get(i);
                active.put(e.getKey(), values);
            }
            Set<Identifier> unlocked = new HashSet<>();
            ClientQuestData.getQuests().forEach((id, q) -> {
                if (!d.isCompleted(id) && q.unlocked(d::isCompleted)) unlocked.add(id);
            });
            return new Snapshot(new HashSet<>(ClientQuestData.getQuests().keySet()), active,
                new HashMap<>(d.completed), new HashSet<>(d.pendingClaim.keySet()), unlocked);
        }
    }

    /** Once a client tick: compares the latest sync with the one before. */
    public static void tick() {
        int s = ClientQuestData.syncCount();
        if (s == seenSync) return;
        seenSync = s;
        Snapshot now = Snapshot.now(), before = last;
        last = now;
        // the first sync after joining (or a disconnect) only sets the starting point
        if (before == null || before.quests().isEmpty() || now.quests().isEmpty()) return;

        String sound = null;
        int rank = Integer.MAX_VALUE;
        Set<Identifier> finished = new HashSet<>();
        for (Map.Entry<Identifier, Long> e : now.completed().entrySet()) {
            if (!e.getValue().equals(before.completed().get(e.getKey()))) finished.add(e.getKey());
        }
        for (Identifier id : finished) {
            Quest q = ClientQuestData.get(id);
            if (q != null) Popups.questDone(id, q, now.waiting().contains(id));
        }
        for (Identifier id : now.unlocked()) {
            Quest q = ClientQuestData.get(id);
            if (before.unlocked().contains(id) || !before.quests().contains(id) || q == null) continue;
            Popups.questNew(id, q);
            if (rank > 0) { sound = "quest.unlock"; rank = 0; }
        }
        for (Identifier id : before.waiting()) {
            if (!now.waiting().contains(id) && rank > 1) { sound = "reward.claim"; rank = 1; }
        }
        for (Identifier id : now.active().keySet()) {
            if (!before.active().containsKey(id) && rank > 2) { sound = "quest.accept"; rank = 2; }
        }
        for (Identifier id : before.active().keySet()) {
            boolean gone = !now.active().containsKey(id) && !finished.contains(id) && now.quests().contains(id);
            if (gone && rank > 3) { sound = "quest.abandon"; rank = 3; }
        }
        for (Map.Entry<Identifier, int[]> e : now.active().entrySet()) {
            int[] was = before.active().get(e.getKey());
            Quest q = ClientQuestData.get(e.getKey());
            if (was == null || q == null) continue;
            for (int i = 0; i < e.getValue().length && i < was.length; i++) {
                if (e.getValue()[i] <= was[i]) continue;
                int need = q.objectives().get(i).requiredCount();
                if (e.getValue()[i] >= need && was[i] < need && rank > 4) { sound = "goal.complete"; rank = 4; }
                else if (rank > 5) { sound = "goal.progress"; rank = 5; }
            }
        }
        if (sound != null) sound(sound);
    }

    /** Plays a JustQuests sound ID ("book.open" is justquests:book.open), unless it is empty. */
    public static void sound(String name) {
        Identifier id = SOUNDS.computeIfAbsent(name, n -> Identifier.tryBuild(Theme.NS, n));
        if (id == null) return;
        // the game warns in the log about every empty sound it is asked to play
        WeighedSoundEvents sounds = Minecraft.getInstance().getSoundManager().getSoundEvent(id);
        if (sounds == null || sounds.getWeight() == 0) return;
        // full volume like the advancement toasts; the click has its quieter volume in sounds.json
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id), 1.0F, 1.0F));
    }
}
