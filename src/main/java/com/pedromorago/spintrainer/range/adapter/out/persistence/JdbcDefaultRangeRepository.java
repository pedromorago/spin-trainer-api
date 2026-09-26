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

    private final JdbcClient jdbc;

    JdbcDefaultRangeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Range> findAll() {
        HandRows hands = HandRows.load(jdbc.sql("SELECT situation, stack, hand, action FROM app.default_range_hand"));
        return jdbc.sql("""
                        SELECT r.situation, r.stack, r.version
                        FROM app.default_range r JOIN app.situation s ON s.key = r.situation
                        ORDER BY s.position, r.stack DESC""")
                .query((rs, row) -> toRange(
                        rs.getString("situation"), Stack.of(rs.getBigDecimal("stack")), rs.getInt("version"), hands))
                .list();
    }

    @Override
    public Optional<Range> find(SituationKey situation, Stack stack) {
        HandRows hands = HandRows.load(
                jdbc.sql("""
                        SELECT situation, stack, hand, action FROM app.default_range_hand
                        WHERE situation = :situation AND stack = :stack""").param("situation", situation.value()).param("stack", stack.bigBlinds()));
        return jdbc.sql("SELECT version FROM app.default_range WHERE situation = :situation AND stack = :stack")
                .param("situation", situation.value())
                .param("stack", stack.bigBlinds())
                .query((rs, row) -> toRange(situation.value(), stack, rs.getInt("version"), hands))
                .optional();
    }

    private static Range toRange(String situation, Stack stack, int version, HandRows hands) {
        SituationKey key = SituationKey.of(situation);
        return new Range(key, stack, hands.of(key, stack), RangeSource.DEFAULT, version, Optional.empty());
    }
}
