package com.pedromorago.spintrainer.range.application.port.in;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.util.Optional;

/** Range to train against (ADR-0012): the custom one if it exists; otherwise, the reference one. */
public interface ResolveEffectiveRange {

    Optional<Range> effectiveRange(UserId user, SituationKey situation, Stack stack);
}
