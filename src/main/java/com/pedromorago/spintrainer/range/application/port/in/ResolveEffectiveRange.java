package com.pedromorago.spintrainer.range.application.port.in;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.util.Optional;

/** Rango contra el que se entrena (ADR-0012): el personalizado si existe; si no, el de referencia. */
public interface ResolveEffectiveRange {

    Optional<Range> effectiveRange(UserId user, SituationKey situation, Stack stack);
}
