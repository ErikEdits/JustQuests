package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import com.erikedits.justquests.generator.v2.api.StartResult;
import com.erikedits.justquests.generator.v2.internal.state.CycleClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.10 — catch-up after downtime, clock moving backwards, boundary alignment. */
class ClockTest {
    private static final long HOUR = 3_600_000L;
    private static final long DAY = 24 * HOUR;

    private static long rotations(FakeHost h) {
        return h.log.lines.stream().filter(l -> l.contains("rotation (")).count();
    }

    @Test
    void catchUpAfterFiveDaysRotatesOnce() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        g.stop();
        host.now += 5 * DAY + 3 * HOUR;
        long before = rotations(host);
        QuestGeneratorV2 g2 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        StartResult r = g2.start(Map.of());
        assertEquals("catch-up", r.initialRotation().reason());
        assertTrue(r.initialRotation().changed());
        assertEquals(before + 1, rotations(host), "exactly one rotation");
        CycleClock clock = new CycleClock(12, 0, ZoneId.of("UTC"));
        assertEquals(clock.boundaryAtOrBefore(host.now) / 1000L, r.initialRotation().cycleId());
        assertFalse(g2.tick().changed(), "no further rotation");
    }

    @Test
    void catchUpViaTick() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        host.now += 5 * DAY;
        long before = rotations(host);
        RotationResult r = g.tick();
        assertEquals("catch-up", r.reason());
        assertEquals(before + 1, rotations(host));
        assertFalse(g.tick().changed());
        host.now += 12 * HOUR;
        assertEquals("cycle", g.tick().reason());
    }

    @Test
    void clockGoingBackwardsNeverRotatesBackwards() {
        FakeHost host = new FakeHost();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        g.start(Map.of());
        var served = g.servedQuests();
        host.now -= 3 * DAY;
        assertFalse(g.tick().changed());
        assertEquals(served, g.servedQuests());
        g.stop();
        QuestGeneratorV2 g2 = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 5));
        StartResult r = g2.start(Map.of());
        assertFalse(r.initialRotation().changed());
        assertEquals(served, g2.servedQuests());
        assertTrue(host.log.lines.stream().anyMatch(l -> l.contains("clock is before")));
        host.now += 3 * DAY; // back to "now": still the same cycle
        assertFalse(g2.tick().changed());
        host.now += 12 * HOUR; // real time passes the next boundary
        assertTrue(g2.tick().changed());
    }

    @Test
    void boundariesAlignToLocalHours() {
        CycleClock utc = new CycleClock(12, 0, ZoneId.of("UTC"));
        long b = utc.boundaryAtOrBefore(1_790_000_000_000L);
        assertEquals(0, (b / 1000L) % (12 * 3600));
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        CycleClock local = new CycleClock(12, 0, berlin);
        long t = ZonedDateTime.of(2026, 3, 29, 1, 0, 0, 0, berlin).toInstant().toEpochMilli(); // DST day
        for (int i = 0; i < 10; i++) {
            long bb = local.boundaryAtOrBefore(t);
            int hour = ZonedDateTime.ofInstant(Instant.ofEpochMilli(bb), berlin).getHour();
            assertTrue(hour == 0 || hour == 12, "boundary at local hour " + hour);
            long next = local.nextBoundaryAfter(bb);
            assertTrue(next > bb);
            t = next + 1;
        }
        CycleClock anchored = new CycleClock(24, 6, ZoneId.of("UTC"));
        long a = anchored.boundaryAtOrBefore(1_790_000_000_000L);
        assertEquals(6 * 3600, (a / 1000L) % (24 * 3600));
        CycleClock hourly = new CycleClock(1, 0, ZoneId.of("UTC"));
        assertEquals(3_600_000L, hourly.nextBoundaryAfter(hourly.boundaryAtOrBefore(1_790_000_000_000L))
            - hourly.boundaryAtOrBefore(1_790_000_000_000L));
    }

    @Test
    void configurableCycleLength() {
        FakeHost host = new FakeHost();
        GeneratorConfig cfg = TestSupport.config(Difficulty.NORMAL, 5).toBuilder().cycleHours(1).build();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, cfg);
        g.start(Map.of());
        host.now += HOUR;
        assertTrue(g.tick().changed());
    }
}
