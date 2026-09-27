package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.ClaimView;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.GeneratorHost;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import com.erikedits.justquests.generator.v2.api.StartResult;
import com.erikedits.justquests.generator.v2.api.StatsSummary;
import com.erikedits.justquests.generator.v2.internal.Core;
import com.erikedits.justquests.generator.v2.internal.util.Ids;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * JustQuests quest generator v2 — the facade the mod talks to.
 *
 * <p><b>One instance per world/server.</b> Create it after datapacks and tags are loaded, call
 * {@link #start} once, {@link #tick} every ~5 minutes, the claim methods from the accept/abandon/
 * complete paths, and {@link #stop} when the server stops. The instance keeps no static state.
 *
 * <p><b>Threading:</b> every method must be called from the Minecraft server thread. The core is
 * not thread-safe and starts no threads or timers.
 *
 * <p><b>Errors:</b> methods never throw during normal operation. Bad data, missing mods or unknown
 * ids are skipped and logged through {@link com.erikedits.justquests.generator.v2.api.GenLog}. Only
 * programmer errors (null arguments) throw {@link NullPointerException}.
 *
 * <p><b>Persistence:</b> state lives in {@code <world>/justquests/generator_v2.json} and (if stats are
 * enabled) {@code generator_v2_stats.json}; both are written through
 * {@link com.erikedits.justquests.generator.v2.api.StateStore} after every state change.
 */
public final class QuestGeneratorV2 {
    private final GeneratorHost host;
    private final Core core;

    /**
     * Creates the generator. Does no I/O; call {@link #start} next.
     *
     * @param host   the per-build host adapter (not null)
     * @param config settings from {@code settings.json} (not null; out-of-range values are clamped and logged)
     */
    public QuestGeneratorV2(GeneratorHost host, GeneratorConfig config) {
        this.host = Objects.requireNonNull(host, "host");
        Objects.requireNonNull(host.log(), "host.log()");
        this.core = new Core(host, Objects.requireNonNull(config, "config"));
    }

    /**
     * Call once at server start AFTER datapacks/tags are loaded.
     * Loads bundled data + world state, migrates v1 state, reconciles claims with the players' real
     * active quests (§10.5) and rotates if a cycle boundary passed while the server was down.
     *
     * <p>{@code activeGeneratedQuests} can hold only one holder per quest; if several players may
     * hold the same quest (exclusivity off, or legacy data), prefer {@link #startWithHolders}.
     *
     * @param activeGeneratedQuests questId → holder UUID for every generated quest that is currently
     *                              ACTIVE in any player's saved data (not null; may be empty)
     * @return quest ids the mod must remove from player data because their definition no longer
     *         exists (dead quests), released claims and the start-up rotation
     */
    public StartResult start(Map<String, UUID> activeGeneratedQuests) {
        Objects.requireNonNull(activeGeneratedQuests, "activeGeneratedQuests");
        Map<String, Set<UUID>> m = new LinkedHashMap<>();
        activeGeneratedQuests.forEach((k, v) -> {
            if (k != null && v != null) {
                m.put(k, Set.of(v));
            }
        });
        return startWithHolders(m);
    }

    /**
     * Same as {@link #start(Map)} but accepts several holders per quest (needed when two players
     * hold the same generated quest, e.g. with exclusive claims off or legacy data).
     *
     * @param activeGeneratedQuests questId → every player UUID that has the quest active (not null)
     * @return see {@link #start(Map)}
     */
    public StartResult startWithHolders(Map<String, ? extends Collection<UUID>> activeGeneratedQuests) {
        Objects.requireNonNull(activeGeneratedQuests, "activeGeneratedQuests");
        return guard("start", () -> core.start(activeGeneratedQuests),
            () -> new StartResult(List.of(), List.of(), RotationResult.none(0)));
    }

    /**
     * Call periodically (the mod calls it every ~5 minutes). O(1) when nothing is due. Rotates when a
     * cycle boundary passed (once, even after long downtime: reason {@code "catch-up"}); releases
     * expired claims if claim expiry is configured.
     *
     * @return what changed; {@code changed == true} means re-register {@link #servedQuests()} and sync.
     *         Always apply {@code expiredClaims}, even when {@code changed} is false.
     */
    public RotationResult tick() {
        return guard("tick", core::tick, () -> RotationResult.none(core.cycleId()));
    }

    /**
     * OP reroll: replace all UNCLAIMED quests of the current cycle now. Claimed and completed
     * quests are kept; new quests continue the id index. Applies the current difficulty.
     *
     * @return the change (reason {@code "reroll"}); unchanged if the generator is disabled
     */
    public RotationResult reroll() {
        return guard("reroll", core::reroll, () -> RotationResult.none(core.cycleId()));
    }

    /**
     * Apply new settings (after {@code /quest reload}, {@code /quest difficulty ...}). Also reloads
     * the world override files. Content/difficulty/count changes take effect at the next rotation or
     * reroll; enabling/disabling takes effect immediately ({@link #servedQuests()} changes, so the
     * mod re-registers and syncs after an enable/disable).
     *
     * @param config the new settings (not null)
     */
    public void updateConfig(GeneratorConfig config) {
        Objects.requireNonNull(config, "config");
        guard("updateConfig", () -> {
            core.updateConfig(config);
            return null;
        }, () -> null);
    }

    /**
     * Everything the mod must register right now: the current cycle's quests plus RETAINED quests
     * (claimed, not yet finished, from older cycles). Iteration order is stable (by {@code sort}, then
     * id). Empty if disabled or not started. Returns defensive copies.
     *
     * @return questId ({@code "justquests:gen/..."}) → quest JSON (§6)
     */
    public Map<String, JsonObject> servedQuests() {
        return guard("servedQuests", core::servedQuests, LinkedHashMap::new);
    }

    /**
     * Counter that increases whenever {@link #servedQuests()} changes (rotation, reroll, enable,
     * abandon of a retained quest…). The mod may compare it after claim calls to re-register lazily.
     *
     * @return monotonically increasing revision
     */
    public long servedRevision() {
        return core.servedRevision();
    }

    /**
     * Tells whether a quest id belongs to the generator (no state lookup).
     *
     * @param questId any quest id (not null)
     * @return true if the id belongs to the generator ({@code justquests:gen/...})
     */
    public boolean isGenerated(String questId) {
        Objects.requireNonNull(questId, "questId");
        return Ids.isGeneratedQuestId(questId);
    }

    /**
     * Call from the accept path AFTER the mod's own eligibility checks and BEFORE adding the quest
     * to the player. {@link ClaimResult#OK} / {@link ClaimResult#ALREADY_YOURS} ⇒ proceed;
     * {@link ClaimResult#NOT_GENERATED} ⇒ ignore claims; anything else ⇒ deny with
     * {@link ClaimResult#denyMessage()}. Saves state on success.
     *
     * @param questId quest id (not null)
     * @param player  accepting player (not null)
     * @return the claim decision
     */
    public ClaimResult tryClaim(String questId, UUID player) {
        Objects.requireNonNull(questId, "questId");
        Objects.requireNonNull(player, "player");
        return guard("tryClaim", () -> core.tryClaim(questId, player), () -> ClaimResult.NOT_SERVED);
    }

    /**
     * Player abandoned the quest (via {@code /quest abandon} or the GUI). A current-cycle quest becomes
     * AVAILABLE again (if {@code releaseOnAbandon}, otherwise it leaves the set); a retained quest from
     * an older cycle is removed entirely. No-op if the player does not hold it.
     *
     * @param questId quest id (not null)
     * @param player  the player (not null)
     */
    public void onAbandon(String questId, UUID player) {
        Objects.requireNonNull(questId, "questId");
        Objects.requireNonNull(player, "player");
        guard("onAbandon", () -> {
            core.onAbandon(questId, player);
            return null;
        }, () -> null);
    }

    /**
     * Player completed the quest (rewards already granted by the mod). With exclusive claims the
     * quest becomes COMPLETED for everyone; completed quests leave the set at the next rotation.
     *
     * @param questId quest id (not null)
     * @param player  the player (not null)
     */
    public void onComplete(String questId, UUID player) {
        Objects.requireNonNull(questId, "questId");
        Objects.requireNonNull(player, "player");
        guard("onComplete", () -> {
            core.onComplete(questId, player);
            return null;
        }, () -> null);
    }

    /**
     * Release every claim held by this player (admin reset of player data), with abandon semantics.
     *
     * @param player the player (not null)
     * @return ids of the released quests
     */
    public List<String> releaseAllFor(UUID player) {
        Objects.requireNonNull(player, "player");
        return guard("releaseAllFor", () -> core.releaseAllFor(player), List::of);
    }

    /**
     * OP tool: force-release one claim (all holders). A current-cycle quest becomes AVAILABLE, a
     * retained one is removed. The mod must remove the quest from the former holder(s)' active quests.
     *
     * @param questId quest id (not null)
     * @return the former (first) holder, if any
     */
    public Optional<UUID> forceRelease(String questId) {
        Objects.requireNonNull(questId, "questId");
        return guard("forceRelease", () -> core.forceRelease(questId), Optional::empty);
    }

    /**
     * Claim state for list/GUI/sync.
     *
     * @param questId quest id (not null)
     * @return the view; {@link ClaimView#AVAILABLE} for unknown ids
     */
    public ClaimView claim(String questId) {
        Objects.requireNonNull(questId, "questId");
        return guard("claim", () -> core.claim(questId), () -> ClaimView.AVAILABLE);
    }

    /**
     * Claim views of every served quest, in {@link #servedQuests()} order.
     *
     * @return questId → view
     */
    public Map<String, ClaimView> claims() {
        return guard("claims", core::claims, LinkedHashMap::new);
    }

    /**
     * All current holders of a quest (several only with exclusive claims off or legacy data).
     *
     * @param questId quest id (not null)
     * @return holders in claim order; empty if none/unknown
     */
    public List<UUID> holders(String questId) {
        Objects.requireNonNull(questId, "questId");
        return guard("holders", () -> core.holders(questId), List::of);
    }

    /**
     * Preview the NEXT cycle without applying it (OP debug). Does not change state or stats.
     *
     * @param count number of quests (clamped to {@code maxPerCycle})
     * @return quest JSON objects (no ids yet)
     */
    public List<JsonObject> preview(int count) {
        return guard("preview", () -> core.preview(count), List::of);
    }

    /**
     * Human-readable, multi-line explanation of why/how a quest was made (effort, tier, rewards,
     * rejected alternatives, claim state).
     *
     * @param questId quest id (not null)
     * @return the explanation (a one-line note for unknown ids)
     */
    public String explain(String questId) {
        Objects.requireNonNull(questId, "questId");
        return guard("explain", () -> core.explain(questId), () -> questId + ": explain failed");
    }

    /**
     * One-line status for {@code /quest generator status}.
     *
     * @return status text
     */
    public String status() {
        return guard("status", core::status, () -> "Generator v2: status unavailable");
    }

    /**
     * Self-check for {@code /quest test}: every served quest passes the host validator and the claims
     * respect the configured rules.
     *
     * @return problems found; empty if healthy
     */
    public List<String> selfTest() {
        return guard("selfTest", core::selfTest, () -> List.of("self test failed with an exception"));
    }

    /**
     * Test-phase statistics summary (§15).
     *
     * @return aggregates; see {@link StatsSummary#toText()}
     */
    public StatsSummary stats() {
        return guard("stats", core::stats, () -> new StatsSummary(false, 0, 0, 0, 0, 0, Map.of(), Map.of(), Map.of(),
            Map.of(), Double.NaN, Double.NaN, Double.NaN, Map.of(), 0, Map.of(), Map.of()));
    }

    /** Persist state now (the core also saves after every state change). */
    public void save() {
        guard("save", () -> {
            core.save();
            return null;
        }, () -> null);
    }

    /** Server stopping: final save; the instance is unusable afterwards. */
    public void stop() {
        guard("stop", () -> {
            core.stop();
            return null;
        }, () -> null);
    }

    /**
     * The configuration currently in effect, after clamping.
     *
     * @return the active (sanitised) configuration
     */
    public GeneratorConfig config() {
        return core.config();
    }

    private <T> T guard(String what, Supplier<T> body, Supplier<T> fallback) {
        try {
            return body.get();
        } catch (RuntimeException e) {
            host.log().error("[GenV2] " + what + " failed; continuing", e);
            return fallback.get();
        }
    }
}
