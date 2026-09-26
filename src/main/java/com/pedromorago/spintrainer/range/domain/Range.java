package com.pedromorago.spintrainer.range.domain;

import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Rango de una situación y stack: acción explícita por mano (las ausentes tienen la implícita de la situación).
 * {@code version}: en los de referencia, la del seed; en los del usuario, la de la concurrencia optimista.
 */
public record Range(
        SituationKey situation,
        Stack stack,
        Map<Hand, Action> hands,
        RangeSource source,
        int version,
        Optional<Instant> updatedAt) {

    public Range {
        Objects.requireNonNull(situation, "situation");
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(updatedAt, "updatedAt");
        hands = Collections.unmodifiableMap(new TreeMap<>(hands));
        if (version < 1) {
            throw new IllegalArgumentException("version debe ser ≥ 1");
        }
    }
}
