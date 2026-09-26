package com.pedromorago.spintrainer.quiz.application.port.in;

import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.util.List;
import java.util.Optional;

public interface ListAttempts {

    /**
     * Intentos del usuario, del más reciente al más antiguo, en páginas de {@code limit}.
     *
     * @param cursor el {@code nextCursor} de la página anterior (opaco); vacío para la primera
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException VALIDATION si el cursor o el límite no son válidos
     */
    Page list(UserId user, Optional<SituationKey> situation, Optional<Stack> stack, int limit, Optional<String> cursor);

    record Page(List<QuizAttempt> items, Optional<String> nextCursor) {}
}
