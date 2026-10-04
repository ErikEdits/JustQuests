package com.erikedits.justquests.plugin.gen;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

/** Day and week numbers for the personal and weekly renewals, and seeds mixed from them. */
final class GenTime {
    private GenTime() {}

    /** The day number (epoch day) whose renewal at {@code hour} last passed. */
    static long day(int hour) {
        return ZonedDateTime.now(ZoneId.systemDefault()).minusHours(hour).toLocalDate().toEpochDay();
    }

    /** The week number (epoch day of its start) whose renewal on {@code day} at {@code hour} last passed. */
    static long week(DayOfWeek day, int hour) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime start = now.with(TemporalAdjusters.previousOrSame(day)).toLocalDate().atStartOfDay(now.getZone()).plusHours(hour);
        if (start.isAfter(now)) start = start.minusWeeks(1);
        return start.toLocalDate().toEpochDay();
    }

    /** Time until the week {@code week} ends. */
    static Duration weekLeft(long week, int hour) {
        ZonedDateTime end = LocalDate.ofEpochDay(week).plusDays(7).atStartOfDay(ZoneId.systemDefault()).plusHours(hour);
        Duration d = Duration.between(ZonedDateTime.now(ZoneId.systemDefault()), end);
        return d.isNegative() ? Duration.ZERO : d;
    }

    static long mix(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L + b;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return h;
    }

    static long mix(long seed, UUID player, long day) {
        return mix(mix(seed, player.getMostSignificantBits() ^ player.getLeastSignificantBits()), day);
    }
}
