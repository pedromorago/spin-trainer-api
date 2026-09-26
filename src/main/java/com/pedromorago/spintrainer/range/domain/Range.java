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
 * Range of a situation and stack: explicit action per hand (absent ones have the implicit action of the situation).
 * {@code version}: in reference ranges, the seed's; in user ranges, the one for optimistic concurrency.
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
