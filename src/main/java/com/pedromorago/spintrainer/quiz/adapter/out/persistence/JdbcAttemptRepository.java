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
        // Condiciones opcionales como fragmentos fijos (nunca texto del usuario; los valores van como parámetros): así
        // la comparación de fila del cursor usa el índice (user_id, answered_at DESC, id DESC) en todas las páginas.
        StringBuilder sql = new StringBuilder("""
                SELECT id, user_id, situation, stack, hand, given, expected, correct, range_source, range_version,
                       answered_at
                FROM app.quiz_attempt
                WHERE user_id = :user""");
        situation.ifPresent(s -> sql.append(" AND situation = :situation"));
        stack.ifPresent(s -> sql.append(" AND stack = :stack"));
        after.ifPresent(p -> sql.append(" AND (answered_at, id) < (:afterAt, :afterId)"));
        sql.append(" ORDER BY answered_at DESC, id DESC LIMIT :limit");

        JdbcClient.StatementSpec query =
                jdbc.sql(sql.toString()).param("user", user.value()).param("limit", limit);
        if (situation.isPresent()) {
            query = query.param("situation", situation.get().value());
        }
        if (stack.isPresent()) {
            query = query.param("stack", stack.get().bigBlinds());
        }
        if (after.isPresent()) {
            query = query.param("afterAt", after.get().answeredAt().atOffset(ZoneOffset.UTC))
                    .param("afterId", after.get().id());
        }
        return query.query(JdbcAttemptRepository::toAttempt).list();
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
