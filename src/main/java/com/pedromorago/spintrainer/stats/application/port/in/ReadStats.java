package com.pedromorago.spintrainer.stats.application.port.in;

import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import com.pedromorago.spintrainer.stats.domain.HandStat;
import com.pedromorago.spintrainer.stats.domain.ProgressDay;
import java.util.List;
import java.util.Optional;

/** Aggregated queries over the user's attempts (the read side of quiz). */
public interface ReadStats {

    /** One row per (situation, stack, hand) with attempts, in catalog order, descending stack and hand. */
    List<HandStat> byHand(UserId user, Optional<SituationKey> situation, Optional<Stack> stack);

    /**
     * Days with activity in the last {@code days} (today included) in the {@code timeZone} zone, from oldest to most
     * recent.
     *
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException VALIDATION if days or the zone are not valid
     */
    List<ProgressDay> byDay(UserId user, int days, String timeZone);
}
