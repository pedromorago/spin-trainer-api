package com.pedromorago.spintrainer.stats.domain;

import com.pedromorago.spintrainer.shared.kernel.DomainException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Los últimos {@code days} días naturales hasta hoy (incluido) en una zona horaria IANA. Los límites son medianoches
 * locales, así que un día de cambio de hora dura 23 o 25 horas y ningún intento cae en el día equivocado.
 */
public record DayWindow(ZoneId zone, LocalDate first, LocalDate last) {

    public static final int MAX_DAYS = 365;

    public static DayWindow lastDays(int days, Instant now, String timeZone) {
        if (days < 1 || days > MAX_DAYS) {
            throw DomainException.validation("days", "days debe estar entre 1 y " + MAX_DAYS);
        }
        ZoneId zone = zone(timeZone);
        LocalDate today = LocalDate.ofInstant(now, zone);
        return new DayWindow(zone, today.minusDays(days - 1L), today);
    }

    /** Primer instante de la ventana (medianoche local del primer día). */
    public Instant start() {
        return first.atStartOfDay(zone).toInstant();
    }

    /** Fin exclusivo: medianoche local del día siguiente a hoy. */
    public Instant end() {
        return last.plusDays(1).atStartOfDay(zone).toInstant();
    }

    /** Cada día de la ventana con sus límites en instantes: la base de datos agrupa sin interpretar zonas horarias. */
    public List<Day> days() {
        return first.datesUntil(last.plusDays(1))
                .map(date -> new Day(
                        date,
                        date.atStartOfDay(zone).toInstant(),
                        date.plusDays(1).atStartOfDay(zone).toInstant()))
                .toList();
    }

    /** Un día natural en la zona: de {@code start} (incluido) a {@code end} (excluido). */
    public record Day(LocalDate date, Instant start, Instant end) {}

    /** Solo nombres de zona IANA ({@code Europe/Madrid}, {@code UTC}), como dice el contrato; no desfases fijos. */
    private static ZoneId zone(String timeZone) {
        if (timeZone == null || !ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw DomainException.validation("tz", "zona IANA desconocida");
        }
        return ZoneId.of(timeZone);
    }
}
