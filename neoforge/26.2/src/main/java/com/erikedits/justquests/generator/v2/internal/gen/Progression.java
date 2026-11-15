package com.erikedits.justquests.generator.v2.internal.gen;

import com.erikedits.justquests.generator.v2.api.WorldContext;
import com.erikedits.justquests.generator.v2.internal.catalog.Balance;
import com.erikedits.justquests.generator.v2.internal.catalog.ProfileDef;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Persisted progression snapshot (§9.7). Unlocks never go backwards; when nobody is online the
 * snapshot is used as-is (day thresholds still apply).
 */
public final class Progression {
    private boolean nether;
    private String netherReason = "";
    private boolean end;
    private String endReason = "";
    private final Map<String, String> moddedDims = new TreeMap<>();
    /** Milestones reached (0.6.0): id -> why. */
    private final Map<String, String> milestones = new TreeMap<>();
    private long maxGameDay;

    public boolean nether() {
        return nether;
    }

    public boolean end() {
        return end;
    }

    public long maxGameDay() {
        return maxGameDay;
    }

    public boolean dimensionUnlocked(String dim) {
        return moddedDims.containsKey(dim);
    }

    /** Whether the milestone (0.6.0) was reached. */
    public boolean milestone(String id) {
        return milestones.containsKey(id);
    }

    /** The highest tier a reached milestone gives its full weight, or -1. */
    public int milestoneTier(Balance balance) {
        int t = -1;
        for (Balance.Milestone m : balance.milestones) {
            if (m.tier() >= 0 && milestones.containsKey(m.id())) {
                t = Math.max(t, m.tier());
            }
        }
        return t;
    }

    /** The weight factor of an objective type while its milestone is not reached (1.0 when there is none). */
    public double typeFactor(Balance balance, String type) {
        for (Balance.Milestone m : balance.milestones) {
            if (type.equals(m.type()) && !milestones.containsKey(m.id())) {
                return m.before();
            }
        }
        return 1.0;
    }

    /** The milestones and why they were reached, for explain. */
    public Map<String, String> milestones() {
        return Map.copyOf(milestones);
    }

    /**
     * Updates the snapshot from the live world.
     *
     * @return true if anything was newly unlocked
     */
    public boolean update(WorldContext world, Balance balance, Collection<ProfileDef> activeProfiles) {
        boolean changed = false;
        long day = Math.max(0, safeDay(world));
        if (day > maxGameDay) {
            maxGameDay = day;
            changed = true;
        }
        if (!nether) {
            String why = unlockReason(world, balance.netherAdvancement, balance.netherUnlockShare, balance.netherUnlockDay);
            if (why != null) {
                nether = true;
                netherReason = why;
                changed = true;
            }
        }
        if (!end) {
            String why = null;
            for (String adv : balance.endAdvancements) {
                why = unlockReason(world, adv, balance.endUnlockShare, 0);
                if (why != null) {
                    break;
                }
            }
            if (why == null && balance.endUnlockDay > 0 && maxGameDay >= balance.endUnlockDay) {
                why = "day " + maxGameDay;
            }
            if (why != null) {
                end = true;
                endReason = why;
                changed = true;
            }
        }
        for (Balance.Milestone m : balance.milestones) {
            if (milestones.containsKey(m.id())) {
                continue;
            }
            String why = null;
            for (String adv : m.advancements()) {
                why = unlockReason(world, adv, m.share(), 0);
                if (why != null) {
                    break;
                }
            }
            if (why == null && m.day() > 0 && maxGameDay >= m.day()) {
                why = "day " + maxGameDay;
            }
            if (why != null) {
                milestones.put(m.id(), why);
                changed = true;
            }
        }
        for (ProfileDef p : activeProfiles) {
            for (ProfileDef.DimensionUnlock u : p.dimensions().values()) {
                if (moddedDims.containsKey(u.dimension())) {
                    continue;
                }
                String why = unlockReason(world, u.advancement(), u.share(), u.day());
                if (why != null) {
                    moddedDims.put(u.dimension(), why);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private String unlockReason(WorldContext world, String advancement, double share, long day) {
        if (advancement != null) {
            double s = safeShare(world, advancement);
            if (s >= 0 && s >= share && share >= 0) {
                return String.format(Locale.ROOT, "%.0f%% online have %s", s * 100.0, advancement);
            }
        }
        if (day > 0 && maxGameDay >= day) {
            return "day " + maxGameDay;
        }
        return null;
    }

    private static long safeDay(WorldContext world) {
        try {
            return world.gameDay();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static double safeShare(WorldContext world, String adv) {
        try {
            if (world.onlinePlayerCount() <= 0) {
                return -1;
            }
            return world.onlineShareWithAdvancement(adv);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** True if content of this tier may appear at all (ignores the difficulty cap). */
    public boolean tierUnlocked(int tier) {
        if (tier <= 2) {
            return true;
        }
        if (tier == 3) {
            return nether;
        }
        return end && nether;
    }

    /** Short text for explain ("always available", "unlocked: day 14"). */
    public String tierNote(int tier) {
        if (tier <= 2) {
            return "always available";
        }
        if (tier == 3) {
            return nether ? "unlocked: " + netherReason : "locked";
        }
        return end ? "unlocked: " + endReason : "locked";
    }

    public String dimensionNote(String dim) {
        String r = moddedDims.get(dim);
        return r == null ? "locked" : "unlocked: " + r;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("nether", nether);
        o.addProperty("netherReason", netherReason);
        o.addProperty("end", end);
        o.addProperty("endReason", endReason);
        o.addProperty("maxGameDay", maxGameDay);
        JsonObject dims = new JsonObject();
        moddedDims.forEach(dims::addProperty);
        o.add("dimensions", dims);
        JsonObject ms = new JsonObject();
        milestones.forEach(ms::addProperty);
        o.add("milestones", ms);
        return o;
    }

    public static Progression fromJson(JsonObject o) {
        Progression p = new Progression();
        if (o == null) {
            return p;
        }
        p.nether = Json.bool(o, "nether", false);
        p.netherReason = Json.str(o, "netherReason", "");
        p.end = Json.bool(o, "end", false);
        p.endReason = Json.str(o, "endReason", "");
        p.maxGameDay = Math.max(0, Json.lng(o, "maxGameDay", 0));
        JsonObject dims = Json.obj(o, "dimensions");
        if (dims != null) {
            dims.entrySet().forEach(e -> p.moddedDims.put(e.getKey(),
                e.getValue().isJsonPrimitive() ? e.getValue().getAsString() : "restored"));
        }
        JsonObject ms = Json.obj(o, "milestones");
        if (ms != null) {
            ms.entrySet().forEach(e -> p.milestones.put(e.getKey(),
                e.getValue().isJsonPrimitive() ? e.getValue().getAsString() : "restored"));
        }
        return p;
    }
}
