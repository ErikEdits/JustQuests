package com.erikedits.justquests.generator.v2.internal;

import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.ClaimState;
import com.erikedits.justquests.generator.v2.api.ClaimView;
import com.erikedits.justquests.generator.v2.api.ContentKind;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.ExpiredClaim;
import com.erikedits.justquests.generator.v2.api.GenLog;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.GeneratorHost;
import com.erikedits.justquests.generator.v2.api.HostCapabilities;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import com.erikedits.justquests.generator.v2.api.StartResult;
import com.erikedits.justquests.generator.v2.api.StatsSummary;
import com.erikedits.justquests.generator.v2.api.ValidationResult;
import com.erikedits.justquests.generator.v2.internal.catalog.Catalog;
import com.erikedits.justquests.generator.v2.internal.catalog.CatalogLoader;
import com.erikedits.justquests.generator.v2.internal.catalog.ProfileDef;
import com.erikedits.justquests.generator.v2.internal.gen.CandidateResolver;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.gen.RewardBuilder;
import com.erikedits.justquests.generator.v2.internal.gen.SchemaCheck;
import com.erikedits.justquests.generator.v2.internal.gen.SetBuilder;
import com.erikedits.justquests.generator.v2.internal.gen.TextBuilder;
import com.erikedits.justquests.generator.v2.internal.state.ClaimRecord;
import com.erikedits.justquests.generator.v2.internal.state.CycleClock;
import com.erikedits.justquests.generator.v2.internal.state.GenState;
import com.erikedits.justquests.generator.v2.internal.state.QuestRecord;
import com.erikedits.justquests.generator.v2.internal.state.V1Migration;
import com.erikedits.justquests.generator.v2.internal.stats.StatsBook;
import com.erikedits.justquests.generator.v2.internal.util.Ids;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.erikedits.justquests.generator.v2.internal.util.Rng;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** The generator's state machine behind {@link com.erikedits.justquests.generator.v2.QuestGeneratorV2}. */
public final class Core {
    private static final long HOUR_MS = 3_600_000L;
    private static final long DAY_MS = 24 * HOUR_MS;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    private final GeneratorHost host;
    private final GenLog log;
    private GeneratorConfig config;
    private Catalog catalog;
    private GenState state = new GenState();
    private StatsBook stats = new StatsBook();
    private CycleClock clock;
    private boolean started;
    private boolean stopped;
    private long nextBoundaryMillis = Long.MAX_VALUE;
    private long nextExpiryMillis = Long.MAX_VALUE;
    private boolean clockWarned;
    private long servedRevision;
    private RotationResult noneResult = RotationResult.none(0);
    private V1Migration.V1Data v1Data;
    private final Set<String> warnedOnce = new HashSet<>();

    public Core(GeneratorHost host, GeneratorConfig config) {
        this.host = host;
        this.log = host.log();
        this.config = sanitize(config);
        this.clock = new CycleClock(this.config.cycleHours(), this.config.cycleAnchorHour(), this.config.zone());
    }

    private GeneratorConfig sanitize(GeneratorConfig c) {
        List<String> notes = new ArrayList<>();
        GeneratorConfig out = c.sanitized(notes);
        for (String n : notes) {
            log.warn("[GenV2] config adjusted: " + n);
        }
        return out;
    }

    // =================================================================== lifecycle

    public StartResult start(Map<String, ? extends Collection<UUID>> active) {
        if (stopped) {
            log.warn("[GenV2] start() after stop() ignored");
            return new StartResult(List.of(), List.of(), noneResult);
        }
        long t0 = System.nanoTime();
        catalog = new CatalogLoader(log, host.store()).load();
        long loadMs = (System.nanoTime() - t0) / 1_000_000L;
        log.info(String.format(Locale.ROOT, "[GenV2] catalog loaded in %d ms: %d targets in %d profile(s), %d reward items, %d themes",
            loadMs, catalog.targetCount(null), catalog.profiles.size(), catalog.items.size(), catalog.themes.size()));
        int dropped = 0;
        if (!started) {
            loadState();
            loadStats();
            dropped = dropInvalidStored();
        }
        started = true;
        long now = now();
        List<String> dead = new ArrayList<>();
        List<ExpiredClaim> released = new ArrayList<>();
        reconcile(normalize(active), now, dead, released);
        v1Data = null;
        RotationResult rotation = noneResult;
        if (config.enabled()) {
            rotation = rotateIfDue(now, true);
            if (!rotation.changed() && dropped > 0) {
                topUp(now);
            }
        }
        refreshTimers();
        save();
        return new StartResult(dead, released, rotation);
    }

    /**
     * Re-checks the stored quest definitions: the state file may have been edited by hand, or a
     * content mod may have been removed since the last run. Quests that no longer pass the schema
     * check or the host validator are dropped; a player who still has one active gets it reported
     * as dead by the reconciliation. When the check itself cannot run (the host fails), the quest
     * is kept.
     *
     * @return number of dropped quests
     */
    private int dropInvalidStored() {
        int dropped = 0;
        for (QuestRecord r : new ArrayList<>(state.served())) {
            String problem = storedProblem(r);
            if (problem == null) {
                continue;
            }
            log.warn("[GenV2] stored quest " + r.id + " is no longer valid (" + problem + "); dropping it");
            state.current.remove(r.id);
            state.retained.remove(r.id);
            dropped++;
        }
        if (dropped > 0) {
            servedRevision++;
        }
        return dropped;
    }

    private String storedProblem(QuestRecord r) {
        if (r.json == null || r.claim == null) {
            return "incomplete record";
        }
        if (!r.legacy) {
            // quests imported from v1 were written by v1; only the host's codec judges those
            HostCapabilities caps;
            try {
                caps = host.capabilities();
            } catch (RuntimeException e) {
                return null;
            }
            List<String> problems;
            try {
                problems = SchemaCheck.check(r.json, caps);
            } catch (RuntimeException e) {
                return "unreadable definition: " + e;
            }
            if (!problems.isEmpty()) {
                return String.join("; ", problems);
            }
        }
        ValidationResult vr;
        try {
            vr = host.validator().validate(r.id, r.json.deepCopy());
        } catch (RuntimeException e) {
            return null;
        }
        return vr == null || vr.ok() ? null : "host validator: " + vr.message();
    }

    /** Fills the current set back up to {@code questsPerCycle} after quests were dropped. */
    private void topUp(long now) {
        List<SetBuilder.Kept> kept = new ArrayList<>();
        for (QuestRecord r : state.current.values()) {
            kept.add(keptOf(r));
        }
        int n = Math.max(0, config.questsPerCycle() - kept.size());
        if (n > 0 && state.cycleId > 0) {
            updateProgression();
            List<String> added = generateInto(n, kept, now, true);
            servedRevision++;
            log.info("[GenV2] topped the set up with " + added.size() + " new quest(s)");
        }
    }

    private static Map<String, Set<UUID>> normalize(Map<String, ? extends Collection<UUID>> active) {
        Map<String, Set<UUID>> out = new TreeMap<>();
        if (active == null) {
            return out;
        }
        for (Map.Entry<String, ? extends Collection<UUID>> e : active.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || !Ids.isGeneratedQuestId(e.getKey())) {
                continue;
            }
            Set<UUID> s = out.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>());
            for (UUID u : e.getValue()) {
                if (u != null) {
                    s.add(u);
                }
            }
        }
        return out;
    }

    public void stop() {
        if (stopped) {
            return;
        }
        save();
        stopped = true;
    }

    public void updateConfig(GeneratorConfig newConfig) {
        GeneratorConfig old = config;
        config = sanitize(newConfig);
        if (old.cycleHours() != config.cycleHours() || old.cycleAnchorHour() != config.cycleAnchorHour()
            || !old.zone().equals(config.zone())) {
            clock = new CycleClock(config.cycleHours(), config.cycleAnchorHour(), config.zone());
        }
        if (!started || stopped) {
            return;
        }
        catalog = new CatalogLoader(log, host.store()).load();
        if (old.enabled() != config.enabled()) {
            servedRevision++;
            if (config.enabled()) {
                rotateIfDue(now(), false);
            }
        }
        refreshTimers();
        save();
    }

    // =================================================================== persistence

    private void loadState() {
        Optional<String> text = read(GenState.FILE);
        if (text.isPresent()) {
            try {
                state = GenState.fromJson(Json.parseObject(text.get()));
                return;
            } catch (RuntimeException e) {
                String backup = "generator_v2.corrupt-" + now() + ".json";
                log.error("[GenV2] " + GenState.FILE + " is unreadable; backed up to " + backup + " and starting fresh", e);
                write(backup, text.get());
                state = new GenState();
            }
        }
        Optional<String> v1 = read(V1Migration.FILE);
        if (v1.isPresent() && state.cycleId == 0) {
            try {
                v1Data = V1Migration.parse(v1.get());
                state.history.putAll(v1Data.history);
                state.migratedFromV1 = true;
                log.info("[GenV2] migrating from generator v1: " + v1Data.history.size() + " history signature(s), "
                    + v1Data.quests.size() + " v1 quest definition(s)");
            } catch (RuntimeException e) {
                log.warn("[GenV2] could not read v1 " + V1Migration.FILE + ": " + e.getMessage());
            }
        }
    }

    private void loadStats() {
        Optional<String> text = read(StatsBook.FILE);
        if (text.isPresent()) {
            try {
                stats = StatsBook.fromJson(Json.parseObject(text.get()));
            } catch (RuntimeException e) {
                String backup = "generator_v2_stats.corrupt-" + now() + ".json";
                log.error("[GenV2] " + StatsBook.FILE + " is unreadable; backed up to " + backup, e);
                write(backup, text.get());
                stats = new StatsBook();
            }
        }
    }

    public void save() {
        if (!started) {
            return;
        }
        state.lastSeenMillis = Math.max(state.lastSeenMillis, now());
        // compact JSON keeps months of play well below 256 KB; pretty-print it to read it
        write(GenState.FILE, Json.compact(state.toJson()));
        if (config.statsEnabled()) {
            write(StatsBook.FILE, Json.compact(stats.toJson()));
        }
    }

    private Optional<String> read(String file) {
        try {
            Optional<String> r = host.store().read(file);
            return r == null ? Optional.empty() : r;
        } catch (RuntimeException e) {
            log.error("[GenV2] could not read " + file, e);
            return Optional.empty();
        }
    }

    private void write(String file, String content) {
        try {
            host.store().write(file, content);
        } catch (RuntimeException e) {
            log.error("[GenV2] could not write " + file, e);
        }
    }

    private long now() {
        return host.currentTimeMillis();
    }

    // =================================================================== reconciliation (§10.5)

    private void reconcile(Map<String, Set<UUID>> active, long now, List<String> dead, List<ExpiredClaim> released) {
        // (a) claims whose holder no longer has the quest active are released
        for (QuestRecord r : new ArrayList<>(state.served())) {
            Set<UUID> holdersNow = active.getOrDefault(r.id, Set.of());
            for (UUID h : new ArrayList<>(r.claim.holders.keySet())) {
                if (!holdersNow.contains(h)) {
                    r.claim.holders.remove(h);
                    released.add(new ExpiredClaim(r.id, h));
                }
            }
            if (state.isRetained(r.id) && r.claim.holders.isEmpty()) {
                state.retained.remove(r.id);
                servedRevision++;
            }
        }
        // (b) active quests without claims: adopt known definitions, report unknown ones as dead
        for (Map.Entry<String, Set<UUID>> e : active.entrySet()) {
            String id = e.getKey();
            QuestRecord r = state.find(id);
            if (r == null && v1Data != null && v1Data.quests.containsKey(id)) {
                r = importV1(id, v1Data.quests.get(id), now);
                state.retained.put(id, r);
                servedRevision++;
            }
            if (r == null) {
                dead.add(id);
                continue;
            }
            for (UUID p : e.getValue()) {
                if (r.claim.holders.containsKey(p)) {
                    continue;
                }
                if (config.exclusiveClaims() && !r.claim.holders.isEmpty()) {
                    log.warn("[GenV2] " + id + " is actively held by several players (legacy data); keeping all holders");
                }
                r.claim.holders.put(p, now);
            }
        }
    }

    private QuestRecord importV1(String id, JsonObject json, long now) {
        QuestRecord r = new QuestRecord();
        r.id = id;
        r.json = json.deepCopy();
        r.cycleId = 0;
        r.legacy = true;
        r.signature = V1Migration.signatureOf(json);
        r.generatedAt = now;
        r.difficulty = config.difficulty().name();
        for (JsonObject o : Json.objects(json, "objectives")) {
            r.types.add(Json.str(o, "type", "?").replace("justquests:", ""));
        }
        r.profiles.add("vanilla");
        r.explain.add("imported from generator v1 (" + V1Migration.FILE + "); kept because a player has it active");
        return r;
    }

    // =================================================================== rotation (§11)

    /** Rotates when a boundary passed; used by start/tick/enable. */
    private RotationResult rotateIfDue(long now, boolean atStart) {
        long latest = clock.boundaryAtOrBefore(now) / 1000L;
        if (state.cycleId == 0) {
            return rotate(latest, "cycle", now, List.of());
        }
        if (latest > state.cycleId) {
            long nextAfterCurrent = clock.nextBoundaryAfter(state.cycleId * 1000L) / 1000L;
            String reason = latest > nextAfterCurrent ? "catch-up" : "cycle";
            return rotate(latest, reason, now, List.of());
        }
        if (latest < state.cycleId && !clockWarned) {
            clockWarned = true;
            log.warn("[GenV2] system clock is before the current cycle start (" + state.cycleId
                + "); keeping the current set until time passes the next boundary");
        }
        return RotationResult.none(state.cycleId);
    }

    private RotationResult rotate(long newCycle, String reason, long now, List<ExpiredClaim> expired) {
        List<String> removed = new ArrayList<>();
        for (QuestRecord r : new ArrayList<>(state.current.values())) {
            if (!r.claim.holders.isEmpty()) {
                state.retained.put(r.id, r);
            } else {
                removed.add(r.id);
            }
        }
        state.current.clear();
        for (QuestRecord r : new ArrayList<>(state.retained.values())) {
            if (r.claim.holders.isEmpty()) {
                state.retained.remove(r.id);
                removed.add(r.id);
            }
        }
        state.cycleId = newCycle;
        state.cycleStartedAt = now;
        state.rerollCounter = 0;
        state.nextIndex = 0;
        state.difficulty = config.difficulty().name();
        state.pruneHistory(now, config.historyDays() * DAY_MS);
        updateProgression();
        updateCalibration();
        List<String> added = generateInto(config.questsPerCycle(), List.of(), now, true);
        servedRevision++;
        noneResult = RotationResult.none(state.cycleId);
        refreshTimers();
        save();
        log.info("[GenV2] rotation (" + reason + "): cycle " + state.cycleId + ", " + added.size() + " new, "
            + state.retained.size() + " retained, " + removed.size() + " removed");
        return new RotationResult(true, reason, state.cycleId, added, new ArrayList<>(state.retained.keySet()), removed, expired);
    }

    private void updateProgression() {
        List<ProfileDef> active = new CandidateResolver(catalog, host.content(), host.capabilities(), log,
            config.difficulty(), state.progression, Map.of(), config.disabledProfiles()).activeProfiles();
        try {
            state.progression.update(host.world(), catalog.balance, active);
        } catch (RuntimeException e) {
            log.error("[GenV2] progression update failed", e);
        }
    }

    private void updateCalibration() {
        if (!config.adaptiveBalancing() || state.calibratedCycle == state.cycleId) {
            return;
        }
        state.calibratedCycle = state.cycleId;
        var b = catalog.balance;
        for (Map.Entry<String, Double> e : stats.calibrationRatios(b.calibrationMinSamples).entrySet()) {
            double target = Math.max(b.calibrationMin, Math.min(b.calibrationMax, e.getValue()));
            double cur = state.calibration.getOrDefault(e.getKey(), 1.0);
            double step = Math.max(1.0 - b.calibrationMaxStep, Math.min(1.0 + b.calibrationMaxStep, target / cur));
            double next = Math.max(b.calibrationMin, Math.min(b.calibrationMax, cur * step));
            state.calibration.put(e.getKey(), next);
        }
    }

    private Generation.Result generate(int n, List<SetBuilder.Kept> kept, long cycleId, long seed, Progression progression,
                                       Set<String> historySigs, int firstIndex) {
        return Generation.run(catalog, host, config, progression, state.calibration, historySigs, kept, n, cycleId, seed,
            firstIndex);
    }

    /** Generates and stores quests for the current cycle; returns the new ids. */
    private List<String> generateInto(int n, List<SetBuilder.Kept> kept, long now, boolean record) {
        long worldSeed;
        try {
            worldSeed = host.world().worldSeed();
        } catch (RuntimeException e) {
            worldSeed = 0L;
        }
        long seed = Rng.cycleSeed(worldSeed, state.cycleId, state.rerollCounter);
        long t0 = System.nanoTime();
        Generation.Result run = generate(n, kept, state.cycleId, seed, state.progression,
            new HashSet<>(state.history.keySet()), state.nextIndex);
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        List<String> added = new ArrayList<>();
        for (QuestDraft d : run.drafts()) {
            QuestRecord r = toRecord(d, state.cycleId, state.nextIndex++, seed, now);
            state.current.put(r.id, r);
            state.history.put(r.signature, now);
            added.add(r.id);
            if (record) {
                stats.onGenerated(r, now);
            }
        }
        if (record) {
            stats.addRejections(run.rejections());
            stats.addRelaxations(run.relaxations());
        }
        if (!run.relaxNotes().isEmpty()) {
            log.warn("[GenV2] set rules relaxed: " + String.join(", ", run.relaxNotes()));
        }
        if (added.size() < n) {
            log.warn("[GenV2] only " + added.size() + " of " + n + " quests could be generated (content pool too small)");
        }
        log.info("[GenV2] generated " + added.size() + " quest(s) in " + ms + " ms (" + config.difficulty() + ")");
        return added;
    }

    private QuestRecord toRecord(QuestDraft d, long cycleId, int index, long seed, long now) {
        QuestRecord r = new QuestRecord();
        r.id = Ids.GEN_PREFIX + cycleId + "_" + index;
        r.cycleId = cycleId;
        r.index = index;
        r.json = d.json;
        r.signature = d.signature();
        r.estMinutes = d.estMinutes;
        r.tier = d.tier();
        r.families = new ArrayList<>(d.families());
        r.types = new ArrayList<>(d.types());
        r.profiles = new ArrayList<>(d.profiles());
        r.difficulty = config.difficulty().name();
        r.generatedAt = now;
        r.seed = seed;
        r.budget = d.budget;
        r.rewardValue = d.rewardValue;
        r.quick = d.quick;
        r.theme = d.themeKey;
        r.explain = new ArrayList<>(d.explain);
        return r;
    }

    private SetBuilder.Kept keptOf(QuestRecord r) {
        Set<String> targets = new LinkedHashSet<>();
        for (JsonObject o : Json.objects(r.json, "objectives")) {
            for (String f : new String[]{"item", "block", "entity", "dimension"}) {
                // an item filter object ({"id": ..., "potion": ...}) counts by its id
                String v = o.has(f) && o.get(f).isJsonObject() ? Json.str(o.getAsJsonObject(f), "id", null)
                    : Json.str(o, f, null);
                if (v != null) {
                    targets.add(v);
                }
            }
        }
        boolean modded = false;
        for (String p : r.profiles) {
            modded |= !"vanilla".equals(p);
        }
        return new SetBuilder.Kept(new LinkedHashSet<>(r.families), targets, r.signature, new LinkedHashSet<>(r.types),
            r.title(), r.quick, new LinkedHashSet<>(r.profiles), modded);
    }

    public RotationResult tick() {
        if (!started || stopped || !config.enabled()) {
            return noneResult;
        }
        long now = now();
        if (now < nextBoundaryMillis && now < nextExpiryMillis) {
            return noneResult;
        }
        List<String> expiryRemoved = new ArrayList<>();
        List<ExpiredClaim> expired = releaseExpired(now, expiryRemoved);
        if (now >= nextBoundaryMillis) {
            RotationResult r = rotateIfDue(now, false);
            if (r.changed()) {
                return new RotationResult(true, r.reason(), r.cycleId(), r.added(), r.retained(), r.removed(), expired);
            }
            refreshTimers();
        }
        if (!expired.isEmpty()) {
            save();
            return new RotationResult(!expiryRemoved.isEmpty(), "expiry", state.cycleId, List.of(),
                new ArrayList<>(state.retained.keySet()), expiryRemoved, expired);
        }
        return noneResult;
    }

    private int effectiveExpiryHours() {
        if (config.claimExpiryHours() > 0) {
            return config.claimExpiryHours();
        }
        return catalog == null ? 0 : catalog.balance.level(config.difficulty()).claimExpiryHours;
    }

    private List<ExpiredClaim> releaseExpired(long now, List<String> removedOut) {
        int hours = effectiveExpiryHours();
        List<ExpiredClaim> out = new ArrayList<>();
        if (hours <= 0) {
            return out;
        }
        long limit = hours * HOUR_MS;
        for (QuestRecord r : new ArrayList<>(state.served())) {
            for (Map.Entry<UUID, Long> h : new ArrayList<>(r.claim.holders.entrySet())) {
                if (now - h.getValue() >= limit) {
                    out.add(new ExpiredClaim(r.id, h.getKey()));
                    stats.onExpired(r.id);
                    if (release(r, h.getKey(), true)) {
                        removedOut.add(r.id);
                    }
                }
            }
        }
        if (!out.isEmpty()) {
            log.info("[GenV2] released " + out.size() + " expired claim(s)");
        }
        refreshTimers();
        return out;
    }

    /** Recomputes the O(1) tick guards. */
    private void refreshTimers() {
        if (state.cycleId > 0) {
            nextBoundaryMillis = clock.nextBoundaryAfter(state.cycleId * 1000L);
        } else {
            nextBoundaryMillis = Long.MIN_VALUE;
        }
        int hours = effectiveExpiryHours();
        long next = Long.MAX_VALUE;
        if (hours > 0) {
            for (QuestRecord r : state.served()) {
                for (long t : r.claim.holders.values()) {
                    next = Math.min(next, t + hours * HOUR_MS);
                }
            }
        }
        nextExpiryMillis = next;
        noneResult = RotationResult.none(state.cycleId);
    }

    public RotationResult reroll() {
        if (!started || stopped || !config.enabled()) {
            return noneResult;
        }
        long now = now();
        long latest = clock.boundaryAtOrBefore(now) / 1000L;
        if (state.cycleId == 0 || latest > state.cycleId) {
            // no set yet, or a boundary passed before the next tick(): a fresh cycle is the reroll
            RotationResult r = rotate(latest, "reroll", now, List.of());
            return new RotationResult(true, "reroll", r.cycleId(), r.added(), r.retained(), r.removed(), List.of());
        }
        List<String> removed = new ArrayList<>();
        List<SetBuilder.Kept> kept = new ArrayList<>();
        for (QuestRecord r : new ArrayList<>(state.current.values())) {
            if (r.claim.holders.isEmpty() && r.claim.completions.isEmpty()) {
                state.current.remove(r.id);
                removed.add(r.id);
            } else {
                kept.add(keptOf(r));
            }
        }
        state.rerollCounter++;
        state.difficulty = config.difficulty().name();
        state.pruneHistory(now, config.historyDays() * DAY_MS);
        updateProgression();
        int n = Math.max(0, config.questsPerCycle() - kept.size());
        List<String> added = generateInto(n, kept, now, true);
        servedRevision++;
        refreshTimers();
        save();
        log.info("[GenV2] reroll: " + added.size() + " new, " + kept.size() + " kept, " + removed.size() + " removed");
        return new RotationResult(true, "reroll", state.cycleId, added, new ArrayList<>(state.retained.keySet()), removed,
            List.of());
    }

    // =================================================================== served set & claims (§10)

    public Map<String, JsonObject> servedQuests() {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        if (!started || !config.enabled()) {
            return out;
        }
        List<QuestRecord> list = state.served();
        list.sort(Comparator.comparingInt((QuestRecord r) -> Json.integer(r.json, "sort", 0)).thenComparing(r -> r.id));
        for (QuestRecord r : list) {
            out.put(r.id, r.json.deepCopy());
        }
        return out;
    }

    public long servedRevision() {
        return servedRevision;
    }

    public ClaimResult tryClaim(String questId, UUID player) {
        if (!Ids.isGeneratedQuestId(questId)) {
            return ClaimResult.NOT_GENERATED;
        }
        if (!config.enabled()) {
            return ClaimResult.DISABLED;
        }
        QuestRecord r = started ? state.find(questId) : null;
        if (r == null) {
            return ClaimResult.NOT_SERVED;
        }
        ClaimRecord c = r.claim;
        if (c.holders.containsKey(player)) {
            // checked first so a legacy second holder can still finish after the other completed
            return ClaimResult.ALREADY_YOURS;
        }
        if (config.exclusiveClaims() ? !c.completions.isEmpty() : c.completions.containsKey(player)) {
            return ClaimResult.COMPLETED;
        }
        if (config.exclusiveClaims() && !c.holders.isEmpty()) {
            return ClaimResult.CLAIMED_BY_OTHER;
        }
        if (state.isRetained(questId)) {
            return ClaimResult.NOT_SERVED;
        }
        if (config.oneActivePerPlayer()) {
            for (QuestRecord o : state.served()) {
                if (!o.id.equals(questId) && o.claim.holders.containsKey(player)) {
                    return ClaimResult.HAS_ACTIVE_GENERATED;
                }
            }
        }
        long now = now();
        c.holders.put(player, now);
        stats.onClaimed(questId, now);
        refreshTimers();
        save();
        return ClaimResult.OK;
    }

    public void onAbandon(String questId, UUID player) {
        QuestRecord r = started ? state.find(questId) : null;
        if (r == null || !r.claim.holders.containsKey(player)) {
            return;
        }
        stats.onAbandoned(questId);
        release(r, player, config.releaseOnAbandon());
        refreshTimers();
        save();
    }

    /**
     * Removes one holder. Retained quests disappear once nobody holds them; current-cycle quests
     * become available again, or leave the set when {@code makeAvailable} is false.
     *
     * @return true if the quest left the served set
     */
    private boolean release(QuestRecord r, UUID player, boolean makeAvailable) {
        r.claim.holders.remove(player);
        if (!r.claim.holders.isEmpty()) {
            return false;
        }
        if (state.isRetained(r.id)) {
            state.retained.remove(r.id);
            servedRevision++;
            return true;
        }
        if (!makeAvailable && state.current.containsKey(r.id) && r.claim.completions.isEmpty()) {
            state.current.remove(r.id);
            servedRevision++;
            return true;
        }
        return false;
    }

    public void onComplete(String questId, UUID player) {
        QuestRecord r = started ? state.find(questId) : null;
        if (r == null) {
            if (Ids.isGeneratedQuestId(questId) && warnedOnce.add("complete:" + questId)) {
                log.warn("[GenV2] completion of unknown generated quest " + questId);
            }
            return;
        }
        long now = now();
        Long claimedAt = r.claim.holders.remove(player);
        if (claimedAt == null && warnedOnce.add("unclaimed:" + questId + player)) {
            log.warn("[GenV2] " + questId + " completed by a player without a claim; recording anyway");
        }
        r.claim.completions.put(player, now);
        double minutes = claimedAt == null ? 0 : (now - claimedAt) / 60000.0;
        stats.onCompleted(questId, now, minutes);
        refreshTimers();
        save();
    }

    public List<String> releaseAllFor(UUID player) {
        List<String> out = new ArrayList<>();
        if (!started) {
            return out;
        }
        for (QuestRecord r : new ArrayList<>(state.served())) {
            if (r.claim.holders.containsKey(player)) {
                out.add(r.id);
                release(r, player, config.releaseOnAbandon());
            }
        }
        if (!out.isEmpty()) {
            refreshTimers();
            save();
        }
        return out;
    }

    public Optional<UUID> forceRelease(String questId) {
        QuestRecord r = started ? state.find(questId) : null;
        if (r == null || r.claim.holders.isEmpty()) {
            return Optional.empty();
        }
        UUID first = r.claim.firstHolder();
        for (UUID h : new ArrayList<>(r.claim.holders.keySet())) {
            release(r, h, true);
        }
        refreshTimers();
        save();
        return Optional.of(first);
    }

    public ClaimView claim(String questId) {
        QuestRecord r = started ? state.find(questId) : null;
        return r == null ? ClaimView.AVAILABLE : view(r);
    }

    private ClaimView view(QuestRecord r) {
        ClaimRecord c = r.claim;
        long completedAt = 0;
        UUID completer = null;
        for (Map.Entry<UUID, Long> e : c.completions.entrySet()) {
            if (e.getValue() >= completedAt) {
                completedAt = e.getValue();
                completer = e.getKey();
            }
        }
        if (config.exclusiveClaims() && !c.completions.isEmpty() && c.holders.isEmpty()) {
            return new ClaimView(ClaimState.COMPLETED, completer, 0L, completedAt);
        }
        if (!c.holders.isEmpty()) {
            UUID h = c.firstHolder();
            return new ClaimView(ClaimState.CLAIMED, h, c.holders.get(h), completedAt);
        }
        return new ClaimView(ClaimState.AVAILABLE, null, 0L, completedAt);
    }

    public Map<String, ClaimView> claims() {
        Map<String, ClaimView> out = new LinkedHashMap<>();
        if (!started) {
            return out;
        }
        for (String id : servedQuests().keySet()) {
            out.put(id, view(state.find(id)));
        }
        return out;
    }

    public List<UUID> holders(String questId) {
        QuestRecord r = started ? state.find(questId) : null;
        return r == null ? List.of() : List.copyOf(r.claim.holders.keySet());
    }

    // =================================================================== debug & stats

    public List<JsonObject> preview(int count) {
        List<JsonObject> out = new ArrayList<>();
        if (!started || count <= 0) {
            return out;
        }
        long now = now();
        long base = state.cycleId > 0 ? state.cycleId * 1000L : clock.boundaryAtOrBefore(now);
        long nextCycle = clock.nextBoundaryAfter(base) / 1000L;
        long worldSeed;
        try {
            worldSeed = host.world().worldSeed();
        } catch (RuntimeException e) {
            worldSeed = 0L;
        }
        Progression copy = Progression.fromJson(state.progression.toJson());
        Set<String> hist = new HashSet<>(state.history.keySet());
        int n = Math.min(count, config.maxPerCycle());
        Generation.Result run = generate(n, List.of(), nextCycle, Rng.cycleSeed(worldSeed, nextCycle, 0), copy, hist, 0);
        for (QuestDraft d : run.drafts()) {
            out.add(d.json.deepCopy());
        }
        return out;
    }

    public String explain(String questId) {
        QuestRecord r = started ? state.find(questId) : null;
        if (r == null) {
            return questId + ": not a served generated quest";
        }
        StringBuilder sb = new StringBuilder();
        String seedHex = Long.toHexString(r.seed).toUpperCase(Locale.ROOT);
        sb.append(r.id).append("  \"").append(r.title()).append("\"  (").append(r.difficulty)
            .append(", cycle ").append(r.cycleId).append(", seed 0x")
            .append(seedHex.length() > 4 ? seedHex.substring(0, 4) + "…" : seedHex).append(")\n");
        for (String line : r.explain) {
            sb.append(line).append('\n');
        }
        if (config.adaptiveBalancing() && !state.calibration.isEmpty()) {
            for (String f : r.families) {
                for (String t : r.types) {
                    Double m = state.calibration.get(f + "|" + t);
                    if (m != null) {
                        sb.append(String.format(Locale.ROOT, "calibration %s|%s ×%.2f (adaptive balancing on)%n", f, t, m));
                    }
                }
            }
        }
        ClaimView v = view(r);
        sb.append("state: ").append(v.state());
        if (v.holder() != null) {
            String h = v.holder().toString();
            sb.append(v.state() == ClaimState.COMPLETED ? " by " : " by ").append(h, 0, 4).append('…');
            long at = v.state() == ClaimState.COMPLETED ? v.completedAtMillis() : v.claimedAtMillis();
            if (at > 0) {
                sb.append("  at ").append(ZonedDateTime.ofInstant(Instant.ofEpochMilli(at), config.zone()).format(STAMP));
            }
        }
        if (state.isRetained(r.id)) {
            sb.append("  (retained from an older cycle)");
        }
        List<UUID> hs = holders(r.id);
        if (hs.size() > 1) {
            sb.append("  (").append(hs.size()).append(" holders)");
        }
        return sb.toString();
    }

    public StatsSummary stats() {
        return stats.summary(config.statsEnabled());
    }

    // =================================================================== accessors for the facade/tests

    public boolean started() {
        return started;
    }

    public GeneratorConfig config() {
        return config;
    }

    public long cycleId() {
        return state.cycleId;
    }

    public Catalog catalog() {
        return catalog;
    }

    /** Lower-case status line for {@code /quest generator status}. */
    public String status() {
        StringBuilder sb = new StringBuilder();
        sb.append("Generator v2: ").append(config.enabled() ? "enabled" : "disabled")
            .append(", difficulty ").append(config.difficulty().settingsValue())
            .append(", ").append(state.current.size()).append(" current + ").append(state.retained.size()).append(" retained");
        if (state.cycleId > 0) {
            sb.append(", cycle ").append(state.cycleId).append(" (next rotation ")
                .append(ZonedDateTime.ofInstant(Instant.ofEpochMilli(clock.nextBoundaryAfter(state.cycleId * 1000L)), config.zone())
                    .format(STAMP)).append(")");
        }
        int claimed = 0;
        for (QuestRecord r : state.served()) {
            if (!r.claim.holders.isEmpty()) {
                claimed++;
            }
        }
        sb.append(", ").append(claimed).append(" claimed");
        sb.append(", Nether ").append(state.progression.nether() ? "unlocked" : "locked")
            .append(", End ").append(state.progression.end() ? "unlocked" : "locked");
        if (catalog != null) {
            List<String> active = new ArrayList<>();
            for (ProfileDef p : new CandidateResolver(catalog, host.content(), host.capabilities(), log, config.difficulty(),
                state.progression, Map.of(), config.disabledProfiles()).activeProfiles()) {
                active.add(p.id());
            }
            sb.append(", profiles ").append(active);
        }
        String mc;
        String loader;
        try {
            mc = host.content().minecraftVersion();
            loader = host.content().loaderName();
        } catch (RuntimeException e) {
            mc = null;
            loader = null;
        }
        sb.append(", Minecraft ").append(mc == null || mc.isBlank() ? "?" : mc)
            .append(" (").append(loader == null || loader.isBlank() ? "?" : loader).append(")");
        return sb.toString();
    }

    /** Validates stored quests against the host codec (self-test). Returns problems. */
    public List<String> selfTest() {
        List<String> problems = new ArrayList<>();
        if (!started) {
            problems.add("generator not started");
            return problems;
        }
        for (Map.Entry<String, JsonObject> e : servedQuests().entrySet()) {
            ValidationResult vr = host.validator().validate(e.getKey(), e.getValue());
            if (vr == null || !vr.ok()) {
                problems.add(e.getKey() + ": " + (vr == null ? "null" : vr.message()));
            }
        }
        for (QuestRecord r : state.served()) {
            if (config.exclusiveClaims() && r.claim.holders.size() > 1) {
                problems.add(r.id + ": " + r.claim.holders.size() + " holders on an exclusive quest");
            }
        }
        Map<UUID, Integer> perPlayer = new TreeMap<>();
        for (QuestRecord r : state.served()) {
            for (UUID u : r.claim.holders.keySet()) {
                perPlayer.merge(u, 1, Integer::sum);
            }
        }
        if (config.oneActivePerPlayer()) {
            perPlayer.forEach((u, n) -> {
                if (n > 1) {
                    problems.add("player " + u + " holds " + n + " generated quests");
                }
            });
        }
        return problems;
    }

    /** Exposed for tests: whether an existence check passes for an id (uses a throwaway resolver). */
    boolean exists(ContentKind kind, String id) {
        return new CandidateResolver(catalog, host.content(), host.capabilities(), log, config.difficulty(),
            state.progression, Map.of(), config.disabledProfiles()).exists(kind, id);
    }
}
