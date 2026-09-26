package com.pedromorago.spintrainer.stats.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.shared.kernel.DomainException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DayWindowTest {

    static final Instant NOW = Instant.parse("2026-09-25T22:30:00Z");

    @Test
    void todayDependsOnTheTimeZone() {
        assertThat(DayWindow.lastDays(1, NOW, "UTC").last()).isEqualTo(LocalDate.parse("2026-09-25"));
        assertThat(DayWindow.lastDays(1, NOW, "Europe/Madrid").last()).isEqualTo(LocalDate.parse("2026-09-26"));
    }

    @Test
    void coversTheLastDaysIncludingToday() {
        DayWindow week = DayWindow.lastDays(7, NOW, "Europe/Madrid");

        assertThat(week.first()).isEqualTo(LocalDate.parse("2026-09-20"));
        assertThat(week.start()).isEqualTo(Instant.parse("2026-09-19T22:00:00Z"));
        assertThat(week.end()).isEqualTo(Instant.parse("2026-09-26T22:00:00Z"));
    }

    @Test
    void daylightSavingDaysAreNaturalDays() {
        // 25/10/2026: en Madrid se atrasa la hora; ese día dura 25 horas.
        DayWindow day = DayWindow.lastDays(1, Instant.parse("2026-10-25T12:00:00Z"), "Europe/Madrid");

        assertThat(Duration.between(day.start(), day.end())).isEqualTo(Duration.ofHours(25));
    }

    @Test
    void listsEveryDayWithItsLocalMidnights() {
        List<DayWindow.Day> days = DayWindow.lastDays(3, Instant.parse("2026-10-26T12:00:00Z"), "Europe/Madrid")
                .days();

        assertThat(days)
                .extracting(DayWindow.Day::date)
                .containsExactly(
                        LocalDate.parse("2026-10-24"), LocalDate.parse("2026-10-25"), LocalDate.parse("2026-10-26"));
        assertThat(days.get(1).start()).isEqualTo(Instant.parse("2026-10-24T22:00:00Z"));
        assertThat(days.get(1).end()).isEqualTo(Instant.parse("2026-10-25T23:00:00Z"));
        assertThat(days.get(2).start()).isEqualTo(days.get(1).end());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 366})
    void daysAreBetweenOneAnd365(int days) {
        assertThatThrownBy(() -> DayWindow.lastDays(days, NOW, "UTC"))
                .isInstanceOfSatisfying(
                        DomainException.class,
                        e -> assertThat(e.errors())
                                .extracting(DomainException.FieldError::field)
                                .containsExactly("days"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Nope/Zone", "+01:00", "GMT+1", "europe/madrid", ""})
    void onlyIanaRegionNamesAreAccepted(String timeZone) {
        assertThatThrownBy(() -> DayWindow.lastDays(30, NOW, timeZone))
                .isInstanceOfSatisfying(
                        DomainException.class,
                        e -> assertThat(e.errors())
                                .extracting(DomainException.FieldError::field)
                                .containsExactly("tz"));
    }
}
