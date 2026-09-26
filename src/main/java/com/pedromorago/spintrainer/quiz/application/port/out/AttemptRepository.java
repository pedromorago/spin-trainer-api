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

    /** Insert only: attempts are never modified or deleted (the API role has no permission to do so). */
    void insert(QuizAttempt attempt);

    /**
     * Up to {@code limit} attempts of the user before {@code after} (if any), ordered by ({@code answeredAt},
     * {@code id}) descending: keyset pagination, stable even if new attempts arrive.
     */
    List<QuizAttempt> findPage(
            UserId user, Optional<SituationKey> situation, Optional<Stack> stack, Optional<Position> after, int limit);

    /** Position of an attempt in the pagination order. */
    record Position(Instant answeredAt, UUID id) {}
}
