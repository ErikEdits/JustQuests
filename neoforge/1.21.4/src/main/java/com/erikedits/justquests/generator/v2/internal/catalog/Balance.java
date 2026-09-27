package com.erikedits.justquests.generator.v2.internal.catalog;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * All tunable numbers (balance.json, §12). Every value has a code default so partial override files
 * work; the bundled file restates the defaults for the test phase.
 */
public final class Balance {

    /** Per-difficulty numbers. */
    public static final class Level {
        public double minMinutes;
        public double maxMinutes;
        public int maxTier;
        public double[] objectiveWeights;
        public double rewardRate;
        public double budgetMin;
        public double budgetMax;
        public boolean dimensionTravel;
        public double effectChance;
        public double lootChance;
        public double messageChance;
        public int claimExpiryHours;
        public double[] tierWeights;
        public Map<String, Double> typeWeights = new LinkedHashMap<>();

        /** Upper bound of the "quick" band (lower third of the range). */
        public double quickLimit(double fraction) {
            return minMinutes + (maxMinutes - minMinutes) * fraction;
        }
    }

    /** Tier weights applied while the world is young. */
    public record EarlyWorld(long maxDay, double[] tierWeights) {
    }

    public final Map<Difficulty, Level> levels = new EnumMap<>(Difficulty.class);
    public final Map<String, Double> typeWeights = new LinkedHashMap<>();
    public final Map<String, Double> hintMultipliers = new LinkedHashMap<>();
    public final Map<String, Double> dimensionOverhead = new LinkedHashMap<>();
    public double moddedDimensionOverhead = 5.0;
    public final Map<String, Integer> toolLevels = new LinkedHashMap<>();
    public double toolPenaltyPerLevel = 0.15;
    public double[] multiEffortFactor = {1.0, 1.0, 1.0};
    public double[] multiRewardPremium = {1.0, 1.1, 1.2};
    public double itemShare = 0.75;
    public double xpPointsPerValue = 12.0;
    public int minXp = 5;
    public int maxXp = 500;
    public double rewardTolerance = 0.2;
    public double netherUnlockShare = 0.25;
    public long netherUnlockDay = 10;
    public String netherAdvancement = "minecraft:story/enter_the_nether";
    public double endUnlockShare = 0.25;
    public long endUnlockDay = 40;
    public List<String> endAdvancements = List.of("minecraft:story/enter_the_end", "minecraft:end/root");
    public final List<EarlyWorld> earlyWorld = new ArrayList<>();
    public int minDistinctTypes = 3;
    public double maxTypeShare = 0.4;
    public boolean requireQuick = true;
    public double quickFraction = 1.0 / 3.0;
    public int minPerActiveModAt = 5;
    /** Largest share of a set that the one-quest-per-active-mod guarantee may claim (many mods, small N). */
    public double maxPerModShare = 0.5;
    public double typeRepeatPenalty = 0.45;
    public double themeChance = 0.85;
    public int calibrationMinSamples = 5;
    public double calibrationMin = 0.5;
    public double calibrationMax = 2.0;
    public double calibrationMaxStep = 0.10;
    public int maxAttemptsPerSlot = 40;
    public double rangeSlack = 0.25;
    public double minTargetFraction = 0.5;

    /** Parses balance JSON; missing values keep their defaults. */
    public static Balance parse(JsonObject root) {
        Balance b = new Balance();
        b.defaults();
        if (root == null) {
            return b;
        }
        JsonObject diffs = Json.obj(root, "difficulties");
        for (Difficulty d : Difficulty.values()) {
            JsonObject o = diffs == null ? null : Json.obj(diffs, d.name());
            if (o == null && diffs != null) {
                o = Json.obj(diffs, d.settingsValue());
            }
            if (o != null) {
                b.readLevel(b.levels.get(d), o);
            }
        }
        readDoubles(Json.obj(root, "typeWeights"), b.typeWeights);
        readDoubles(Json.obj(root, "hintMultipliers"), b.hintMultipliers);
        JsonObject dims = Json.obj(root, "dimensionOverheadMinutes");
        if (dims != null) {
            for (Map.Entry<String, JsonElement> e : dims.entrySet()) {
                if ("modded".equals(e.getKey())) {
                    b.moddedDimensionOverhead = asDouble(e.getValue(), b.moddedDimensionOverhead);
                } else {
                    b.dimensionOverhead.put(e.getKey(), asDouble(e.getValue(), 0));
                }
            }
        }
        JsonObject tools = Json.obj(root, "toolLevels");
        if (tools != null) {
            for (Map.Entry<String, JsonElement> e : tools.entrySet()) {
                b.toolLevels.put(e.getKey(), (int) asDouble(e.getValue(), 0));
            }
        }
        b.toolPenaltyPerLevel = Json.dbl(root, "toolPenaltyPerLevel", b.toolPenaltyPerLevel);
        JsonObject multi = Json.obj(root, "multiObjective");
        if (multi != null) {
            b.multiEffortFactor = doubles(Json.arr(multi, "effortFactor"), b.multiEffortFactor, 3);
            b.multiRewardPremium = doubles(Json.arr(multi, "rewardPremium"), b.multiRewardPremium, 3);
        }
        JsonObject rw = Json.obj(root, "rewards");
        if (rw != null) {
            b.itemShare = Json.dbl(rw, "itemShare", b.itemShare);
            b.xpPointsPerValue = Json.dbl(rw, "xpPointsPerValue", b.xpPointsPerValue);
            b.minXp = Json.integer(rw, "minXp", b.minXp);
            b.maxXp = Json.integer(rw, "maxXp", b.maxXp);
            b.rewardTolerance = Json.dbl(rw, "tolerance", b.rewardTolerance);
        }
        JsonObject prog = Json.obj(root, "progression");
        if (prog != null) {
            b.netherUnlockShare = Json.dbl(prog, "netherUnlockShare", b.netherUnlockShare);
            b.netherUnlockDay = Json.lng(prog, "netherUnlockDay", b.netherUnlockDay);
            b.netherAdvancement = Json.str(prog, "netherAdvancement", b.netherAdvancement);
            b.endUnlockShare = Json.dbl(prog, "endUnlockShare", b.endUnlockShare);
            b.endUnlockDay = Json.lng(prog, "endUnlockDay", b.endUnlockDay);
            List<String> adv = Json.strings(prog, "endAdvancements");
            if (!adv.isEmpty()) {
                b.endAdvancements = List.copyOf(adv);
            }
            List<JsonObject> early = Json.objects(prog, "earlyWorld");
            if (!early.isEmpty()) {
                b.earlyWorld.clear();
                for (JsonObject e : early) {
                    b.earlyWorld.add(new EarlyWorld(Json.lng(e, "maxDay", 0),
                        doubles(Json.arr(e, "tierWeights"), new double[]{1, 1, 1, 1, 1}, 5)));
                }
            }
        }
        JsonObject rules = Json.obj(root, "setRules");
        if (rules != null) {
            b.minDistinctTypes = Json.integer(rules, "minDistinctTypes", b.minDistinctTypes);
            b.maxTypeShare = Json.dbl(rules, "maxTypeShare", b.maxTypeShare);
            b.requireQuick = Json.bool(rules, "requireQuick", b.requireQuick);
            b.quickFraction = Json.dbl(rules, "quickFraction", b.quickFraction);
            b.minPerActiveModAt = Json.integer(rules, "minPerActiveModAt", b.minPerActiveModAt);
            b.maxPerModShare = Math.max(0.0, Math.min(1.0, Json.dbl(rules, "maxPerModShare", b.maxPerModShare)));
            b.typeRepeatPenalty = Json.dbl(rules, "typeRepeatPenalty", b.typeRepeatPenalty);
            b.themeChance = Json.dbl(rules, "themeChance", b.themeChance);
            b.maxAttemptsPerSlot = Json.integer(rules, "maxAttemptsPerSlot", b.maxAttemptsPerSlot);
            b.rangeSlack = Json.dbl(rules, "rangeSlack", b.rangeSlack);
            b.minTargetFraction = Json.dbl(rules, "minTargetFraction", b.minTargetFraction);
        }
        JsonObject cal = Json.obj(root, "calibration");
        if (cal != null) {
            b.calibrationMinSamples = Json.integer(cal, "minSamples", b.calibrationMinSamples);
            b.calibrationMin = Json.dbl(cal, "minMultiplier", b.calibrationMin);
            b.calibrationMax = Json.dbl(cal, "maxMultiplier", b.calibrationMax);
            b.calibrationMaxStep = Json.dbl(cal, "maxStepPerCycle", b.calibrationMaxStep);
        }
        b.sanitize();
        return b;
    }

    private void readLevel(Level l, JsonObject o) {
        JsonArray range = Json.arr(o, "targetMinutes");
        if (range != null && range.size() == 2) {
            l.minMinutes = asDouble(range.get(0), l.minMinutes);
            l.maxMinutes = asDouble(range.get(1), l.maxMinutes);
        }
        l.maxTier = Json.integer(o, "maxTier", l.maxTier);
        l.objectiveWeights = doubles(Json.arr(o, "objectiveWeights"), l.objectiveWeights, 3);
        l.rewardRate = Json.dbl(o, "rewardRate", l.rewardRate);
        JsonArray clamp = Json.arr(o, "budgetClamp");
        if (clamp != null && clamp.size() == 2) {
            l.budgetMin = asDouble(clamp.get(0), l.budgetMin);
            l.budgetMax = asDouble(clamp.get(1), l.budgetMax);
        }
        l.dimensionTravel = Json.bool(o, "dimensionTravel", l.dimensionTravel);
        l.effectChance = Json.dbl(o, "effectChance", l.effectChance);
        l.lootChance = Json.dbl(o, "lootTableChance", l.lootChance);
        l.messageChance = Json.dbl(o, "messageChance", l.messageChance);
        l.claimExpiryHours = Json.integer(o, "claimExpiryHours", l.claimExpiryHours);
        l.tierWeights = doubles(Json.arr(o, "tierWeights"), l.tierWeights, 5);
        readDoubles(Json.obj(o, "typeWeights"), l.typeWeights);
    }

    private void defaults() {
        levels.put(Difficulty.EASY, level(4, 10, 1, new double[]{100, 0, 0}, 1.0, 3, 15, false, 0, 0, 0.10,
            new double[]{1.0, 0.7, 0, 0, 0}));
        levels.put(Difficulty.NORMAL, level(8, 20, 2, new double[]{75, 25, 0}, 1.2, 8, 30, true, 0.05, 0.03, 0.08,
            new double[]{0.7, 1.0, 0.8, 0.8, 0.6}));
        levels.put(Difficulty.HARD, level(15, 35, 4, new double[]{50, 35, 15}, 1.5, 18, 60, true, 0.20, 0.15, 0.05,
            new double[]{0.35, 0.8, 1.0, 1.0, 1.0}));
        String[] types = {"collect_item", "mine_block", "craft_item", "smelt_item", "kill_mob", "breed_animal",
            "tame_animal", "consume_item", "place_block", "visit_dimension"};
        double[] w = {1.0, 1.0, 1.0, 0.8, 1.0, 0.6, 0.25, 0.6, 0.35, 0.15};
        for (int i = 0; i < types.length; i++) {
            typeWeights.put(types[i], w[i]);
        }
        toolLevels.put("none", 0);
        toolLevels.put("wood", 0);
        toolLevels.put("fishing_rod", 0);
        toolLevels.put("knife", 0);
        toolLevels.put("stone", 1);
        toolLevels.put("shears", 1);
        toolLevels.put("iron", 2);
        toolLevels.put("diamond", 3);
        toolLevels.put("silk_touch", 3);
        toolLevels.put("netherite", 4);
        earlyWorld.add(new EarlyWorld(2, new double[]{1.0, 0.6, 0.15, 0.05, 0.05}));
        earlyWorld.add(new EarlyWorld(6, new double[]{1.0, 1.0, 0.6, 0.5, 0.3}));
    }

    private static Level level(double min, double max, int maxTier, double[] objW, double rate, double bMin, double bMax,
                               boolean travel, double effect, double loot, double message, double[] tierW) {
        Level l = new Level();
        l.minMinutes = min;
        l.maxMinutes = max;
        l.maxTier = maxTier;
        l.objectiveWeights = objW;
        l.rewardRate = rate;
        l.budgetMin = bMin;
        l.budgetMax = bMax;
        l.dimensionTravel = travel;
        l.effectChance = effect;
        l.lootChance = loot;
        l.messageChance = message;
        l.claimExpiryHours = 0;
        l.tierWeights = tierW;
        return l;
    }

    private void sanitize() {
        for (Level l : levels.values()) {
            l.minMinutes = Math.max(0.5, l.minMinutes);
            l.maxMinutes = Math.max(l.minMinutes, l.maxMinutes);
            l.maxTier = Math.max(0, Math.min(4, l.maxTier));
            l.budgetMin = Math.max(0.1, l.budgetMin);
            l.budgetMax = Math.max(l.budgetMin, l.budgetMax);
            l.claimExpiryHours = Math.max(0, l.claimExpiryHours);
        }
        maxTypeShare = Math.max(0.05, Math.min(1.0, maxTypeShare));
        quickFraction = Math.max(0.05, Math.min(1.0, quickFraction));
        itemShare = Math.max(0.0, Math.min(1.0, itemShare));
        xpPointsPerValue = Math.max(0.1, xpPointsPerValue);
        maxXp = Math.max(minXp, maxXp);
        rewardTolerance = Math.max(0.01, rewardTolerance);
        calibrationMin = Math.max(0.1, calibrationMin);
        calibrationMax = Math.max(calibrationMin, calibrationMax);
        maxAttemptsPerSlot = Math.max(4, Math.min(500, maxAttemptsPerSlot));
    }

    public Level level(Difficulty d) {
        return levels.get(d);
    }

    public double typeWeight(Difficulty d, ObjectiveType t) {
        double base = typeWeights.getOrDefault(t.shortName(), 1.0);
        Double mult = levels.get(d).typeWeights.get(t.shortName());
        return base * (mult == null ? 1.0 : mult);
    }

    public int toolLevel(String tool) {
        if (tool == null) {
            return 0;
        }
        Integer v = toolLevels.get(tool.toLowerCase(Locale.ROOT));
        return v == null ? 0 : v;
    }

    /** Tier weights for the world's age (earliest matching early-world band, else all 1). */
    public double[] earlyWorldWeights(long gameDay) {
        for (EarlyWorld e : earlyWorld) {
            if (gameDay <= e.maxDay()) {
                return e.tierWeights();
            }
        }
        return new double[]{1, 1, 1, 1, 1};
    }

    public Map<String, Double> hintMultipliers() {
        return Collections.unmodifiableMap(hintMultipliers);
    }

    private static void readDoubles(JsonObject o, Map<String, Double> into) {
        if (o == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            into.put(e.getKey(), asDouble(e.getValue(), 1.0));
        }
    }

    private static double asDouble(JsonElement e, double def) {
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : def;
    }

    private static double[] doubles(JsonArray a, double[] def, int len) {
        if (a == null) {
            return def;
        }
        double[] out = new double[len];
        for (int i = 0; i < len; i++) {
            out[i] = i < a.size() ? asDouble(a.get(i), i < def.length ? def[i] : 0) : (i < def.length ? def[i] : 0);
        }
        return out;
    }
}
