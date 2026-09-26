package com.pedromorago.spintrainer.stats.adapter.in.rest;

import com.pedromorago.spintrainer.api.StatsApi;
import com.pedromorago.spintrainer.api.model.HandStatDto;
import com.pedromorago.spintrainer.api.model.ProgressDayDto;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.security.CurrentUser;
import com.pedromorago.spintrainer.stats.application.port.in.ReadStats;
import com.pedromorago.spintrainer.stats.domain.HandStat;
import com.pedromorago.spintrainer.stats.domain.ProgressDay;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StatsController implements StatsApi {

    private final ReadStats stats;
    private final CurrentUser currentUser;

    StatsController(ReadStats stats, CurrentUser currentUser) {
        this.stats = stats;
        this.currentUser = currentUser;
    }

    @Override
    public ResponseEntity<List<HandStatDto>> getHandStats(@Nullable String situation, @Nullable BigDecimal stack) {
        return ResponseEntity.ok(stats
                .byHand(
                        currentUser.id(),
                        Optional.ofNullable(situation).map(SituationKey::of),
                        Optional.ofNullable(stack).map(Stack::of))
                .stream()
                .map(StatsController::toDto)
                .toList());
    }

    @Override
    public ResponseEntity<List<ProgressDayDto>> getProgress(Integer days, String tz) {
        return ResponseEntity.ok(stats.byDay(currentUser.id(), days, tz).stream()
                .map(StatsController::toDto)
                .toList());
    }

    private static HandStatDto toDto(HandStat stat) {
        return new HandStatDto(
                stat.situation().value(),
                stat.stack().bigBlinds(),
                stat.hand().code(),
                stat.attempts(),
                stat.correct(),
                stat.lastAnsweredAt().atOffset(ZoneOffset.UTC));
    }

    private static ProgressDayDto toDto(ProgressDay day) {
        return new ProgressDayDto(day.date(), day.attempts(), day.correct());
    }
}
