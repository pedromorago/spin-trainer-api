package com.pedromorago.spintrainer.stats.domain;

import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.time.Instant;

/** Attempts and correct answers of a hand in a situation and stack. The client applies the study policy (ADR-0013). */
public record HandStat(
        SituationKey situation, Stack stack, Hand hand, int attempts, int correct, Instant lastAnsweredAt) {

    public HandStat {
        if (attempts < 1 || correct < 0 || correct > attempts) {
            throw new IllegalArgumentException("recuentos incoherentes: " + correct + "/" + attempts);
        }
    }
}
