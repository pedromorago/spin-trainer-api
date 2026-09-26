package com.pedromorago.spintrainer.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import com.pedromorago.spintrainer.stats.application.port.out.AttemptStatistics;
import com.pedromorago.spintrainer.stats.domain.DayWindow;
import com.pedromorago.spintrainer.stats.domain.HandStat;
import com.pedromorago.spintrainer.stats.domain.ProgressDay;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The stats use cases delegate the aggregation to the database; the day window comes from the injected clock. */
class StatsServiceTest {

    static final Instant NOW = Instant.parse("2026-09-25T22:30:00Z");

    final UserId user = new UserId(UUID.randomUUID());
    final HandStat row = new HandStat(SituationKey.of("btn_open"), Stack.of(25), Hand.of("AA"), 2, 1, NOW);
    final ProgressDay day = new ProgressDay(LocalDate.parse("2026-09-26"), 2, 1);
    final List<DayWindow> windows = new ArrayList<>();

    final AttemptStatistics statistics = new AttemptStatistics() {
        @Override
        public List<HandStat> byHand(UserId u, Optional<SituationKey> situation, Optional<Stack> stack) {
            return u.equals(user) && situation.isPresent() && stack.isPresent() ? List.of(row) : List.of();
        }

        @Override
        public List<ProgressDay> byDay(UserId u, DayWindow window) {
            windows.add(window);
            return List.of(day);
        }
    };
    final StatsService service = new StatsService(statistics, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void handsAreTheUsersAggregatesWithTheirFilters() {
        assertThat(service.byHand(user, Optional.of(SituationKey.of("btn_open")), Optional.of(Stack.of(25))))
                .containsExactly(row);
    }

    @Test
    void progressUsesTheLastDaysInTheRequestedTimeZone() {
        assertThat(service.byDay(user, 7, "Europe/Madrid")).containsExactly(day);

        assertThat(windows).singleElement().satisfies(window -> {
            assertThat(window.last()).isEqualTo(LocalDate.parse("2026-09-26"));
            assertThat(window.first()).isEqualTo(LocalDate.parse("2026-09-20"));
        });
    }

    @Test
    void anInvalidWindowNeverReachesTheDatabase() {
        assertThatThrownBy(() -> service.byDay(user, 0, "UTC")).isInstanceOf(DomainException.class);
        assertThat(windows).isEmpty();
    }
}
