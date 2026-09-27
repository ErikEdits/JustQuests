package com.erikedits.justquests.generator.v2.internal.state;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Rotation boundaries on a wall-clock grid: {@code anchorHour + k × cycleHours} local time in the
 * configured zone (00:00 and 12:00 by default). DST-aware: the grid is in local time, so a boundary
 * stays at the same local hour across DST changes.
 */
public final class CycleClock {
    private static final LocalDateTime GRID_BASE = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final int cycleHours;
    private final int anchorHour;
    private final ZoneId zone;

    public CycleClock(int cycleHours, int anchorHour, ZoneId zone) {
        this.cycleHours = Math.max(1, cycleHours);
        this.anchorHour = Math.max(0, Math.min(23, anchorHour));
        this.zone = zone;
    }

    private long boundaryMillis(long k) {
        LocalDateTime t = GRID_BASE.plusHours(anchorHour).plusHours(k * cycleHours);
        return t.atZone(zone).toInstant().toEpochMilli();
    }

    /** Latest boundary at or before {@code nowMillis} (epoch ms). */
    public long boundaryAtOrBefore(long nowMillis) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone);
        long hours = ChronoUnit.HOURS.between(GRID_BASE.plusHours(anchorHour), now);
        long k = Math.floorDiv(hours, cycleHours);
        while (boundaryMillis(k) > nowMillis) {
            k--;
        }
        while (boundaryMillis(k + 1) <= nowMillis) {
            k++;
        }
        return boundaryMillis(k);
    }

    /** First boundary strictly after {@code boundaryMillis}. */
    public long nextBoundaryAfter(long boundaryMillis) {
        long b = boundaryAtOrBefore(boundaryMillis);
        LocalDateTime local = LocalDateTime.ofInstant(Instant.ofEpochMilli(b), zone);
        long hours = ChronoUnit.HOURS.between(GRID_BASE.plusHours(anchorHour), local);
        long k = Math.floorDiv(hours, cycleHours);
        long next = boundaryMillis(k + 1);
        while (next <= boundaryMillis) {
            k++;
            next = boundaryMillis(k + 1);
        }
        return next;
    }
}
