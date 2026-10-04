package com.pedromorago.spintrainer.range.application.port.in;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.util.List;

/** Reference ranges: examples seeded by the migrations (ADR-0006, ADR-0024). */
public interface ReadDefaultRanges {

    /** All the loaded ones, in catalog order and from largest to smallest stack. */
    List<Range> all();

    /**
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException NOT_FOUND if the combination does not exist or
     *     has no seed
     */
    Range get(SituationKey situation, Stack stack);
}
