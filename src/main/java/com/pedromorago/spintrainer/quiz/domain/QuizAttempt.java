package com.pedromorago.spintrainer.quiz.domain;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.range.domain.RangeRules;
import com.pedromorago.spintrainer.range.domain.RangeSource;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Respuesta a una mano del Quiz: un evento inmutable (ADR-0007). Guarda la acción esperada en ese momento y de qué rango
 * salió, así que las estadísticas siguen siendo válidas aunque el rango cambie después (ADR-0006, ADR-0012).
 */
public record QuizAttempt(
        UUID id,
        UserId user,
        SituationKey situation,
        Stack stack,
        Hand hand,
        Action given,
        Action expected,
        boolean correct,
        RangeSource rangeSource,
        int rangeVersion,
        Instant answeredAt) {

    public QuizAttempt {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(situation, "situation");
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(given, "given");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(rangeSource, "rangeSource");
        Objects.requireNonNull(answeredAt, "answeredAt");
        if (correct != (given == expected)) {
            throw new IllegalArgumentException("correct no coincide con given/expected");
        }
    }

    /**
     * Corrige la respuesta en el servidor: el cliente solo dice qué hizo; la acción esperada sale del rango efectivo
     * con la misma regla que el feedback local de la web.
     *
     * @throws DomainException VALIDATION si la acción no existe en la situación
     */
    public static QuizAttempt grade(
            UUID id, UserId user, Situation situation, Stack stack, Hand hand, Action given, Range range, Instant at) {
        if (!situation.allows(given)) {
            throw DomainException.validation("given", "acción " + given.code() + " no permitida en " + situation.key());
        }
        Action expected = RangeRules.actionFor(range.hands(), hand, situation);
        return new QuizAttempt(
                id,
                user,
                situation.key(),
                stack,
                hand,
                given,
                expected,
                given == expected,
                range.source(),
                range.version(),
                at);
    }
}
