package com.erikedits.justquests.generator.v2.internal.gen;

import com.erikedits.justquests.generator.v2.api.ContentKind;
import com.erikedits.justquests.generator.v2.api.ContentView;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GenLog;
import com.erikedits.justquests.generator.v2.api.HostCapabilities;
import com.erikedits.justquests.generator.v2.api.TriState;
import com.erikedits.justquests.generator.v2.internal.catalog.Balance;
import com.erikedits.justquests.generator.v2.internal.catalog.Catalog;
import com.erikedits.justquests.generator.v2.internal.catalog.EntryDef;
import com.erikedits.justquests.generator.v2.internal.catalog.ObjectiveType;
import com.erikedits.justquests.generator.v2.internal.catalog.ProfileDef;
import com.erikedits.justquests.generator.v2.internal.catalog.TagConcept;
import com.erikedits.justquests.generator.v2.internal.catalog.TargetDef;
import com.erikedits.justquests.generator.v2.internal.util.English;
import com.erikedits.justquests.generator.v2.internal.util.Ids;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Achievability pipeline steps 1–5 (§9.2): type allowed → profile active → id exists → hook
 * compatible → progression/tier. Steps 6 (history/set) and 7 (validator) run in the composer.
 * One instance per generation run; host answers are cached for the run.
 */
public final class CandidateResolver {
    public static final String STEP_TYPE = "1_type";
    public static final String STEP_PROFILE = "2_profile";
    public static final String STEP_ID = "3_missing_id";
    public static final String STEP_HOOK = "4_hook";
    public static final String STEP_TIER = "5_tier";
    public static final String STEP_PROGRESSION = "5_progression";
    public static final String STEP_EFFORT = "5_effort_range";
    public static final String STEP_HISTORY = "6_history";
    public static final String STEP_DUPLICATE = "6_duplicate";
    public static final String STEP_VALIDATOR = "7_validator";

    /** Standard objective types assumed when a host reports an empty type set. */
    private static final Set<String> STANDARD_TYPES;

    static {
        Set<String> s = new LinkedHashSet<>();
        for (ObjectiveType t : ObjectiveType.values()) {
            s.add(t.typeId());
        }
        STANDARD_TYPES = s;
    }

    private final Catalog catalog;
    private final ContentView content;
    private final HostCapabilities caps;
    private final GenLog log;
    private final Difficulty difficulty;
    private final Balance.Level level;
    private final Progression progression;
    private final Map<String, Double> calibration;
    private final Set<String> disabledProfiles;
    private final Map<String, Long> rejections = new LinkedHashMap<>();
    private final Map<String, List<String>> rejectedNotes = new HashMap<>();
    private final Map<String, Boolean> existsCache = new HashMap<>();
    private final Map<String, String> tagCache = new HashMap<>();

    public CandidateResolver(Catalog catalog, ContentView content, HostCapabilities caps, GenLog log,
                             Difficulty difficulty, Progression progression, Map<String, Double> calibration,
                             Set<String> disabledProfiles) {
        this.catalog = catalog;
        this.content = content;
        this.caps = caps;
        this.log = log;
        this.difficulty = difficulty;
        this.level = catalog.balance.level(difficulty);
        this.progression = progression;
        this.calibration = calibration;
        this.disabledProfiles = disabledProfiles;
    }

    /** Result of a resolution run. */
    public record Pool(List<Candidate> candidates, List<ProfileDef> activeProfiles, Map<String, Long> rejections,
                       Map<String, List<String>> rejectedNotes) {
        public List<String> notesFor(EntryDef e) {
            return rejectedNotes.getOrDefault(e.profile() + ":" + e.key(), List.of());
        }
    }

    /** Profiles whose mod is loaded and whose loader/version filters match (vanilla always, unless disabled). */
    public List<ProfileDef> activeProfiles() {
        List<ProfileDef> out = new ArrayList<>();
        for (ProfileDef p : catalog.profiles) {
            if (profileActive(p)) {
                out.add(p);
            }
        }
        return out;
    }

    private boolean profileActive(ProfileDef p) {
        if (disabledProfiles.contains(p.id())) {
            return false;
        }
        if (p.isVanilla()) {
            return true;
        }
        boolean loaded = false;
        for (String mod : p.requiresMod()) {
            if (safe(() -> content.isModLoaded(mod), false)) {
                loaded = true;
                break;
            }
        }
        if (!loaded) {
            return false;
        }
        String loader = safe(content::loaderName, "");
        if (!p.loaders().isEmpty() && !p.loaders().contains(loader == null ? "" : loader.toLowerCase(Locale.ROOT))) {
            return false;
        }
        String mc = safe(content::minecraftVersion, null);
        if (mc != null && !mc.isEmpty()) {
            if (p.mcMin() != null && Versions.compare(mc, p.mcMin()) < 0) {
                return false;
            }
            if (p.mcMax() != null && Versions.compare(mc, p.mcMax()) > 0) {
                return false;
            }
        }
        return true;
    }

    private String runningVersion;

    public Pool resolve() {
        String mc = safe(content::minecraftVersion, "");
        runningVersion = mc != null && Versions.isVersion(mc) ? mc : null;
        Set<String> hostTypes = safe(caps::objectiveTypes, Set.of());
        if (hostTypes == null || hostTypes.isEmpty()) {
            hostTypes = STANDARD_TYPES;
        }
        List<ProfileDef> active = activeProfiles();
        Set<String> activeIds = new LinkedHashSet<>();
        for (ProfileDef p : active) {
            activeIds.add(p.id());
        }
        List<Candidate> out = new ArrayList<>();
        for (ProfileDef p : catalog.profiles) {
            boolean on = activeIds.contains(p.id());
            for (EntryDef e : p.entries()) {
                for (TargetDef t : e.targets()) {
                    if (!hostTypes.contains(t.type().typeId())) {
                        reject(e, t, STEP_TYPE, "type not supported by this build");
                        continue;
                    }
                    if (!on) {
                        reject(e, t, STEP_PROFILE, null);
                        continue;
                    }
                    Candidate c = resolveTarget(p, e, t);
                    if (c != null) {
                        out.add(c);
                    }
                }
            }
        }
        // entries required via "key:<entry>" need at least one surviving candidate
        Set<String> liveKeys = new TreeSet<>();
        for (Candidate c : out) {
            liveKeys.add(c.entry().key());
        }
        List<Candidate> filtered = new ArrayList<>();
        for (Candidate c : out) {
            String missing = null;
            for (String r : c.entry().requires()) {
                if (r.startsWith("key:") && !liveKeys.contains(r.substring(4))) {
                    missing = r;
                }
            }
            if (missing != null) {
                reject(c.entry(), c.def(), STEP_PROGRESSION, "requires " + missing.substring(4));
            } else {
                filtered.add(c);
            }
        }
        return new Pool(filtered, active, rejections, rejectedNotes);
    }

    private Candidate resolveTarget(ProfileDef p, EntryDef e, TargetDef t) {
        ObjectiveType type = t.type();
        // content registered before its release (experimental feature flags, e.g. the breeze on 1.20.4)
        String since = t.since() != null ? t.since() : e.since();
        if (since != null && runningVersion != null && Versions.isVersion(since)
            && Versions.compare(runningVersion, since) < 0) {
            reject(e, t, STEP_ID, "released in " + since + ", running " + runningVersion);
            return null;
        }
        // step 3: id exists (tag first when supported, then id, then alternatives)
        String target = null;
        boolean isTag = false;
        String tagNoun = null;
        if (t.tagConcept() != null && safe(() -> caps.supportsTag(type.typeId()), false)) {
            String tag = resolveTag(catalog.tags.get(t.tagConcept()));
            if (tag != null) {
                target = "#" + tag;
                isTag = true;
                tagNoun = catalog.tags.get(t.tagConcept()).name();
            }
        }
        if (target == null) {
            List<String> ids = new ArrayList<>();
            if (t.id() != null) {
                ids.add(t.id());
            }
            ids.addAll(t.alts());
            for (String id : ids) {
                if (exists(type.kind(), id)) {
                    target = id;
                    break;
                }
            }
        }
        if (target == null) {
            reject(e, t, STEP_ID, "id not present on this build");
            return null;
        }
        // step 4: hook compatibility
        String hookProblem = isTag ? null : hookProblem(type, target, t);
        if (hookProblem != null) {
            reject(e, t, STEP_HOOK, hookProblem);
            return null;
        }
        // step 5: tier cap, progression, dimension, difficulty gating
        int tier = t.tier() >= 0 ? t.tier() : e.tier();
        if (tier > level.maxTier || difficulty.ordinal() < t.minDifficulty()) {
            reject(e, t, STEP_TIER, "tier " + tier + " above " + difficulty + " cap");
            return null;
        }
        if (!progression.tierUnlocked(tier)) {
            reject(e, t, STEP_PROGRESSION, "tier " + tier + " not unlocked yet");
            return null;
        }
        String dimension = t.dimension() != null ? t.dimension() : e.dimension();
        if (type == ObjectiveType.VISIT_DIMENSION) {
            dimension = target;
        }
        String dimProblem = dimensionProblem(p, dimension);
        if (dimProblem != null) {
            reject(e, t, STEP_PROGRESSION, dimProblem);
            return null;
        }
        for (String r : e.requires()) {
            String rp = requirementProblem(r);
            if (rp != null) {
                reject(e, t, STEP_PROGRESSION, rp);
                return null;
            }
        }
        // derived numbers
        List<String> hints = new ArrayList<>(e.hints());
        for (String h : t.hints()) {
            if (!hints.contains(h)) {
                hints.add(h);
            }
        }
        String tool = t.tool() != null ? t.tool() : e.tool();
        double mult = 1.0;
        StringBuilder note = new StringBuilder();
        for (String h : hints) {
            Double m = catalog.balance.hintMultipliers.get(h);
            if (m != null && m != 1.0) {
                mult *= m;
                note.append(String.format(Locale.ROOT, " ×%.2f %s", m, h));
            }
        }
        int toolGap = catalog.balance.toolLevel(tool) - expectedToolLevel();
        if (toolGap > 0) {
            double m = 1.0 + catalog.balance.toolPenaltyPerLevel * toolGap;
            mult *= m;
            note.append(String.format(Locale.ROOT, " ×%.2f tool", m));
        }
        Double cal = calibration.get(e.family() + "|" + type.shortName());
        if (cal != null && cal != 1.0) {
            mult *= cal;
            note.append(String.format(Locale.ROOT, " ×%.2f calibration", cal));
        }
        double overhead = overheadFor(dimension);
        final String resolved = target;
        int stack = 0;
        if (type.itemBased()) {
            int host = isTag ? -1 : safe(() -> content.maxStackSize(targetId(resolved)), -1);
            stack = host > 0 ? host : t.stack() > 0 ? t.stack() : 64;
        }
        String name = displayName(type.kind(), t, resolved, isTag);
        String plural = t.plural() != null ? t.plural() : English.plural(name, type.kind() == ContentKind.ENTITY);
        double weight = e.weight() * t.weight();
        Candidate c = new Candidate(p.id(), e, t, type, target, isTag, tier, e.family(), dimension, tool,
            List.copyOf(hints), t.effort(), mult, note.toString().trim(), overhead, t.min(), cap(t.max()), stack, name, plural,
            weight, tagNoun);
        // effort range feasibility for this difficulty
        double hi = level.maxMinutes * (1.0 + catalog.balance.rangeSlack);
        if (c.minMinutes() > hi) {
            reject(e, t, STEP_EFFORT, String.format(Locale.ROOT, "min count takes %.1f min (> %.0f)", c.minMinutes(), hi));
            return null;
        }
        if (c.maxMinutes() < level.minMinutes * 0.3) {
            reject(e, t, STEP_EFFORT, String.format(Locale.ROOT, "max count takes only %.1f min", c.maxMinutes()));
            return null;
        }
        return c;
    }

    private static String targetId(String target) {
        return target.startsWith("#") ? target.substring(1) : target;
    }

    private String hookProblem(ObjectiveType type, String id, TargetDef t) {
        switch (type) {
            case TAME_ANIMAL:
                if (!t.tamable()) {
                    return "not flagged tamable in the catalog";
                }
                return safe(() -> content.isTamableAnimal(id), TriState.UNKNOWN) == TriState.NO ? "host: not a TamableAnimal" : null;
            case BREED_ANIMAL:
                return safe(() -> content.isBreedableAnimal(id), TriState.UNKNOWN) == TriState.NO ? "host: not an Animal" : null;
            case CRAFT_ITEM:
                return safe(() -> content.hasCraftingRecipe(id), TriState.UNKNOWN) == TriState.NO ? "host: no crafting-grid recipe" : null;
            case SMELT_ITEM:
                return safe(() -> content.hasSmeltingRecipe(id), TriState.UNKNOWN) == TriState.NO ? "host: no smelting recipe" : null;
            case CONSUME_ITEM:
                return safe(() -> content.isConsumable(id), TriState.UNKNOWN) == TriState.NO ? "host: not consumable" : null;
            default:
                return null;
        }
    }

    private String dimensionProblem(ProfileDef p, String dimension) {
        if (dimension == null || "minecraft:overworld".equals(dimension)) {
            return null;
        }
        if (!level.dimensionTravel) {
            return "no dimension travel on " + difficulty;
        }
        if (!exists(ContentKind.DIMENSION, dimension)) {
            return "dimension " + dimension + " does not exist";
        }
        if ("minecraft:the_nether".equals(dimension)) {
            return progression.nether() ? null : "Nether not unlocked";
        }
        if ("minecraft:the_end".equals(dimension)) {
            return progression.end() ? null : "End not unlocked";
        }
        ProfileDef.DimensionUnlock u = findUnlock(dimension);
        if (u != null && !progression.dimensionUnlocked(dimension)) {
            return dimension + " not unlocked";
        }
        return null;
    }

    private ProfileDef.DimensionUnlock findUnlock(String dimension) {
        for (ProfileDef p : catalog.profiles) {
            ProfileDef.DimensionUnlock u = p.dimensions().get(dimension);
            if (u != null) {
                return u;
            }
        }
        return null;
    }

    private String requirementProblem(String r) {
        switch (r) {
            case "nether":
                return progression.nether() ? null : "requires the Nether";
            case "end":
                return progression.end() ? null : "requires the End";
            default:
                if (r.startsWith("dim:")) {
                    String d = r.substring(4);
                    return exists(ContentKind.DIMENSION, d) && progression.dimensionUnlocked(d) ? null : "requires " + d;
                }
                return null;
        }
    }

    private double overheadFor(String dimension) {
        if (dimension == null || "minecraft:overworld".equals(dimension)) {
            return 0.0;
        }
        Double d = catalog.balance.dimensionOverhead.get(dimension);
        return d != null ? d : catalog.balance.moddedDimensionOverhead;
    }

    /** Tool level an average player of this world is expected to have. */
    private int expectedToolLevel() {
        int lvl = 1;
        if (progression.maxGameDay() >= 2) {
            lvl++;
        }
        if (progression.nether()) {
            lvl++;
        }
        return Math.min(3, lvl);
    }

    private String displayName(ContentKind kind, TargetDef t, String target, boolean isTag) {
        if (isTag) {
            TagConcept c = catalog.tags.get(t.tagConcept());
            return English.titleCase(c.name());
        }
        if (t.name() != null && !t.name().isBlank()) {
            return t.name();
        }
        String host = English.cleanHostName(safe(() -> content.englishName(kind, target), null));
        if (host != null) {
            return host;
        }
        return Ids.prettify(target);
    }

    private String resolveTag(TagConcept concept) {
        if (concept == null) {
            return null;
        }
        String cacheKey = concept.key();
        if (tagCache.containsKey(cacheKey)) {
            return tagCache.get(cacheKey);
        }
        String found = null;
        for (String cand : concept.candidates()) {
            Set<String> members;
            switch (concept.kind()) {
                case BLOCK:
                    members = safe(() -> content.blockTagMembers(cand), Set.of());
                    break;
                case ENTITY:
                    members = safe(() -> content.entityTypeTagMembers(cand), Set.of());
                    break;
                default:
                    members = safe(() -> content.itemTagMembers(cand), Set.of());
                    break;
            }
            if (members != null && !members.isEmpty()) {
                found = cand;
                break;
            }
        }
        tagCache.put(cacheKey, found);
        return found;
    }

    /** Existence check with a per-run cache. */
    public boolean exists(ContentKind kind, String id) {
        String key = kind.ordinal() + id;
        Boolean cached = existsCache.get(key);
        if (cached != null) {
            return cached;
        }
        boolean r;
        switch (kind) {
            case ITEM:
                r = safe(() -> content.itemExists(id), false);
                break;
            case BLOCK:
                r = safe(() -> content.blockExists(id), false);
                break;
            case ENTITY:
                r = safe(() -> content.entityTypeExists(id), false);
                break;
            default:
                r = safe(() -> content.dimensionExists(id), false);
                break;
        }
        existsCache.put(key, r);
        return r;
    }

    private void reject(EntryDef e, TargetDef t, String step, String why) {
        rejections.merge(step, 1L, Long::sum);
        if (why != null) {
            String id = t.id() != null ? t.id() : "#" + t.tagConcept();
            rejectedNotes.computeIfAbsent(e.profile() + ":" + e.key(), k -> new ArrayList<>())
                .add(t.type().shortName() + " " + id + " (" + why + ")");
        }
    }

    public Map<String, Long> rejections() {
        return rejections;
    }

    /** Runs a host call, logging and falling back on any runtime exception. */
    private <T> T safe(java.util.function.Supplier<T> call, T fallback) {
        try {
            T v = call.get();
            return v == null ? fallback : v;
        } catch (RuntimeException ex) {
            log.warn("[GenV2] host call failed: " + ex);
            return fallback;
        }
    }

    /** Numeric dotted-version comparison ("1.21.10" &gt; "1.21.9"; "26.1" &gt; "1.21.10"). */
    public static final class Versions {
        private Versions() {
        }

        /** True for dotted numeric versions such as "1.21.1" or "26.1". */
        public static boolean isVersion(String s) {
            return s != null && s.matches("\\d+(\\.\\d+)+.*");
        }

        public static int compare(String a, String b) {
            int[] x = parse(a);
            int[] y = parse(b);
            for (int i = 0; i < Math.max(x.length, y.length); i++) {
                int u = i < x.length ? x[i] : 0;
                int v = i < y.length ? y[i] : 0;
                if (u != v) {
                    return Integer.compare(u, v);
                }
            }
            return 0;
        }

        private static int[] parse(String s) {
            String[] parts = s.trim().split("[.\\-+]");
            List<Integer> out = new ArrayList<>();
            for (String p : parts) {
                StringBuilder digits = new StringBuilder();
                for (char c : p.toCharArray()) {
                    if (Character.isDigit(c)) {
                        digits.append(c);
                    } else {
                        break;
                    }
                }
                if (digits.length() == 0) {
                    break;
                }
                out.add(digits.length() > 9 ? Integer.MAX_VALUE : Integer.parseInt(digits.toString()));
            }
            int[] r = new int[out.size()];
            for (int i = 0; i < r.length; i++) {
                r[i] = out.get(i);
            }
            return r;
        }
    }

    /** Collects the ids of all profiles in a list (helper for callers). */
    public static Set<String> ids(Collection<ProfileDef> profiles) {
        Set<String> s = new LinkedHashSet<>();
        for (ProfileDef p : profiles) {
            s.add(p.id());
        }
        return s;
    }

    /** A target's maximum count, raised for scaled sets (see {@link Balance#countCapScale}). */
    private int cap(int max) {
        double s = catalog.balance.countCapScale;
        return s == 1.0 ? max : (int) Math.min(1_000_000L, Math.round(max * s));
    }
}
