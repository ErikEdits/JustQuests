package com.erikedits.justquests.generator.v2.internal.gen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** A quest under construction (objectives, texts, rewards and explain data). */
public final class QuestDraft {
    /**
     * One objective.
     *
     * @param c       candidate
     * @param count   count (1 for visit_dimension)
     * @param minutes estimated minutes without travel overhead
     */
    public record Objective(Candidate c, int count, double minutes) {
    }

    /**
     * One reward.
     *
     * @param type      short reward type ({@code give_item}, {@code xp}, {@code effect}, {@code loot_table}, {@code message})
     * @param id        item/effect/loot table id, or null
     * @param count     item count, XP points, or effect seconds
     * @param amplifier effect amplifier
     * @param text      message text or null
     * @param value     value units
     */
    public record Reward(String type, String id, int count, int amplifier, String text, double value) {
    }

    public final List<Objective> objectives = new ArrayList<>();
    public final List<Reward> rewards = new ArrayList<>();
    public final List<String> explain = new ArrayList<>();
    public String title;
    public String description;
    /** Per-language titles, descriptions and message reward text ({@code en_us} first); see TextBuilder.localize. */
    public java.util.Map<String, String> titles = java.util.Map.of();
    public java.util.Map<String, String> descriptions = java.util.Map.of();
    public java.util.Map<String, String> messageTexts = java.util.Map.of();
    /** Where the English title came from (TextBuilder.SRC_*) and its index, for rendering other languages. */
    public int titleSource = -1;
    public int titleIndex = -1;
    /** Phrase template index per objective (-1 = default phrase) and theme description index (-1 = none). */
    public int[] phraseChoice;
    public int themeDescriptionChoice = -1;
    public String themeKey;
    public List<String> themeNames = List.of();
    public List<String> themeDescriptions = List.of();
    public com.google.gson.JsonObject json;
    public String comboFamily;
    public double targetMinutes;
    public double overhead;
    public double estMinutes;
    public double budget;
    public double rewardValue;
    public boolean quick;

    public int tier() {
        int t = 0;
        for (Objective o : objectives) {
            t = Math.max(t, o.c().tier());
        }
        return t;
    }

    public Set<String> families() {
        Set<String> s = new LinkedHashSet<>();
        for (Objective o : objectives) {
            s.add(o.c().family());
        }
        return s;
    }

    public Set<String> types() {
        Set<String> s = new LinkedHashSet<>();
        for (Objective o : objectives) {
            s.add(o.c().type().shortName());
        }
        return s;
    }

    public Set<String> profiles() {
        Set<String> s = new TreeSet<>();
        for (Objective o : objectives) {
            s.add(o.c().profile());
        }
        return s;
    }

    public Set<String> targets() {
        Set<String> s = new LinkedHashSet<>();
        for (Objective o : objectives) {
            s.add(o.c().target());
        }
        return s;
    }

    public boolean modded() {
        for (Objective o : objectives) {
            if (o.c().modded()) {
                return true;
            }
        }
        return false;
    }

    /** Order-independent content signature without counts (§11.4). */
    public String signature() {
        return signatureOf(objectives);
    }

    public static String signatureOf(List<Objective> objs) {
        TreeSet<String> parts = new TreeSet<>();
        for (Objective o : objs) {
            parts.add(o.c().signaturePart());
        }
        return String.join("|", parts);
    }

    /** Value of all rewards. */
    public double totalRewardValue() {
        double v = 0;
        for (Reward r : rewards) {
            v += r.value();
        }
        return v;
    }
}
