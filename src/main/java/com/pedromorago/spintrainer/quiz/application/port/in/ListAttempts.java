package com.pedromorago.spintrainer.quiz.application.port.in;

import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.util.List;
import java.util.Optional;

public interface ListAttempts {

    /**
     * The user's attempts, from most recent to oldest, in pages of {@code limit}.
     *
     * @param cursor the {@code nextCursor} of the previous page (opaque); empty for the first one
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException VALIDATION if the cursor or limit is invalid
     */
    Page list(UserId user, Optional<SituationKey> situation, Optional<Stack> stack, int limit, Optional<String> cursor);

    record Page(List<QuizAttempt> items, Optional<String> nextCursor) {}
}
