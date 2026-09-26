package com.pedromorago.spintrainer.stats.application.port.out;

import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import com.pedromorago.spintrainer.stats.domain.DayWindow;
import com.pedromorago.spintrainer.stats.domain.HandStat;
import com.pedromorago.spintrainer.stats.domain.ProgressDay;
import java.util.List;
import java.util.Optional;

/** Aggregates computed by the database (GROUP BY) over the attempts. */
public interface AttemptStatistics {

    List<HandStat> byHand(UserId user, Optional<SituationKey> situation, Optional<Stack> stack);

    List<ProgressDay> byDay(UserId user, DayWindow window);
}
