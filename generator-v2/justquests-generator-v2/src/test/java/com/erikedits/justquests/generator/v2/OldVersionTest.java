package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.gen.Candidate;
import com.erikedits.justquests.generator.v2.internal.gen.CandidateResolver;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Older and newer Minecraft versions. Several versions register content of the next release behind
 * an experimental feature flag (cherry in 1.19.4, breeze and copper grates in 1.20.3+, bogged in
 * 1.20.5/6, pale oak in 1.21.2/3). A host that only asks the registry would report those ids as
 * existing, so the catalog's {@code since} versions must keep them out on their own.
 */
class OldVersionTest {
    private static final List<String> V120 = List.of("minecraft:cherry_log", "minecraft:cherry_sapling",
        "minecraft:camel", "minecraft:chiseled_bookshelf", "minecraft:decorated_pot");
    private static final List<String> V1205 = List.of("minecraft:armadillo");
    private static final List<String> V121 = List.of("minecraft:bogged", "minecraft:breeze", "minecraft:copper_grate",
        "minecraft:copper_door");
    private static final List<String> V1214 = List.of("minecraft:pale_oak_log");
    private static final List<String> V119 = List.of("minecraft:mangrove_log", "minecraft:mud", "minecraft:frog",
        "minecraft:packed_mud", "minecraft:mud_bricks", "minecraft:sculk", "minecraft:sculk_sensor",
        "minecraft:mangrove_roots");

    private static final List<String> AFTER_1_21_3 = V1214;
    private static final List<String> AFTER_1_20_6 = concat(V121, AFTER_1_21_3);
    private static final List<String> AFTER_1_20_4 = concat(V1205, AFTER_1_20_6);
    private static final List<String> AFTER_1_19_4 = concat(V120, AFTER_1_20_4);
    private static final List<String> AFTER_1_18_2 = concat(V119, AFTER_1_19_4);

    /** Version → ids that must never be used on it. */
    static Stream<Object[]> versions() {
        return Stream.of(
            new Object[]{"1.18.2", AFTER_1_18_2},
            new Object[]{"1.19.2", AFTER_1_19_4},
            new Object[]{"1.19.4", AFTER_1_19_4},
            new Object[]{"1.20.1", AFTER_1_20_4},
            new Object[]{"1.20.4", AFTER_1_20_4},
            new Object[]{"1.20.6", AFTER_1_20_6},
            new Object[]{"1.21.1", AFTER_1_21_3},
            new Object[]{"1.21.3", AFTER_1_21_3},
            new Object[]{"1.21.4", List.of()},
            new Object[]{"1.21.10", List.of()},
            new Object[]{"26.1", List.of()});
    }

    private static List<String> concat(List<String> a, List<String> b) {
        return Stream.concat(a.stream(), b.stream()).toList();
    }

    private static FakeHost host(String version) {
        FakeHost h = new FakeHost().withMods("farmersdelight", "create");
        h.content.version = version;
        return h;
    }

    private static Set<String> poolTargets(Generation.Result r) {
        Set<String> out = new HashSet<>();
        for (Candidate c : r.pool().candidates()) {
            out.add(c.target());
        }
        return out;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("versions")
    void flaggedContentNeverAppearsAndSetsStillFill(String version, List<String> forbidden) {
        for (Difficulty d : Difficulty.values()) {
            FakeHost h = host(version);
            Set<String> used = new HashSet<>();
            for (long seed = 1; seed <= 40; seed++) {
                Generation.Result r = TestSupport.generate(h, TestSupport.config(d, 20), TestSupport.unlockedProgression(h),
                    seed * 7919L, 20);
                assertEquals(20, r.drafts().size(), version + " " + d + " seed " + seed + " did not fill the set");
                for (QuestDraft q : r.drafts()) {
                    for (QuestDraft.Objective o : q.objectives) {
                        used.add(o.c().target());
                    }
                }
                if (seed == 1) {
                    Set<String> pool = poolTargets(r);
                    for (String id : forbidden) {
                        assertFalse(pool.contains(id), version + " " + d + ": " + id + " must be rejected by the since-guard");
                    }
                }
            }
            for (String id : forbidden) {
                assertFalse(used.contains(id), version + " " + d + ": used " + id);
            }
        }
    }

    @Test
    void newestContentIsAvailableOnNewVersions() {
        for (String v : new String[]{"1.21.4", "1.21.10", "26.1"}) {
            FakeHost h = host(v);
            Generation.Result r = TestSupport.generate(h, TestSupport.config(Difficulty.NORMAL, 20),
                TestSupport.unlockedProgression(h), 1L, 20);
            boolean versionRejected = r.pool().rejectedNotes().values().stream().flatMap(List::stream)
                .anyMatch(n -> n.contains("released in"));
            assertFalse(versionRejected, v + ": nothing may be rejected for its version");
            Set<String> pool = poolTargets(r);
            for (String id : List.of("minecraft:pale_oak_log", "minecraft:cherry_log", "minecraft:copper_grate",
                "minecraft:mangrove_log", "minecraft:decorated_pot")) {
                assertTrue(pool.contains(id), v + " pool misses " + id);
            }
        }
    }

    @Test
    void unparsableVersionDisablesTheGuardInsteadOfEverything() {
        for (String v : new String[]{null, "", "snapshot-24w14a", "unknown"}) {
            FakeHost h = host(v);
            Generation.Result r = TestSupport.generate(h, TestSupport.config(Difficulty.NORMAL, 20),
                TestSupport.unlockedProgression(h), 3L, 20);
            assertEquals(20, r.drafts().size(), "version " + v);
            assertTrue(poolTargets(r).contains("minecraft:pale_oak_log"), "guard must be off for version " + v);
        }
    }

    @Test
    void rejectionIsReportedUnderTheIdStep() {
        FakeHost h = host("1.20.1");
        Generation.Result r = TestSupport.generate(h, TestSupport.config(Difficulty.NORMAL, 10), TestSupport.unlockedProgression(h),
            5L, 10);
        Map<String, Long> rej = r.pool().rejections();
        assertTrue(rej.getOrDefault("3_missing_id", 0L) >= V1205.size() + V121.size() + V1214.size(), rej.toString());
        boolean noted = r.pool().rejectedNotes().values().stream().flatMap(List::stream)
            .anyMatch(n -> n.contains("released in 1.21") && n.contains("running 1.20.1"));
        assertTrue(noted, "explain notes carry the version reason");
        assertTrue(CandidateResolver.Versions.compare("1.21.10", "1.21.9") > 0);
        assertTrue(CandidateResolver.Versions.compare("26.1", "1.21.10") > 0);
        assertEquals(0, CandidateResolver.Versions.compare("1.21", "1.21.0"));
    }

    @Test
    void statusNamesTheRunningVersionAndLoader() {
        FakeHost h = host("1.20.1");
        h.content.loader = "forge";
        QuestGeneratorV2 g = new QuestGeneratorV2(h, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        assertTrue(g.status().endsWith("Minecraft 1.20.1 (forge)"), g.status());
        h.content.version = null;
        assertTrue(g.status().endsWith("Minecraft ? (forge)"), g.status());
    }
}
