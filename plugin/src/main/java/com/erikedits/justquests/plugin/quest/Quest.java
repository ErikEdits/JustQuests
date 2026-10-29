package com.erikedits.justquests.plugin.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One quest, read from the mod's JSON format, so bundled quests, datapack quests and the mod's
 * custom-quests.json all work unchanged.
 *
 * @param mode       "all" objectives (default) or "any" one of them
 * @param requires   quest ids that must be completed first
 * @param cooldownHours wait before a repeatable quest can be taken again; 0 = none
 * @param sort       order within the category (lower first)
 * @param icon       item shown in the quest book; null = the first objective's item
 * @param permission node needed to see and take the quest; null = everyone
 * @param team       a team quest: one for the whole team, every member's actions count and every
 *                   member claims the rewards (category "team" unless one is given)
 * @param requiresMin how many of requires must be done first (any 12 of 16); -1 = all of them
 * @param resetOnDeath the progress goes back to zero when the player (team quest: any member) dies
 * @param groups     per objective the group it belongs to ("" = none); the objectives of one group
 *                   are alternatives, one of them is enough
 */
public record Quest(String id, LocalizedText title, LocalizedText description, String category, boolean any,
                    List<String> requires, boolean repeatable, int cooldownHours, int sort,
                    List<Objective> objectives, List<Reward> rewards, Material icon, String permission,
                    boolean team, int requiresMin, boolean resetOnDeath, List<String> groups) {

    /**
     * The first folder of a quest's path, used as its category when it names none: "ns:mining/iron"
     * (data/ns/justquests/quests/mining/iron.json or quests/mining/iron.json) is in "mining". Null
     * for a quest that is not in a folder.
     */
    static String folder(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        int slash = path.indexOf('/');
        return slash > 0 ? path.substring(0, slash) : null;
    }

    public static Quest parse(String id, JsonObject o) {
        if (!o.has("title")) throw new IllegalArgumentException("missing \"title\"");
        List<Objective> objectives = new ArrayList<>();
        List<String> groups = new ArrayList<>();
        if (o.has("objectives")) {
            for (JsonElement e : o.getAsJsonArray("objectives")) {
                JsonObject obj = e.getAsJsonObject();
                objectives.add(Objective.parse(obj));
                groups.add(obj.has("group") ? obj.get("group").getAsString() : "");
            }
        }
        if (objectives.isEmpty()) throw new IllegalArgumentException("no objectives");
        for (Objective obj : objectives) {
            if (obj.requiredCount() <= 0) throw new IllegalArgumentException("an objective has a count <= 0");
        }
        List<Reward> rewards = new ArrayList<>();
        if (o.has("rewards")) {
            for (JsonElement e : o.getAsJsonArray("rewards")) rewards.add(Reward.parse(e.getAsJsonObject()));
        }
        List<String> requires = new ArrayList<>();
        if (o.has("requires")) {
            for (JsonElement e : o.getAsJsonArray("requires")) requires.add(Matchers.ns(e.getAsString().toLowerCase(Locale.ROOT)));
        }
        Material icon = null;
        if (o.has("icon")) {
            icon = Registry.MATERIAL.get(Matchers.key(o.get("icon").getAsString()));
            if (icon != null && !icon.isItem()) icon = null;
        }
        boolean team = o.has("team") && o.get("team").getAsBoolean();
        return new Quest(id,
            LocalizedText.parse(o.get("title")),
            o.has("description") ? LocalizedText.parse(o.get("description")) : LocalizedText.EMPTY,
            o.has("category") ? o.get("category").getAsString() : folder(id) != null ? folder(id) : team ? "team" : "datapack",
            o.has("mode") && "any".equalsIgnoreCase(o.get("mode").getAsString()),
            List.copyOf(requires),
            o.has("repeatable") && o.get("repeatable").getAsBoolean(),
            o.has("cooldown_hours") ? o.get("cooldown_hours").getAsInt() : 0,
            o.has("sort") ? o.get("sort").getAsInt() : 0,
            List.copyOf(objectives), List.copyOf(rewards), icon,
            o.has("permission") ? o.get("permission").getAsString() : null, team,
            o.has("requires_min") ? o.get("requires_min").getAsInt() : -1,
            o.has("reset_on_death") && o.get("reset_on_death").getAsBoolean(),
            List.copyOf(groups));
    }

    static String string(JsonObject o, String field) {
        JsonElement e = o.get(field);
        if (e == null || !e.isJsonPrimitive()) throw new IllegalArgumentException("missing \"" + field + "\"");
        return e.getAsString();
    }

    /** The item for the quest book: the quest's icon, else its first objective's. */
    public Material iconOrGoal() {
        return icon != null ? icon : objectives.get(0).icon();
    }

    /** How many of the required quests must be done first: all of them, or requires_min. */
    public int needed() {
        return requiresMin < 0 ? requires.size() : Math.min(requiresMin, requires.size());
    }

    /** How many of the required quests are done. */
    public int requiredDone(java.util.function.Predicate<String> done) {
        int count = 0;
        for (String id : requires) if (done.test(id)) count++;
        return count;
    }

    /** Whether enough of the required quests are done to take this one. */
    public boolean unlocked(java.util.function.Predicate<String> done) {
        return requiredDone(done) >= needed();
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
        boolean one = false;
        for (int i = 0; i < objectives.size(); i++) {
            if (done.test(i)) one = true;
            else if (!any && !alternativeDone(i, done)) return false;
        }
        return !any || one;
    }

    /** The objectives in the order they are shown: the alternatives of a group together, where its first one is. */
    public List<Integer> shownOrder() {
        List<Integer> out = new ArrayList<>();
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
}
