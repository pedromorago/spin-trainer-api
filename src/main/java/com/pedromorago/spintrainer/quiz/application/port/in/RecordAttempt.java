package com.pedromorago.spintrainer.quiz.application.port.in;

import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;

public interface RecordAttempt {

    /**
     * Corrige y guarda una respuesta del Quiz contra el rango efectivo del usuario en este momento.
     *
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException NOT_FOUND (combinación desconocida), VALIDATION
     *     (mano o acción no válidas) o NO_RANGE (la combinación no tiene rango con el que corregir)
     */
    QuizAttempt record(UserId user, SituationKey situation, Stack stack, String hand, Action given);
}
