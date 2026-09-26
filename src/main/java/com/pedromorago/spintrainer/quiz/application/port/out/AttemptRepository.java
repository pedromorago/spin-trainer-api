package com.pedromorago.spintrainer.quiz.application.port.out;

import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptRepository {

    /** Solo inserta: los intentos no se modifican ni se borran (el rol de la API no tiene permiso para ello). */
    void insert(QuizAttempt attempt);

    /**
     * Hasta {@code limit} intentos del usuario anteriores a {@code after} (si hay), ordenados por
     * ({@code answeredAt}, {@code id}) descendente: paginación por clave, estable aunque lleguen intentos nuevos.
     */
    List<QuizAttempt> findPage(
            UserId user, Optional<SituationKey> situation, Optional<Stack> stack, Optional<Position> after, int limit);

    /** Posición de un intento en el orden de la paginación. */
    record Position(Instant answeredAt, UUID id) {}
}
