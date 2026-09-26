package com.pedromorago.spintrainer.range.adapter.out.persistence;

import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;

/** Filas (situación, stack, mano, acción) agrupadas por rango; se cargan en una consulta para todos los rangos. */
final class HandRows {

    private record Spot(SituationKey situation, Stack stack) {}

    private final Map<Spot, Map<Hand, Action>> bySpot;

    private HandRows(Map<Spot, Map<Hand, Action>> bySpot) {
        this.bySpot = bySpot;
    }

    static HandRows load(StatementSpec query) {
        Map<Spot, Map<Hand, Action>> bySpot = new HashMap<>();
        query.query(rs -> {
            Spot spot = new Spot(SituationKey.of(rs.getString("situation")), Stack.of(rs.getBigDecimal("stack")));
            bySpot.computeIfAbsent(spot, s -> new HashMap<>())
                    .put(Hand.of(rs.getString("hand")), Action.fromCode(rs.getString("action")));
        });
        return new HandRows(bySpot);
    }

    Map<Hand, Action> of(SituationKey situation, Stack stack) {
        return bySpot.getOrDefault(new Spot(situation, stack), Map.of());
    }
}
