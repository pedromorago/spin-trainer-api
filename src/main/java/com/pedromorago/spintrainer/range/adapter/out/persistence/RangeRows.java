package com.pedromorago.spintrainer.range.adapter.out.persistence;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.range.domain.RangeSource;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;

/**
 * Builds ranges from a single {@code range LEFT JOIN hands} query (one row per hand, or a single row without a hand if
 * the range is empty). A single statement sees a single snapshot: the version and the hands always correspond to the
 * same write, even if another PUT finishes midway through the read (otherwise the optimistic version control could
 * accept a half-read range as current).
 */
final class RangeRows {

    private record Spot(SituationKey situation, Stack stack) {}

    private static final class Builder {
        final Spot spot;
        final int version;
        final Optional<Instant> updatedAt;
        final Map<Hand, Action> hands = new HashMap<>();

        Builder(Spot spot, int version, Optional<Instant> updatedAt) {
            this.spot = spot;
            this.version = version;
            this.updatedAt = updatedAt;
        }
    }

    private RangeRows() {}

    /**
     * @param query columns {@code situation, stack, version, hand, action} and, if {@code source} is USER,
     *     {@code updated_at}; ordered as the ranges must come out
     */
    static List<Range> load(StatementSpec query, RangeSource source) {
        Map<Spot, Builder> ranges = new LinkedHashMap<>();
        query.query((ResultSet rs) -> {
            Spot spot = new Spot(SituationKey.of(rs.getString("situation")), Stack.of(rs.getBigDecimal("stack")));
            Builder range = ranges.computeIfAbsent(spot, s -> newBuilder(s, rs, source));
            String hand = rs.getString("hand");
            if (hand != null) {
                range.hands.put(Hand.of(hand), Action.fromCode(rs.getString("action")));
            }
        });
        List<Range> result = new ArrayList<>(ranges.size());
        ranges.values()
                .forEach(r -> result.add(
                        new Range(r.spot.situation(), r.spot.stack(), r.hands, source, r.version, r.updatedAt)));
        return result;
    }

    private static Builder newBuilder(Spot spot, ResultSet rs, RangeSource source) {
        try {
            Optional<Instant> updatedAt = source == RangeSource.USER
                    ? Optional.of(
                            rs.getObject("updated_at", OffsetDateTime.class).toInstant())
                    : Optional.empty();
            return new Builder(spot, rs.getInt("version"), updatedAt);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
