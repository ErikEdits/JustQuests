package com.erikedits.justquests.generator.v2.internal.gen;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GenLog;
import com.erikedits.justquests.generator.v2.api.HostCapabilities;
import com.erikedits.justquests.generator.v2.api.ValidationResult;
import com.erikedits.justquests.generator.v2.internal.catalog.Balance;
import com.erikedits.justquests.generator.v2.internal.catalog.Catalog;
import com.erikedits.justquests.generator.v2.internal.catalog.ObjectiveType;
import com.erikedits.justquests.generator.v2.internal.catalog.ProfileDef;
import com.erikedits.justquests.generator.v2.internal.catalog.ThemeDef;
import com.erikedits.justquests.generator.v2.internal.util.NiceNumbers;
import com.erikedits.justquests.generator.v2.internal.util.Rng;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Composes a varied set of quests (§9.5): slot planning (objective counts, one quick slot, modded
 * share and one quest per active mod), themes and same-family combos for multi-objective quests,
 * set rules, and graceful relaxation in the documented order
 * family → type share → modded share → history. Never loops forever, never throws; returns fewer
 * quests when the pool is too small.
 */
public final class SetBuilder {
    /** Host validation hook: builds the final id from the ordinal and runs the mod's codec. */
    public interface Validator {
        ValidationResult validate(QuestDraft draft, JsonObject json, int ordinal);
    }

    /** Relaxation levels in order. */
    public static final String[] RELAX_NAMES = {"none", "family", "type_share", "modded_share", "history"};
    /** Largest tier difference between the objectives of one themed quest. */
    static final int THEME_TIER_SPAN = 2;

    private enum Mode { ANY, VANILLA, ANY_MOD, PROFILE }

    private record Slot(int objectives, boolean quick, Mode mode, String profile) {
    }

    /**
     * A quest that stays in the set (reroll) and counts toward the set rules.
     *
     * @param families   families used
     * @param targets    resolved targets
     * @param signature  content signature
     * @param types      objective short type names
     * @param title      title
     * @param quick      in the quick band
     * @param profiles   profiles used
     * @param modded     uses modded content
     */
    public record Kept(Set<String> families, Set<String> targets, String signature, Set<String> types, String title,
                       boolean quick, Set<String> profiles, boolean modded) {
        public static Kept of(QuestDraft d) {
            return new Kept(d.families(), d.targets(), d.signature(), d.types(), d.title, d.quick, d.profiles(), d.modded());
        }
    }

    private final Catalog catalog;
    private final Balance balance;
    private final Balance.Level level;
    private final Difficulty difficulty;
    private final CandidateResolver.Pool pool;
    private final Rng rng;
    private final Set<String> history;
    private final RewardBuilder rewards;
    private final TextBuilder text;
    private final Validator validator;
    private final GenLog log;
    private final HostCapabilities caps;
    private final Progression progression;
    private final double[] tierWeights = new double[5];
    private final Set<String> activeIds;

    private final Map<String, Long> relaxations = new LinkedHashMap<>();
    private final Map<String, Long> rejections = new LinkedHashMap<>();
    private final List<String> relaxNotes = new ArrayList<>();
    private final Set<String> badSignatures = new HashSet<>();

    // current set state
    private final Set<String> usedFamilies = new HashSet<>();
    private final Set<String> usedTargets = new HashSet<>();
    private final Set<String> usedSignatures = new HashSet<>();
    private final Set<String> usedTitles = new HashSet<>();
    private final Map<ObjectiveType, Integer> typeCounts = new EnumMap<>(ObjectiveType.class);
    private int maxPerType;
    private double quickLimit;
    private int acceptedCount;

    public SetBuilder(Catalog catalog, Difficulty difficulty, CandidateResolver.Pool pool, Rng rng, Set<String> history,
                      RewardBuilder rewards, TextBuilder text, Validator validator, GenLog log, HostCapabilities caps,
                      Progression progression) {
        this.catalog = catalog;
        this.balance = catalog.balance;
        this.level = balance.level(difficulty);
        this.difficulty = difficulty;
        this.pool = pool;
        this.rng = rng;
        this.history = history;
        this.rewards = rewards;
        this.text = text;
        this.validator = validator;
        this.log = log;
        this.caps = caps;
        this.progression = progression;
        double[] early = balance.earlyWorldWeights(progression.maxGameDay());
        for (int i = 0; i < 5; i++) {
            double lw = i < level.tierWeights.length ? level.tierWeights[i] : 1.0;
            tierWeights[i] = lw * (i < early.length ? early[i] : 1.0);
        }
        this.activeIds = CandidateResolver.ids(pool.activeProfiles());
        this.quickLimit = level.quickLimit(balance.quickFraction);
    }

    public Map<String, Long> relaxations() {
        return relaxations;
    }

    public List<String> relaxNotes() {
        return relaxNotes;
    }

    public Map<String, Long> rejections() {
        return rejections;
    }

    public double quickLimit() {
        return quickLimit;
    }

    /**
     * Builds {@code n} new quests next to {@code kept} ones (reroll keeps claimed/completed quests).
     *
     * @param n           quests to create
     * @param kept        quests that stay in the set (count toward the set rules)
     * @param totalSize   size of the whole set (kept + new) the rules refer to
     * @param moddedShare target share of quests using modded content
     * @return accepted drafts in generation order (JSON in {@code draft.json})
     */
    public List<QuestDraft> build(int n, List<Kept> kept, int totalSize, double moddedShare) {
        maxPerType = Math.max(1, (int) Math.floor(balance.maxTypeShare * totalSize + 1e-9));
        for (Kept k : kept) {
            register(k);
        }
        long blocked = 0;
        for (Candidate c : pool.candidates()) {
            if (history.contains(c.signaturePart())) {
                blocked++;
            }
        }
        if (blocked > 0) {
            rejections.merge(CandidateResolver.STEP_HISTORY, blocked, Long::sum);
        }
        boolean keptQuick = false;
        for (Kept k : kept) {
            keptQuick |= k.quick();
        }
        List<Slot> slots = planSlots(n, kept, totalSize, moddedShare, !keptQuick && balance.requireQuick);
        List<QuestDraft> accepted = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            QuestDraft d = fillSlot(slots.get(i), i, null);
            if (d != null) {
                accepted.add(d);
                register(Kept.of(d));
                acceptedCount++;
            }
        }
        repairDistinctTypes(accepted, kept, totalSize);
        if (balance.requireQuick && !accepted.isEmpty()) {
            boolean anyQuick = keptQuick;
            for (QuestDraft d : accepted) {
                anyQuick |= d.quick;
            }
            if (!anyQuick) {
                relax("quick", "no quest fits the quick band");
            }
        }
        return accepted;
    }

    // ------------------------------------------------------------------ planning

    private List<Slot> planSlots(int n, List<Kept> kept, int totalSize, double moddedShare, boolean needQuick) {
        List<String> activeMods = new ArrayList<>();
        for (ProfileDef p : pool.activeProfiles()) {
            if (!p.isVanilla() && hasCandidates(p.id())) {
                activeMods.add(p.id());
            }
        }
        List<Mode> modes = new ArrayList<>();
        List<String> profiles = new ArrayList<>();
        if (activeMods.isEmpty()) {
            for (int i = 0; i < n; i++) {
                modes.add(Mode.ANY);
                profiles.add(null);
            }
        } else if (moddedShare <= 0) {
            for (int i = 0; i < n; i++) {
                modes.add(Mode.VANILLA);
                profiles.add(null);
            }
        } else {
            int keptModded = 0;
            Set<String> covered = new HashSet<>();
            for (Kept k : kept) {
                if (k.modded()) {
                    keptModded++;
                }
                covered.addAll(k.profiles());
            }
            List<String> required = new ArrayList<>();
            if (totalSize >= balance.minPerActiveModAt) {
                for (String m : activeMods) {
                    if (!covered.contains(m)) {
                        required.add(m);
                    }
                }
            }
            int target = (int) Math.round(totalSize * moddedShare) - keptModded;
            // with more mods than the set can carry, the guarantee covers a rotating subset (the cycle's
            // RNG picks it) and never takes more than maxPerModShare of the set or the modded target
            int cap = Math.max(target, (int) Math.ceil(n * balance.maxPerModShare));
            if (required.size() > cap) {
                for (int i = required.size() - 1; i > 0; i--) {
                    int j = rng.nextInt(i + 1);
                    String x = required.get(i);
                    required.set(i, required.get(j));
                    required.set(j, x);
                }
                relax("per_mod", required.size() + " active mods, " + Math.max(0, cap) + " covered this cycle");
                required = new ArrayList<>(required.subList(0, Math.max(0, cap)));
            }
            int modded = Math.max(0, Math.min(n, Math.max(target, required.size())));
            for (int i = 0; i < n; i++) {
                if (i < Math.min(required.size(), modded)) {
                    modes.add(Mode.PROFILE);
                    profiles.add(required.get(i));
                } else if (i < modded) {
                    modes.add(Mode.ANY_MOD);
                    profiles.add(null);
                } else {
                    modes.add(Mode.VANILLA);
                    profiles.add(null);
                }
            }
        }
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            slots.add(new Slot(sampleObjectiveCount(), false, modes.get(i), profiles.get(i)));
        }
        for (int i = slots.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Slot x = slots.get(i);
            slots.set(i, slots.get(j));
            slots.set(j, x);
        }
        if (needQuick && !slots.isEmpty()) {
            Slot s = slots.get(0);
            slots.set(0, new Slot(1, true, s.mode(), s.profile()));
        }
        return slots;
    }

    private boolean hasCandidates(String profile) {
        for (Candidate c : pool.candidates()) {
            if (c.profile().equals(profile)) {
                return true;
            }
        }
        return false;
    }

    private int sampleObjectiveCount() {
        double[] w = level.objectiveWeights;
        double total = 0;
        for (double x : w) {
            total += Math.max(0, x);
        }
        if (total <= 0) {
            return 1;
        }
        double r = rng.nextDouble() * total;
        for (int i = 0; i < w.length; i++) {
            r -= Math.max(0, w[i]);
            if (r < 0) {
                return i + 1;
            }
        }
        return 1;
    }

    // ------------------------------------------------------------------ slot filling

    private QuestDraft fillSlot(Slot slot, int index, Set<ObjectiveType> requireNewType) {
        int passes = slot.quick() ? 2 : 1;
        for (int pass = 0; pass < passes; pass++) {
            boolean quick = slot.quick() && pass == 0;
            for (int lvl = 0; lvl < RELAX_NAMES.length; lvl++) {
                List<Candidate> singles = eligible(slot, lvl, quick, requireNewType);
                boolean multiPossible = !quick && slot.objectives() > 1 && requireNewType == null;
                if (singles.isEmpty() && !multiPossible) {
                    continue;
                }
                for (int a = 0; a < balance.maxAttemptsPerSlot; a++) {
                    QuestDraft d = tryBuild(slot, quick, lvl, singles, requireNewType);
                    if (d == null || !finish(d)) {
                        continue;
                    }
                    if (lvl > 0) {
                        for (int r = 1; r <= lvl; r++) {
                            relax(RELAX_NAMES[r], "slot " + index);
                        }
                    }
                    if (slot.quick() && !quick) {
                        relax("quick", "slot " + index);
                    }
                    return d;
                }
            }
        }
        relax("slot_skipped", "slot " + index + " (" + slot.mode() + ")");
        return null;
    }

    private List<Candidate> eligible(Slot slot, int lvl, boolean quick, Set<ObjectiveType> requireNewType) {
        List<Candidate> out = new ArrayList<>();
        for (Candidate c : pool.candidates()) {
            if (!setEligible(c, lvl)) {
                continue;
            }
            if (lvl < 3 && !fitsMode(slot, c.modded(), c.profile())) {
                continue;
            }
            if (lvl < 4 && history.contains(c.signaturePart())) {
                continue;
            }
            if (quick && c.minMinutes() > quickLimit) {
                continue;
            }
            if (requireNewType != null && requireNewType.contains(c.type())) {
                continue;
            }
            out.add(c);
        }
        return out;
    }

    /** Set-level rules that apply to every objective of a candidate quest. */
    private boolean setEligible(Candidate c, int lvl) {
        if (usedTargets.contains(c.target()) || badSignatures.contains(c.signaturePart())) {
            return false;
        }
        if (lvl < 1 && usedFamilies.contains(c.family())) {
            return false;
        }
        return lvl >= 2 || typeCounts.getOrDefault(c.type(), 0) < maxPerType;
    }

    private static boolean fitsMode(Slot slot, boolean modded, String profile) {
        switch (slot.mode()) {
            case VANILLA:
                return !modded;
            case ANY_MOD:
                return modded;
            case PROFILE:
                return slot.profile().equals(profile);
            default:
                return true;
        }
    }

    private boolean questFitsMode(Slot slot, List<QuestDraft.Objective> objs) {
        switch (slot.mode()) {
            case VANILLA:
                for (QuestDraft.Objective o : objs) {
                    if (o.c().modded()) {
                        return false;
                    }
                }
                return true;
            case ANY_MOD:
                for (QuestDraft.Objective o : objs) {
                    if (o.c().modded()) {
                        return true;
                    }
                }
                return false;
            case PROFILE:
                for (QuestDraft.Objective o : objs) {
                    if (o.c().profile().equals(slot.profile())) {
                        return true;
                    }
                }
                return false;
            default:
                return true;
        }
    }

    private double weight(Candidate c) {
        int tier = Math.max(0, Math.min(4, c.tier()));
        double w = c.weight() * balance.typeWeight(difficulty, c.type()) * tierWeights[tier];
        int used = typeCounts.getOrDefault(c.type(), 0);
        for (int i = 0; i < used; i++) {
            w *= balance.typeRepeatPenalty;
        }
        return w;
    }

    private QuestDraft tryBuild(Slot slot, boolean quick, int lvl, List<Candidate> singles, Set<ObjectiveType> requireNewType) {
        double targetMinutes = quick ? rng.range(level.minMinutes, quickLimit) : rng.range(level.minMinutes, level.maxMinutes);
        int k = quick ? 1 : slot.objectives();
        if (k >= 2 && requireNewType == null) {
            QuestDraft d = null;
            if (rng.chance(balance.themeChance)) {
                d = tryTheme(slot, k, lvl, targetMinutes);
            }
            if (d == null) {
                d = tryCombo(slot, lvl, targetMinutes, singles);
            }
            if (d != null) {
                return d;
            }
        }
        if (singles.isEmpty()) {
            return null;
        }
        final double want = targetMinutes;
        Candidate c = rng.weighted(singles, x -> weight(x) * capacity(x, want));
        if (c == null) {
            return null;
        }
        QuestDraft d = new QuestDraft();
        d.targetMinutes = targetMinutes;
        d.overhead = c.overhead();
        d.objectives.add(size(c, targetMinutes - c.overhead()));
        return checkRange(d, quick, lvl) ? d : null;
    }

    /** Prefers candidates whose count range can actually reach the target minutes. */
    private static double capacity(Candidate c, double targetMinutes) {
        double f = Math.min(1.0, c.maxMinutes() / Math.max(0.1, targetMinutes));
        return f * f;
    }

    private QuestDraft.Objective size(Candidate c, double minutes) {
        if (!c.type().counted()) {
            return new QuestDraft.Objective(c, 1, c.effortPerUnit());
        }
        double per = c.effortPerUnit();
        double raw = Math.max(0.0, minutes) / per;
        int count = NiceNumbers.round(raw, c.min(), c.max(), c.stack(), balance.countCapScale != 1.0);
        return new QuestDraft.Objective(c, count, count * per);
    }

    private boolean checkRange(QuestDraft d, boolean quick, int lvl) {
        double sum = d.overhead;
        for (QuestDraft.Objective o : d.objectives) {
            sum += o.minutes();
        }
        int k = d.objectives.size();
        if (k > 1) {
            sum *= balance.multiEffortFactor[Math.min(2, k - 1)];
        }
        d.estMinutes = sum;
        String sig = d.signature();
        if (usedSignatures.contains(sig) || badSignatures.contains(sig)) {
            return false;
        }
        if (lvl < 4 && history.contains(sig)) {
            return false;
        }
        if (quick) {
            d.quick = sum <= quickLimit + 1e-9;
            return d.quick;
        }
        double lo = Math.max(level.minMinutes * (1.0 - balance.rangeSlack), d.targetMinutes * balance.minTargetFraction);
        double hi = level.maxMinutes * (1.0 + balance.rangeSlack);
        d.quick = sum <= quickLimit + 1e-9;
        return sum >= lo && sum <= hi;
    }

    private QuestDraft tryTheme(Slot slot, int k, int lvl, double targetMinutes) {
        List<ThemeDef> themes = new ArrayList<>();
        for (ThemeDef t : catalog.themes) {
            if (t.minDifficulty() <= difficulty.ordinal() && t.maxDifficulty() >= difficulty.ordinal()
                && activeIds.containsAll(t.requiresProfiles()) && t.requiredSlots() <= k && t.weight() > 0) {
                themes.add(t);
            }
        }
        ThemeDef theme = rng.weighted(themes, ThemeDef::weight);
        if (theme == null) {
            return null;
        }
        int optionalBudget = k - theme.requiredSlots();
        List<Candidate> chosen = new ArrayList<>();
        String dimension = null;
        int firstTier = -1;
        for (ThemeDef.Slot s : theme.slots()) {
            if (s.optional()) {
                if (optionalBudget <= 0) {
                    continue;
                }
            }
            List<Candidate> options = new ArrayList<>();
            for (Candidate c : pool.candidates()) {
                if (!matches(s, c) || !setEligible(c, lvl) || (lvl < 4 && history.contains(c.signaturePart()))) {
                    continue;
                }
                // a theme may ask for two targets of one entry ("mine two stone layers"); only the
                // same target twice is a duplicate
                boolean dup = false;
                for (Candidate x : chosen) {
                    if (x.target().equals(c.target())) {
                        dup = true;
                        break;
                    }
                }
                if (dup) {
                    continue;
                }
                String dim = c.dimension() == null ? "minecraft:overworld" : c.dimension();
                if (dimension != null && !dimension.equals(dim)) {
                    continue;
                }
                // themes are curated combinations, so they may span two tiers (lapis and bookshelves)
                if (firstTier >= 0 && Math.abs(c.tier() - firstTier) > THEME_TIER_SPAN) {
                    continue;
                }
                options.add(c);
            }
            Candidate pick = rng.weighted(options, this::weight);
            if (pick == null) {
                if (s.optional()) {
                    continue;
                }
                return null;
            }
            if (s.optional()) {
                optionalBudget--;
            }
            chosen.add(pick);
            if (dimension == null) {
                dimension = pick.dimension() == null ? "minecraft:overworld" : pick.dimension();
                firstTier = pick.tier();
            }
        }
        if (chosen.size() < 2) {
            return null;
        }
        QuestDraft d = sizeMulti(chosen, targetMinutes);
        if (!multiAllowed(slot, d, lvl)) {
            return null;
        }
        d.themeKey = theme.key();
        d.themeNames = theme.names();
        d.themeDescriptions = theme.descriptions();
        return checkRange(d, false, lvl) ? d : null;
    }

    private static boolean matches(ThemeDef.Slot s, Candidate c) {
        return (s.types().isEmpty() || s.types().contains(c.type()))
            && (s.keys().isEmpty() || s.keys().contains(c.entry().key()))
            && (s.families().isEmpty() || s.families().contains(c.family()))
            && (s.profiles().isEmpty() || s.profiles().contains(c.profile()));
    }

    private QuestDraft tryCombo(Slot slot, int lvl, double targetMinutes, List<Candidate> singles) {
        if (singles.isEmpty()) {
            return null;
        }
        Candidate first = rng.weighted(singles, this::weight);
        if (first == null) {
            return null;
        }
        List<Candidate> partners = new ArrayList<>();
        for (Candidate c : pool.candidates()) {
            if (c == first || !c.family().equals(first.family()) || c.target().equals(first.target())
                || c.type() == ObjectiveType.VISIT_DIMENSION || first.type() == ObjectiveType.VISIT_DIMENSION) {
                continue;
            }
            if (c.entry() == first.entry() && c.type() == first.type()) {
                continue;
            }
            String d1 = first.dimension() == null ? "" : first.dimension();
            String d2 = c.dimension() == null ? "" : c.dimension();
            if (!d1.equals(d2) || Math.abs(c.tier() - first.tier()) > 1) {
                continue;
            }
            if (!setEligible(c, lvl) || (lvl < 4 && history.contains(c.signaturePart()))) {
                continue;
            }
            partners.add(c);
        }
        Candidate second = rng.weighted(partners, this::weight);
        if (second == null) {
            return null;
        }
        List<Candidate> chosen = new ArrayList<>();
        chosen.add(first);
        chosen.add(second);
        QuestDraft d = sizeMulti(chosen, targetMinutes);
        if (!multiAllowed(slot, d, lvl)) {
            return null;
        }
        d.comboFamily = first.family();
        return checkRange(d, false, lvl) ? d : null;
    }

    private QuestDraft sizeMulti(List<Candidate> chosen, double targetMinutes) {
        QuestDraft d = new QuestDraft();
        d.targetMinutes = targetMinutes;
        double overhead = 0;
        for (Candidate c : chosen) {
            overhead = Math.max(overhead, c.overhead());
        }
        d.overhead = overhead;
        double avail = Math.max(0.0, targetMinutes - overhead);
        double fixed = 0;
        int counted = 0;
        for (Candidate c : chosen) {
            if (c.type().counted()) {
                counted++;
            } else {
                fixed += c.effortPerUnit();
            }
        }
        double perObjective = counted == 0 ? 0 : Math.max(0.0, avail - fixed) / counted;
        for (Candidate c : chosen) {
            double jitter = rng.range(0.85, 1.15);
            d.objectives.add(size(c, perObjective * jitter));
        }
        return d;
    }

    private boolean multiAllowed(Slot slot, QuestDraft d, int lvl) {
        if (lvl < 3 && !questFitsMode(slot, d.objectives)) {
            return false;
        }
        if (lvl < 1) {
            for (String f : d.families()) {
                if (usedFamilies.contains(f)) {
                    return false;
                }
            }
        }
        if (lvl < 2) {
            for (String t : d.types()) {
                ObjectiveType ot = ObjectiveType.parse(t);
                if (typeCounts.getOrDefault(ot, 0) >= maxPerType) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ finishing

    private boolean finish(QuestDraft d) {
        if (!rewards.build(d, rng)) {
            return false;
        }
        String title = text.title(d, usedTitles, rng);
        if (title == null) {
            return false;
        }
        d.title = title;
        d.description = text.description(d, d.themeDescriptions, rng);
        text.localize(d);
        explain(d);
        JsonObject json = QuestJson.build(d);
        List<String> problems = SchemaCheck.check(json, caps);
        if (!problems.isEmpty()) {
            markBad(d, "schema check failed: " + problems);
            return false;
        }
        ValidationResult vr;
        try {
            vr = validator.validate(d, json, acceptedCount);
        } catch (RuntimeException e) {
            vr = ValidationResult.fail("validator threw " + e);
        }
        if (vr == null || !vr.ok()) {
            rejections.merge(CandidateResolver.STEP_VALIDATOR, 1L, Long::sum);
            markBad(d, "validator rejected: " + (vr == null ? "null result" : vr.message()));
            return false;
        }
        d.json = json;
        return true;
    }

    private void markBad(QuestDraft d, String why) {
        String sig = d.signature();
        if (badSignatures.add(sig)) {
            log.warn("[GenV2] discarded quest " + sig + " (" + why + ")");
        }
        if (d.objectives.size() == 1) {
            badSignatures.add(d.objectives.get(0).c().signaturePart());
        }
    }

    private void explain(QuestDraft d) {
        d.explain.clear();
        for (QuestDraft.Objective o : d.objectives) {
            Candidate c = o.c();
            if (c.type().counted()) {
                d.explain.add("objective " + c.type().shortName() + " " + c.target() + " ×" + o.count());
            } else {
                d.explain.add("objective " + c.type().shortName() + " " + c.target());
            }
            String mods = c.modifierNote().isEmpty() ? "" : " (" + c.modifierNote() + ")";
            d.explain.add(String.format(Locale.ROOT, "  effort %.2f min/unit%s → %.1f min (target %.1f, range %s–%s min)",
                c.baseEffort(), mods, o.minutes(), d.targetMinutes, num(level.minMinutes), num(level.maxMinutes)));
            StringBuilder sb = new StringBuilder("  tier ").append(c.tier()).append(" (")
                .append(progression.tierNote(c.tier())).append("), family ").append(c.family())
                .append(", profile ").append(c.profile());
            if (c.tool() != null && !"none".equals(c.tool())) {
                sb.append(", tool ").append(c.tool());
            }
            if (!c.hints().isEmpty()) {
                sb.append(", hints ").append(String.join("/", c.hints()));
            }
            if (c.dimension() != null && !"minecraft:overworld".equals(c.dimension())) {
                sb.append(", ").append(c.dimension());
            }
            d.explain.add(sb.toString());
        }
        if (d.overhead > 0) {
            d.explain.add(String.format(Locale.ROOT, "travel overhead %.1f min", d.overhead));
        }
        if (d.themeKey != null) {
            d.explain.add("theme " + d.themeKey);
        } else if (d.comboFamily != null) {
            d.explain.add("combo within family " + d.comboFamily);
        }
        d.explain.add(String.format(Locale.ROOT, "estimated %.1f min%s", d.estMinutes, d.quick ? " (quick)" : ""));
        d.explain.add(RewardBuilder.describe(d));
        List<String> alts = new ArrayList<>();
        Set<Object> seen = new HashSet<>();
        for (QuestDraft.Objective o : d.objectives) {
            if (!seen.add(o.c().entry())) {
                continue;
            }
            alts.addAll(o.c().entry().exclusions());
            alts.addAll(pool.notesFor(o.c().entry()));
        }
        if (!alts.isEmpty()) {
            d.explain.add("rejected alternatives: " + String.join("; ", alts.subList(0, Math.min(3, alts.size()))));
        }
    }

    private static String num(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    // ------------------------------------------------------------------ set bookkeeping

    private void register(Kept d) {
        usedFamilies.addAll(d.families());
        usedTargets.addAll(d.targets());
        usedSignatures.add(d.signature());
        if (d.title() != null) {
            usedTitles.add(d.title().toLowerCase(Locale.ROOT));
        }
        for (String t : d.types()) {
            ObjectiveType ot = ObjectiveType.parse(t);
            if (ot != null) {
                typeCounts.merge(ot, 1, Integer::sum);
            }
        }
    }

    private void rebuildState(List<Kept> kept, List<QuestDraft> accepted) {
        usedFamilies.clear();
        usedTargets.clear();
        usedSignatures.clear();
        usedTitles.clear();
        typeCounts.clear();
        for (Kept k : kept) {
            register(k);
        }
        for (QuestDraft a : accepted) {
            register(Kept.of(a));
        }
    }

    private void repairDistinctTypes(List<QuestDraft> accepted, List<Kept> kept, int totalSize) {
        int needed = Math.min(totalSize, balance.minDistinctTypes);
        if (distinctTypes(accepted, kept).size() >= needed || accepted.isEmpty()) {
            return;
        }
        for (int i = accepted.size() - 1; i >= 0 && distinctTypes(accepted, kept).size() < needed; i--) {
            QuestDraft old = accepted.get(i);
            if (old.quick) {
                continue;
            }
            List<QuestDraft> others = new ArrayList<>(accepted);
            others.remove(i);
            Set<String> present = distinctTypes(others, kept);
            Set<ObjectiveType> exclude = new LinkedHashSet<>();
            for (String t : present) {
                exclude.add(ObjectiveType.parse(t));
            }
            // only worth replacing if the old quest's types are duplicated elsewhere
            if (!present.containsAll(old.types())) {
                continue;
            }
            rebuildState(kept, others);
            QuestDraft repl = fillSlot(new Slot(1, false, Mode.ANY, null), i, exclude);
            if (repl != null) {
                accepted.set(i, repl);
            }
            rebuildState(kept, accepted);
        }
        if (distinctTypes(accepted, kept).size() < needed) {
            relax("distinct_types", "only " + distinctTypes(accepted, kept).size() + " types available");
        }
    }

    private static Set<String> distinctTypes(List<QuestDraft> a, List<Kept> b) {
        Set<String> s = new HashSet<>();
        for (QuestDraft d : a) {
            s.addAll(d.types());
        }
        for (Kept d : b) {
            s.addAll(d.types());
        }
        return s;
    }

    private void relax(String rule, String where) {
        relaxations.merge(rule, 1L, Long::sum);
        relaxNotes.add(rule + " (" + where + ")");
    }
}
