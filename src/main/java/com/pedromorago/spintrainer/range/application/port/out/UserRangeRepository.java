package com.pedromorago.spintrainer.range.application.port.out;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.util.List;
import java.util.Optional;

public interface UserRangeRepository {

    List<Range> findAll(UserId user);

    Optional<Range> find(UserId user, SituationKey situation, Stack stack);

    /** Inserta la versión 1. {@code false} si ya existía (otra escritura se adelantó). */
    boolean insert(UserId user, Range range);

    /**
     * Sustituye el rango si su versión actual es {@code expectedVersion} (en una sola sentencia, sin carreras).
     * {@code false} si la versión cambió o el rango ya no existe.
     */
    boolean replace(UserId user, Range range, int expectedVersion);

    void delete(UserId user, SituationKey situation, Stack stack);
}
