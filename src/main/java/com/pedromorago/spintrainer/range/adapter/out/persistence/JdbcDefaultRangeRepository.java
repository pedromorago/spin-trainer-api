package com.pedromorago.spintrainer.range.adapter.out.persistence;

import com.pedromorago.spintrainer.range.application.port.out.DefaultRangeRepository;
import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.range.domain.RangeSource;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Rangos de referencia: solo lectura (los escribe el seed de Flyway; spin_app no tiene permiso de escritura). */
@Repository
class JdbcDefaultRangeRepository implements DefaultRangeRepository {

    private static final String SELECT = """
            SELECT r.situation, r.stack, r.version, h.hand, h.action
            FROM app.default_range r
            JOIN app.situation s ON s.key = r.situation
            LEFT JOIN app.default_range_hand h ON h.situation = r.situation AND h.stack = r.stack
            """;

    private final JdbcClient jdbc;

    JdbcDefaultRangeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Range> findAll() {
        return RangeRows.load(jdbc.sql(SELECT + "ORDER BY s.position, r.stack DESC"), RangeSource.DEFAULT);
    }

    @Override
    public Optional<Range> find(SituationKey situation, Stack stack) {
        return RangeRows.load(
                        jdbc.sql(SELECT + "WHERE r.situation = :situation AND r.stack = :stack")
                                .param("situation", situation.value())
                                .param("stack", stack.bigBlinds()),
                        RangeSource.DEFAULT)
                .stream()
                .findFirst();
    }
}
