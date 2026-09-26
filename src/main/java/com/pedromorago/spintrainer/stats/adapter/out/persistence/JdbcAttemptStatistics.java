package com.pedromorago.spintrainer.stats.adapter.out.persistence;

import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import com.pedromorago.spintrainer.stats.application.port.out.AttemptStatistics;
import com.pedromorago.spintrainer.stats.domain.DayWindow;
import com.pedromorago.spintrainer.stats.domain.HandStat;
import com.pedromorago.spintrainer.stats.domain.ProgressDay;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lee {@code app.quiz_attempt} (tabla del módulo quiz) solo con SQL de agregación: stats es el lado de lectura de quiz
 * y no depende de sus clases (ArchUnit).
 */
@Repository
class JdbcAttemptStatistics implements AttemptStatistics {

    private final JdbcClient jdbc;

    JdbcAttemptStatistics(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<HandStat> byHand(UserId user, Optional<SituationKey> situation, Optional<Stack> stack) {
        return jdbc.sql("""
                        SELECT a.situation, a.stack, a.hand,
                               count(*) AS attempts,
                               count(*) FILTER (WHERE a.correct) AS correct,
                               max(a.answered_at) AS last_answered_at
                        FROM app.quiz_attempt a
                        JOIN app.situation s ON s.key = a.situation
                        WHERE a.user_id = :user
                          AND (CAST(:situation AS text) IS NULL OR a.situation = :situation)
                          AND (CAST(:stack AS numeric) IS NULL OR a.stack = :stack)
                        GROUP BY s.position, a.situation, a.stack, a.hand
                        ORDER BY s.position, a.stack DESC, a.hand""")
                .param("user", user.value())
                .param("situation", situation.map(SituationKey::value).orElse(null))
                .param("stack", stack.map(Stack::bigBlinds).orElse(null))
                .query((rs, row) -> new HandStat(
                        SituationKey.of(rs.getString("situation")),
                        Stack.of(rs.getBigDecimal("stack")),
                        Hand.of(rs.getString("hand")),
                        rs.getInt("attempts"),
                        rs.getInt("correct"),
                        rs.getObject("last_answered_at", OffsetDateTime.class).toInstant()))
                .list();
    }

    @Override
    public List<ProgressDay> byDay(UserId user, DayWindow window) {
        // El día se corta en la zona pedida (nombre IANA ya validado); el filtro usa instantes para aprovechar el
        // índice.
        return jdbc.sql("""
                        SELECT CAST(answered_at AT TIME ZONE :zone AS date) AS day,
                               count(*) AS attempts,
                               count(*) FILTER (WHERE correct) AS correct
                        FROM app.quiz_attempt
                        WHERE user_id = :user AND answered_at >= :start AND answered_at < :end
                        GROUP BY day
                        ORDER BY day""")
                .param("zone", window.zone().getId())
                .param("user", user.value())
                .param("start", window.start().atOffset(ZoneOffset.UTC))
                .param("end", window.end().atOffset(ZoneOffset.UTC))
                .query((rs, row) -> new ProgressDay(
                        rs.getObject("day", LocalDate.class), rs.getInt("attempts"), rs.getInt("correct")))
                .list();
    }
}
