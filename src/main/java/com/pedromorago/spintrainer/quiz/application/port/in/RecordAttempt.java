package com.pedromorago.spintrainer.quiz.application.port.in;

import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;

public interface RecordAttempt {

    /**
     * Grades and stores a Quiz answer against the user's effective range at this moment.
     *
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException NOT_FOUND (unknown combination), VALIDATION
     *     (invalid hand or action) or NO_RANGE (the combination has no range to grade against)
     */
    QuizAttempt record(UserId user, SituationKey situation, Stack stack, String hand, Action given);
}
