package com.erikedits.justquests.data;

import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record Quest(LocalizedText title, LocalizedText description, String category, QuestMode mode,
                    List<Identifier> requires, boolean repeatable, Optional<Integer> cooldownHours,
                    int sort, List<QuestObjective> objectives, List<QuestReward> rewards,
                    Optional<Identifier> icon, Optional<String> permission, boolean team,
                    Optional<Integer> requiresMin, boolean resetOnDeath, List<String> groups) {
    public static final Codec<Quest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        LocalizedText.CODEC.fieldOf("title").forGetter(Quest::title),
        LocalizedText.CODEC.optionalFieldOf("description", LocalizedText.EMPTY).forGetter(Quest::description),
        Codec.STRING.optionalFieldOf("category", "datapack").forGetter(Quest::category),
        QuestMode.CODEC.optionalFieldOf("mode", QuestMode.ALL).forGetter(Quest::mode),
        // quest ids that must be completed before this one can be accepted (Q28)
        Identifier.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(Quest::requires),
        // can be done again after completion; optional cooldown in hours (Q26)
        Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(Quest::repeatable),
        Codec.INT.optionalFieldOf("cooldown_hours").forGetter(Quest::cooldownHours),
        // ordering within a category in /quest list (lower = first); default 0
        Codec.INT.optionalFieldOf("sort", 0).forGetter(Quest::sort),
        // each objective may name a "group": the objectives of one group are alternatives, one of them is enough
        Goal.CODEC.listOf().fieldOf("objectives").forGetter(Quest::goals),
        QuestReward.CODEC.listOf().fieldOf("rewards").forGetter(Quest::rewards),
        // item shown in the quest book and the HUD; picked from the first objective when absent
        Identifier.CODEC.optionalFieldOf("icon").forGetter(Quest::icon),
        // permission node needed to see and take the quest (LuckPerms etc.; operators by default)
        Codec.STRING.optionalFieldOf("permission").forGetter(Quest::permission),
        // a quest for a whole team: every member's actions count, every member claims the rewards
        Codec.BOOL.optionalFieldOf("team", false).forGetter(Quest::team),
        // only this many of "requires" must be done first (any 12 of 16); all of them when left out
        Codec.INT.optionalFieldOf("requires_min").forGetter(Quest::requiresMin),
        // the progress goes back to zero when the player (for a team quest: any member) dies
        Codec.BOOL.optionalFieldOf("reset_on_death", false).forGetter(Quest::resetOnDeath)
    ).apply(instance, (title, description, category, mode, requires, repeatable, cooldownHours, sort, goals, rewards,
                       icon, permission, team, requiresMin, resetOnDeath) ->
        new Quest(title, description, category, mode, requires, repeatable, cooldownHours, sort,
            goals.stream().map(Goal::objective).toList(), rewards, icon, permission, team, requiresMin, resetOnDeath,
            goals.stream().map(Goal::group).toList())));

    /** One entry of "objectives": the objective and the group it belongs to ("" = none). */
    private record Goal(QuestObjective objective, String group) {
        static final Codec<Goal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.<QuestObjective>dispatchMap("type", QuestObjective::typeId, QuestObjective::codecForType).forGetter(Goal::objective),
            Codec.STRING.optionalFieldOf("group", "").forGetter(Goal::group)
        ).apply(instance, Goal::new));
    }

    public Quest {
        // team quests have their own category unless they name one
        if (team && "datapack".equals(category)) category = "team";
        // one group per objective, "" where there is none
        List<String> g = new java.util.ArrayList<>(groups == null ? List.of() : groups);
        while (g.size() < objectives.size()) g.add("");
        groups = List.copyOf(g.subList(0, objectives.size()));
    }

    private List<Goal> goals() {
        List<Goal> out = new java.util.ArrayList<>();
        for (int i = 0; i < objectives.size(); i++) out.add(new Goal(objectives.get(i), groups.get(i)));
        return out;
    }

    /** The group of objective i, "" when it has none. */
    public String group(int i) {
        return groups.get(i);
    }

    /** Whether another objective of objective i's group is done, which makes i done as well. */
    public boolean alternativeDone(int i, java.util.function.IntPredicate done) {
        String g = groups.get(i);
        if (g.isEmpty()) return false;
        for (int j = 0; j < objectives.size(); j++) {
            if (j != i && g.equals(groups.get(j)) && done.test(j)) return true;
        }
        return false;
    }

    /**
     * Whether the objectives are done: every one of them, where one objective of a group is enough;
     * in mode "any", any one of them.
     */
    public boolean done(java.util.function.IntPredicate done) {
        boolean any = false;
        for (int i = 0; i < objectives.size(); i++) {
            if (done.test(i)) any = true;
            else if (mode == QuestMode.ALL && !alternativeDone(i, done)) return false;
        }
        return mode == QuestMode.ALL || any;
    }

    /** The objectives in the order they are shown: the alternatives of a group together, where its first one is. */
    public List<Integer> shownOrder() {
        List<Integer> out = new java.util.ArrayList<>();
        for (int i = 0; i < objectives.size(); i++) {
            if (out.contains(i)) continue;
            out.add(i);
            if (groups.get(i).isEmpty()) continue;
            for (int j = i + 1; j < objectives.size(); j++) if (groups.get(i).equals(groups.get(j))) out.add(j);
        }
        return out;
    }

    /** Whether objective i is shown as "or ..." after another objective of its group. */
    public boolean orBefore(int i) {
        String g = groups.get(i);
        return !g.isEmpty() && groups.indexOf(g) < i;
    }

    /**
     * A quest in a sub-folder that names no category gets the folder as its category:
     * data/&lt;ns&gt;/justquests/quests/mining/iron.json (id path "mining/iron") is in "mining". Deeper
     * folders only sort the files; the first one is the category.
     */
    public Quest withFolderCategory(String path) {
        int slash = path.indexOf('/');
        boolean named = !"datapack".equals(category) && !(team && "team".equals(category));
        if (slash <= 0 || named) return this;
        return new Quest(title, description, path.substring(0, slash), mode, requires, repeatable, cooldownHours,
            sort, objectives, rewards, icon, permission, team, requiresMin, resetOnDeath, groups);
    }

    /** How many of the required quests must be done first: all of them, or requires_min. */
    public int needed() {
        return requiresMin.map(n -> Math.max(0, Math.min(n, requires.size()))).orElse(requires.size());
    }

    /** How many of the required quests are done. */
    public int requiredDone(java.util.function.Predicate<Identifier> done) {
        int count = 0;
        for (Identifier id : requires) if (done.test(id)) count++;
        return count;
    }

    /** Whether enough of the required quests are done to take this one. */
    public boolean unlocked(java.util.function.Predicate<Identifier> done) {
        return requiredDone(done) >= needed();
    }
}
