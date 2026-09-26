package com.pedromorago.spintrainer.stats.application;

import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import com.pedromorago.spintrainer.stats.application.port.in.ReadStats;
import com.pedromorago.spintrainer.stats.application.port.out.AttemptStatistics;
import com.pedromorago.spintrainer.stats.domain.DayWindow;
import com.pedromorago.spintrainer.stats.domain.HandStat;
import com.pedromorago.spintrainer.stats.domain.ProgressDay;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class StatsService implements ReadStats {

    private final AttemptStatistics statistics;
    private final Clock clock;

    StatsService(AttemptStatistics statistics, Clock clock) {
        this.statistics = statistics;
        this.clock = clock;
    }

    @Override
    public List<HandStat> byHand(UserId user, Optional<SituationKey> situation, Optional<Stack> stack) {
        return statistics.byHand(user, situation, stack);
    }

    @Override
    public List<ProgressDay> byDay(UserId user, int days, String timeZone) {
        return statistics.byDay(user, DayWindow.lastDays(days, clock.instant(), timeZone));
    }
}
