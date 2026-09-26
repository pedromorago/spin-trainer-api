package com.pedromorago.spintrainer.range.application.port.in;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.util.List;
import java.util.Map;

/** Custom ranges: only the Explorer writes them (ADR-0012), with optimistic concurrency (ADR-0013). */
public interface ManageUserRanges {

    List<Range> all(UserId user);

    /**
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException NOT_FOUND if the combination does not exist or
     *     there is no range
     */
    Range get(UserId user, SituationKey situation, Stack stack);

    /**
     * Creates ({@code version} 0) or replaces version {@code version}. Hands with the implicit action are discarded.
     *
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException VALIDATION, NOT_FOUND or CONFLICT (the version
     *     is not the current one: another tab or device changed it)
     */
    SavedRange save(UserId user, SituationKey situation, Stack stack, Map<String, Action> hands, int version);

    /** Idempotent: the reference range applies again. */
    void delete(UserId user, SituationKey situation, Stack stack);

    record SavedRange(Range range, boolean created) {}
}
