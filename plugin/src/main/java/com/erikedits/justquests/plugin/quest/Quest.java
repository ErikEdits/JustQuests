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
 */
public record Quest(String id, LocalizedText title, LocalizedText description, String category, boolean any,
                    List<String> requires, boolean repeatable, int cooldownHours, int sort,
                    List<Objective> objectives, List<Reward> rewards, Material icon, String permission) {

    public static Quest parse(String id, JsonObject o) {
        if (!o.has("title")) throw new IllegalArgumentException("missing \"title\"");
        List<Objective> objectives = new ArrayList<>();
        if (o.has("objectives")) {
            for (JsonElement e : o.getAsJsonArray("objectives")) objectives.add(Objective.parse(e.getAsJsonObject()));
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
        return new Quest(id,
            LocalizedText.parse(o.get("title")),
            o.has("description") ? LocalizedText.parse(o.get("description")) : LocalizedText.EMPTY,
            o.has("category") ? o.get("category").getAsString() : "datapack",
            o.has("mode") && "any".equalsIgnoreCase(o.get("mode").getAsString()),
            List.copyOf(requires),
            o.has("repeatable") && o.get("repeatable").getAsBoolean(),
            o.has("cooldown_hours") ? o.get("cooldown_hours").getAsInt() : 0,
            o.has("sort") ? o.get("sort").getAsInt() : 0,
            List.copyOf(objectives), List.copyOf(rewards), icon,
            o.has("permission") ? o.get("permission").getAsString() : null);
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
}
