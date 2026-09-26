package com.pedromorago.spintrainer.quiz.adapter.out.persistence;

import com.pedromorago.spintrainer.quiz.application.port.out.AttemptRepository;
import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.range.domain.RangeSource;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAttemptRepository implements AttemptRepository {

    private final JdbcClient jdbc;

    JdbcAttemptRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(QuizAttempt attempt) {
        jdbc.sql("""
                        INSERT INTO app.quiz_attempt
                            (id, user_id, situation, stack, hand, given, expected, correct, range_source, range_version,
                             answered_at)
                        VALUES (:id, :user, :situation, :stack, :hand, :given, :expected, :correct, :rangeSource,
                                :rangeVersion, :answeredAt)""")
                .param("id", attempt.id())
                .param("user", attempt.user().value())
                .param("situation", attempt.situation().value())
                .param("stack", attempt.stack().bigBlinds())
                .param("hand", attempt.hand().code())
                .param("given", attempt.given().code())
                .param("expected", attempt.expected().code())
                .param("correct", attempt.correct())
                .param("rangeSource", attempt.rangeSource().code())
                .param("rangeVersion", attempt.rangeVersion())
                .param("answeredAt", attempt.answeredAt().atOffset(ZoneOffset.UTC))
                .update();
    }

    @Override
    public List<QuizAttempt> findPage(
            UserId user, Optional<SituationKey> situation, Optional<Stack> stack, Optional<Position> after, int limit) {
        // Filtros opcionales con parámetros tipados (sin concatenar SQL): un filtro nulo no restringe.
        return jdbc.sql("""
                        SELECT id, user_id, situation, stack, hand, given, expected, correct, range_source,
                               range_version, answered_at
                        FROM app.quiz_attempt
                        WHERE user_id = :user
                          AND (CAST(:situation AS text) IS NULL OR situation = :situation)
                          AND (CAST(:stack AS numeric) IS NULL OR stack = :stack)
                          AND (CAST(:afterAt AS timestamptz) IS NULL OR (answered_at, id) < (:afterAt, :afterId))
                        ORDER BY answered_at DESC, id DESC
                        LIMIT :limit""")
                .param("user", user.value())
                .param("situation", situation.map(SituationKey::value).orElse(null))
                .param("stack", stack.map(Stack::bigBlinds).orElse(null))
                .param(
                        "afterAt",
                        after.map(p -> p.answeredAt().atOffset(ZoneOffset.UTC)).orElse(null))
                .param("afterId", after.map(Position::id).orElse(null))
                .param("limit", limit)
                .query(JdbcAttemptRepository::toAttempt)
                .list();
    }

    private static QuizAttempt toAttempt(ResultSet rs, int row) throws SQLException {
        return new QuizAttempt(
                rs.getObject("id", UUID.class),
                new UserId(rs.getObject("user_id", UUID.class)),
                SituationKey.of(rs.getString("situation")),
                Stack.of(rs.getBigDecimal("stack")),
                Hand.of(rs.getString("hand")),
                Action.fromCode(rs.getString("given")),
                Action.fromCode(rs.getString("expected")),
                rs.getBoolean("correct"),
                "user".equals(rs.getString("range_source")) ? RangeSource.USER : RangeSource.DEFAULT,
                rs.getInt("range_version"),
                rs.getObject("answered_at", OffsetDateTime.class).toInstant());
    }
}
