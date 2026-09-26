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
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads {@code app.quiz_attempt} (table of the quiz module) only with aggregation SQL: stats is the read side of quiz
 * and does not depend on its classes (ArchUnit).
 */
@Repository
class JdbcAttemptStatistics implements AttemptStatistics {

    private final JdbcClient jdbc;

    JdbcAttemptStatistics(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<HandStat> byHand(UserId user, Optional<SituationKey> situation, Optional<Stack> stack) {
        // Optional filters as fixed fragments; the values go as parameters.
        StringBuilder sql = new StringBuilder("""
                SELECT a.situation, a.stack, a.hand,
                       count(*) AS attempts,
                       count(*) FILTER (WHERE a.correct) AS correct,
                       max(a.answered_at) AS last_answered_at
                FROM app.quiz_attempt a
                JOIN app.situation s ON s.key = a.situation
                WHERE a.user_id = :user""");
        situation.ifPresent(s -> sql.append(" AND a.situation = :situation"));
        stack.ifPresent(s -> sql.append(" AND a.stack = :stack"));
        sql.append(" GROUP BY s.position, a.situation, a.stack, a.hand ORDER BY s.position, a.stack DESC, a.hand");

        JdbcClient.StatementSpec query = jdbc.sql(sql.toString()).param("user", user.value());
        if (situation.isPresent()) {
            query = query.param("situation", situation.get().value());
        }
        if (stack.isPresent()) {
            query = query.param("stack", stack.get().bigBlinds());
        }
        return query.query((rs, row) -> new HandStat(
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
        // Each day's boundaries come from java.time (DST changes included) and travel as instants: Postgres groups
        // without interpreting zone names (with AT TIME ZONE, "CET" would be a fixed offset, not the European zone).
        List<DayWindow.Day> days = window.days();
        return jdbc.sql("""
                        SELECT d.day, count(*) AS attempts, count(*) FILTER (WHERE a.correct) AS correct
                        FROM unnest(CAST(:days AS date[]), CAST(:starts AS timestamptz[]), CAST(:ends AS timestamptz[]))
                             AS d(day, start_at, end_at)
                        JOIN app.quiz_attempt a
                          ON a.user_id = :user AND a.answered_at >= d.start_at AND a.answered_at < d.end_at
                        GROUP BY d.day
                        ORDER BY d.day""")
                .param("user", user.value())
                .param("days", days.stream().map(d -> d.date().toString()).toArray(String[]::new))
                .param("starts", days.stream().map(d -> d.start().toString()).toArray(String[]::new))
                .param("ends", days.stream().map(d -> d.end().toString()).toArray(String[]::new))
                .query((rs, row) -> new ProgressDay(
                        rs.getObject("day", LocalDate.class), rs.getInt("attempts"), rs.getInt("correct")))
                .list();
    }
}
