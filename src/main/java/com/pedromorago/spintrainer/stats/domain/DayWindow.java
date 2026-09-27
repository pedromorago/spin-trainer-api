package com.pedromorago.spintrainer.stats.domain;

import com.pedromorago.spintrainer.shared.kernel.DomainException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * The last {@code days} calendar days up to today (included) in an IANA time zone. The boundaries are local midnights,
 * so a DST-change day lasts 23 or 25 hours and no attempt falls on the wrong day.
 */
public record DayWindow(ZoneId zone, LocalDate first, LocalDate last) {

    public static final int MAX_DAYS = 365;

    public static DayWindow lastDays(int days, Instant now, String timeZone) {
        if (days < 1 || days > MAX_DAYS) {
            throw DomainException.validation("days", "days must be between 1 and " + MAX_DAYS);
        }
        ZoneId zone = zone(timeZone);
        LocalDate today = LocalDate.ofInstant(now, zone);
        return new DayWindow(zone, today.minusDays(days - 1L), today);
    }

    /** First instant of the window (local midnight of the first day). */
    public Instant start() {
        return first.atStartOfDay(zone).toInstant();
    }

    /** Exclusive end: local midnight of the day after today. */
    public Instant end() {
        return last.plusDays(1).atStartOfDay(zone).toInstant();
    }

    /** Each day of the window with its boundaries as instants: the database groups without interpreting time zones. */
    public List<Day> days() {
        return first.datesUntil(last.plusDays(1))
                .map(date -> new Day(
                        date,
                        date.atStartOfDay(zone).toInstant(),
                        date.plusDays(1).atStartOfDay(zone).toInstant()))
                .toList();
    }

    /** A calendar day in the zone: from {@code start} (included) to {@code end} (excluded). */
    public record Day(LocalDate date, Instant start, Instant end) {}

    /** Only IANA zone names ({@code Europe/Madrid}, {@code UTC}), as the contract says; no fixed offsets. */
    private static ZoneId zone(String timeZone) {
        if (timeZone == null || !ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw DomainException.validation("tz", "unknown IANA time zone");
        }
        return ZoneId.of(timeZone);
    }
}
