package com.pedromorago.spintrainer.range.application.port.in;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.util.List;

/** Rangos de referencia (seed del PDF, ADR-0006). */
public interface ReadDefaultRanges {

    /** Todos los cargados, en orden de catálogo y de mayor a menor stack. */
    List<Range> all();

    /** @throws com.pedromorago.spintrainer.shared.kernel.DomainException NOT_FOUND si la combinación no existe o no tiene seed */
    Range get(SituationKey situation, Stack stack);
}
