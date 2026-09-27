package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.ClaimState;
import com.erikedits.justquests.generator.v2.api.ClaimView;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.ExpiredClaim;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import com.erikedits.justquests.generator.v2.api.StartResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property test for the claim state machine (§10): random sequences of claims, completions,
 * abandons, force releases, logouts, time jumps, rerolls, restarts with a (sometimes lossy) view of
 * the mod's active quests, and disable/enable. A simple model of what the mod believes is kept next
 * to the generator; after every step both must agree and the invariants must hold.
 */
class ClaimsPropertyTest {
    private static final long MIN = 60_000L;
    private static final long HOUR = 60 * MIN;

    /** What the mod believes: active holders per quest, completions per quest. */
    static final class Model {
        final Map<String, Set<UUID>> holders = new HashMap<>();
        final Map<String, Set<UUID>> completions = new HashMap<>();

        Set<UUID> holders(String q) {
            return holders.getOrDefault(q, Set.of());
        }

        void add(String q, UUID p) {
            holders.computeIfAbsent(q, k -> new HashSet<>()).add(p);
        }

        void remove(String q, UUID p) {
            Set<UUID> s = holders.get(q);
            if (s != null) {
                s.remove(p);
                if (s.isEmpty()) {
                    holders.remove(q);
                }
            }
        }

        void apply(List<ExpiredClaim> expired) {
            for (ExpiredClaim e : expired) {
                remove(e.questId(), e.holder());
            }
        }

        Map<String, Set<UUID>> activeView() {
            Map<String, Set<UUID>> out = new HashMap<>();
            holders.forEach((q, s) -> out.put(q, new HashSet<>(s)));
            return out;
        }
    }

    private static long cycleOf(String questId) {
        String rest = questId.substring(questId.lastIndexOf('/') + 1);
        return Long.parseLong(rest.substring(0, rest.indexOf('_')));
    }

    private static boolean isRetained(String q, Set<String> served) {
        long max = 0;
        for (String s : served) {
            max = Math.max(max, cycleOf(s));
        }
        return cycleOf(q) < max;
    }

    private static ClaimResult expected(GeneratorConfig cfg, Model m, Set<String> served, String q, UUID p) {
        if (!served.contains(q)) {
            return ClaimResult.NOT_SERVED;
        }
        if (m.holders(q).contains(p)) {
            return ClaimResult.ALREADY_YOURS;
        }
        Set<UUID> done = m.completions.getOrDefault(q, Set.of());
        if (cfg.exclusiveClaims() ? !done.isEmpty() : done.contains(p)) {
            return ClaimResult.COMPLETED;
        }
        if (cfg.exclusiveClaims() && !m.holders(q).isEmpty()) {
            return ClaimResult.CLAIMED_BY_OTHER;
        }
        if (isRetained(q, served)) {
            return ClaimResult.NOT_SERVED;
        }
        if (cfg.oneActivePerPlayer()) {
            for (String o : served) {
                if (!o.equals(q) && m.holders(o).contains(p)) {
                    return ClaimResult.HAS_ACTIVE_GENERATED;
                }
            }
        }
        return ClaimResult.OK;
    }

    private static void checkInvariants(QuestGeneratorV2 g, GeneratorConfig cfg, Model m, FakeHost host, String ctx) {
        Set<String> served = g.servedQuests().keySet();
        for (Map.Entry<String, Set<UUID>> e : m.holders.entrySet()) {
            assertTrue(served.contains(e.getKey()), ctx + ": held quest " + e.getKey() + " is no longer served");
        }
        Map<UUID, Integer> perPlayer = new HashMap<>();
        for (String q : served) {
            Set<UUID> actual = new HashSet<>(g.holders(q));
            assertEquals(m.holders(q), actual, ctx + ": holders of " + q);
            ClaimView v = g.claim(q);
            if (actual.isEmpty()) {
                assertTrue(v.state() != ClaimState.CLAIMED, ctx + ": " + q + " CLAIMED without holders");
            } else {
                assertEquals(ClaimState.CLAIMED, v.state(), ctx + ": " + q);
                assertTrue(actual.contains(v.holder()), ctx + ": view holder of " + q);
            }
            if (cfg.exclusiveClaims()) {
                assertTrue(actual.size() <= 1, ctx + ": exclusive quest " + q + " has " + actual);
                if (!m.completions.getOrDefault(q, Set.of()).isEmpty() && actual.isEmpty()) {
                    assertEquals(ClaimState.COMPLETED, v.state(), ctx + ": " + q);
                }
            }
            for (UUID u : actual) {
                perPlayer.merge(u, 1, Integer::sum);
            }
            assertTrue(g.isGenerated(q), ctx);
        }
        if (cfg.oneActivePerPlayer()) {
            perPlayer.forEach((u, n) -> assertTrue(n <= 1, ctx + ": " + u + " holds " + n));
        }
        // retained quests (older cycles) stay only while someone holds them, or until the next
        // rotation once their holder completed them
        long retained = 0;
        for (String q : served) {
            if (isRetained(q, served)) {
                retained++;
                assertTrue(!m.holders(q).isEmpty() || !m.completions.getOrDefault(q, Set.of()).isEmpty(),
                    ctx + ": retained quest " + q + " without holders or completions");
            }
        }
        assertTrue(served.size() <= cfg.questsPerCycle() + retained, ctx + ": served " + served.size());
        assertTrue(g.selfTest().isEmpty(), ctx + ": " + g.selfTest());
        assertEquals(0, host.log.count("ERROR"), ctx + ": " + host.log.lines);
    }

    /** How often the interesting paths were taken over all runs (the test fails if one never is). */
    private static final Map<String, Integer> COVERAGE = new HashMap<>();

    private static void hit(String what) {
        COVERAGE.merge(what, 1, Integer::sum);
    }

    private static void run(long seed, GeneratorConfig cfg, int steps) {
        FakeHost host = new FakeHost();
        if (seed % 2 == 0) {
            host.withMods("farmersdelight", "create");
        }
        SplittableRandom rng = new SplittableRandom(seed);
        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            players.add(new UUID(seed, i));
        }
        Model m = new Model();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, cfg);
        g.start(Map.of());
        String ctx0 = "seed " + seed + " " + cfg.difficulty() + " excl=" + cfg.exclusiveClaims() + " one="
            + cfg.oneActivePerPlayer() + " rel=" + cfg.releaseOnAbandon() + " exp=" + cfg.claimExpiryHours() + " cyc="
            + cfg.cycleHours();
        checkInvariants(g, cfg, m, host, ctx0 + " start");
        for (int step = 0; step < steps; step++) {
            String ctx = ctx0 + " step " + step;
            List<String> served = new ArrayList<>(g.servedQuests().keySet());
            UUID p = players.get(rng.nextInt(players.size()));
            String q = served.isEmpty() || rng.nextInt(20) == 0 ? "justquests:gen/1_" + rng.nextInt(9)
                : served.get(rng.nextInt(served.size()));
            int op = rng.nextInt(100);
            if (op < 35) {
                ClaimResult want = expected(cfg, m, new HashSet<>(served), q, p);
                ClaimResult got = g.tryClaim(q, p);
                assertEquals(want, got, ctx + ": tryClaim " + q + " by " + p);
                hit("claim " + got);
                if (got == ClaimResult.OK) {
                    m.add(q, p);
                }
            } else if (op < 52) {
                // complete something the player holds (usually) or a random quest (rare, mod edge case)
                String target = q;
                for (String s : served) {
                    if (m.holders(s).contains(p) && rng.nextInt(4) != 0) {
                        target = s;
                        break;
                    }
                }
                g.onComplete(target, p);
                if (served.contains(target)) {
                    m.remove(target, p);
                    m.completions.computeIfAbsent(target, k -> new HashSet<>()).add(p);
                    hit("completed");
                }
            } else if (op < 62) {
                g.onAbandon(q, p);
                m.remove(q, p);
            } else if (op < 66) {
                List<String> released = g.releaseAllFor(p);
                for (String s : released) {
                    assertTrue(m.holders(s).contains(p), ctx + ": released " + s + " not held by " + p);
                }
                for (String s : new ArrayList<>(m.holders.keySet())) {
                    m.remove(s, p);
                }
            } else if (op < 69) {
                Set<UUID> before = new HashSet<>(m.holders(q));
                Optional<UUID> first = g.forceRelease(q);
                assertEquals(before.isEmpty(), first.isEmpty(), ctx + ": forceRelease " + q);
                first.ifPresent(u -> assertTrue(before.contains(u), ctx));
                m.holders.remove(q);
            } else if (op < 86) {
                long jump = rng.nextInt(10) == 0 ? (24 + rng.nextInt(72)) * HOUR : (5 + rng.nextInt(180)) * MIN;
                host.now += jump;
                RotationResult r = g.tick();
                m.apply(r.expiredClaims());
                if (!r.expiredClaims().isEmpty()) {
                    hit("expired");
                }
                if (r.changed() && !r.retained().isEmpty()) {
                    hit("rotation with retained");
                }
                for (ExpiredClaim e : r.expiredClaims()) {
                    assertTrue(g.holders(e.questId()).stream().noneMatch(e.holder()::equals), ctx);
                }
            } else if (op < 90) {
                RotationResult r = g.reroll();
                m.apply(r.expiredClaims());
            } else if (op < 95) {
                // server restart; the mod's view is sometimes lossy (a player file was reset)
                g.stop();
                Map<String, Set<UUID>> view = m.activeView();
                UUID lost = null;
                String lostQuest = null;
                if (!view.isEmpty() && rng.nextInt(3) == 0) {
                    lostQuest = new ArrayList<>(view.keySet()).get(rng.nextInt(view.size()));
                    lost = view.get(lostQuest).iterator().next();
                    view.get(lostQuest).remove(lost);
                    if (view.get(lostQuest).isEmpty()) {
                        view.remove(lostQuest);
                    }
                    m.remove(lostQuest, lost);
                    hit("lossy restart");
                }
                String ghost = null;
                if (rng.nextInt(4) == 0) {
                    ghost = "justquests:gen/7_" + (1000 + rng.nextInt(100));
                    view.put(ghost, new HashSet<>(Set.of(p)));
                }
                host.now += rng.nextInt(3) == 0 ? rng.nextInt(48) * HOUR : rng.nextInt(10) * MIN;
                g = new QuestGeneratorV2(host, cfg);
                StartResult sr = g.startWithHolders(view);
                if (lost != null) {
                    final UUID lp = lost;
                    final String lq = lostQuest;
                    assertTrue(sr.releasedClaims().stream().anyMatch(e -> e.questId().equals(lq) && e.holder().equals(lp)),
                        ctx + ": lossy restart must release " + lq + " of " + lp + " but released " + sr.releasedClaims());
                }
                if (ghost != null) {
                    assertTrue(sr.deadQuestIds().contains(ghost), ctx + ": ghost quest not reported dead");
                }
                assertEquals(lost == null ? 0 : 1, sr.releasedClaims().size(), ctx + ": " + sr.releasedClaims());
                m.apply(sr.initialRotation().expiredClaims());
            } else if (op < 97) {
                g.updateConfig(cfg.withEnabled(false));
                assertTrue(g.servedQuests().isEmpty(), ctx);
                assertEquals(ClaimResult.DISABLED, g.tryClaim(q, p), ctx);
                g.onComplete("minecraft:not_generated", p);
                g.updateConfig(cfg);
            } else {
                g.save();
            }
            checkInvariants(g, cfg, m, host, ctx + " op " + op);
        }
        g.stop();
    }

    @Test
    void randomOperationSequencesKeepModelAndGeneratorInSync() {
        long seed = 1;
        for (Difficulty d : Difficulty.values()) {
            for (boolean exclusive : new boolean[]{true, false}) {
                for (boolean oneActive : new boolean[]{true, false}) {
                    GeneratorConfig cfg = TestSupport.config(d, 5 + (int) (seed % 8))
                        .withExclusiveClaims(exclusive)
                        .toBuilder()
                        .oneActivePerPlayer(oneActive)
                        .releaseOnAbandon(seed % 3 != 0)
                        .claimExpiryHours(seed % 4 == 0 ? 0 : seed % 4 == 1 ? 3 : 30)
                        .cycleHours(seed % 3 == 0 ? 24 : seed % 3 == 1 ? 6 : 2)
                        .build()
                        .sanitized(null);
                    run(seed++, cfg, 300);
                }
            }
        }
        for (String path : List.of("claim OK", "claim ALREADY_YOURS", "claim CLAIMED_BY_OTHER", "claim COMPLETED",
            "claim HAS_ACTIVE_GENERATED", "claim NOT_SERVED", "expired", "rotation with retained", "lossy restart",
            "completed")) {
            assertTrue(COVERAGE.getOrDefault(path, 0) > 0, "never exercised: " + path + " " + COVERAGE);
        }
    }
}
