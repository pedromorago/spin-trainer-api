package com.pedromorago.spintrainer.range.adapter.out.persistence;

import com.pedromorago.spintrainer.range.application.port.out.UserRangeRepository;
import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.range.domain.RangeSource;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Rangos personalizados. La versión se comprueba en la propia sentencia ({@code UPDATE ... WHERE version = ?}), así
 * que dos escrituras concurrentes sobre la misma versión no pueden ganar las dos.
 */
@Repository
class JdbcUserRangeRepository implements UserRangeRepository {

    // Versión y manos en una sola sentencia (misma instantánea): ver RangeRows.
    private static final String SELECT = """
            SELECT r.situation, r.stack, r.version, r.updated_at, h.hand, h.action
            FROM app.user_range r
            JOIN app.situation s ON s.key = r.situation
            LEFT JOIN app.user_range_hand h
                   ON h.user_id = r.user_id AND h.situation = r.situation AND h.stack = r.stack
            """;

    private final JdbcClient jdbc;

    JdbcUserRangeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Range> findAll(UserId user) {
        return RangeRows.load(
                jdbc.sql(SELECT + "WHERE r.user_id = :user ORDER BY s.position, r.stack DESC")
                        .param("user", user.value()),
                RangeSource.USER);
    }

    @Override
    public Optional<Range> find(UserId user, SituationKey situation, Stack stack) {
        return RangeRows.load(
                        jdbc.sql(SELECT + "WHERE r.user_id = :user AND r.situation = :situation AND r.stack = :stack")
                                .param("user", user.value())
                                .param("situation", situation.value())
                                .param("stack", stack.bigBlinds()),
                        RangeSource.USER)
                .stream()
                .findFirst();
    }

    @Override
    public boolean insert(UserId user, Range range) {
        int inserted = jdbc.sql("""
                        INSERT INTO app.user_range (user_id, situation, stack, version, updated_at)
                        VALUES (:user, :situation, :stack, :version, :updatedAt)
                        ON CONFLICT DO NOTHING""")
                .param("user", user.value())
                .param("situation", range.situation().value())
                .param("stack", range.stack().bigBlinds())
                .param("version", range.version())
                .param("updatedAt", updatedAt(range))
                .update();
        if (inserted == 0) {
            return false;
        }
        insertHands(user, range);
        return true;
    }

    @Override
    public boolean replace(UserId user, Range range, int expectedVersion) {
        int updated = jdbc.sql("""
                        UPDATE app.user_range SET version = :version, updated_at = :updatedAt
                        WHERE user_id = :user AND situation = :situation AND stack = :stack
                          AND version = :expectedVersion""")
                .param("user", user.value())
                .param("situation", range.situation().value())
                .param("stack", range.stack().bigBlinds())
                .param("version", range.version())
                .param("updatedAt", updatedAt(range))
                .param("expectedVersion", expectedVersion)
                .update();
        if (updated == 0) {
            return false;
        }
        jdbc.sql("DELETE FROM app.user_range_hand WHERE user_id = :user AND situation = :situation AND stack = :stack")
                .param("user", user.value())
                .param("situation", range.situation().value())
                .param("stack", range.stack().bigBlinds())
                .update();
        insertHands(user, range);
        return true;
    }

    @Override
    public void delete(UserId user, SituationKey situation, Stack stack) {
        jdbc.sql("DELETE FROM app.user_range WHERE user_id = :user AND situation = :situation AND stack = :stack")
                .param("user", user.value())
                .param("situation", situation.value())
                .param("stack", stack.bigBlinds())
                .update();
    }

    /** Todas las manos en una sentencia (unnest de dos arrays paralelos). */
    private void insertHands(UserId user, Range range) {
        if (range.hands().isEmpty()) {
            return;
        }
        String[] hands = range.hands().keySet().stream().map(Hand::code).toArray(String[]::new);
        String[] actions = range.hands().values().stream().map(Action::code).toArray(String[]::new);
        jdbc.sql("""
                        INSERT INTO app.user_range_hand (user_id, situation, stack, hand, action)
                        SELECT :user, :situation, :stack, h.hand, h.action
                        FROM unnest(CAST(:hands AS text[]), CAST(:actions AS text[])) AS h(hand, action)""")
                .param("user", user.value())
                .param("situation", range.situation().value())
                .param("stack", range.stack().bigBlinds())
                .param("hands", hands)
                .param("actions", actions)
                .update();
    }

    private static OffsetDateTime updatedAt(Range range) {
        return range.updatedAt().orElseThrow().atOffset(ZoneOffset.UTC);
    }
}
