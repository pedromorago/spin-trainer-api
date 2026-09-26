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

    /** Inserts version 1. {@code false} if it already existed (another write got there first). */
    boolean insert(UserId user, Range range);

    /**
     * Replaces the range if its current version is {@code expectedVersion} (in a single statement, without races).
     * {@code false} if the version changed or the range no longer exists.
     */
    boolean replace(UserId user, Range range, int expectedVersion);

    void delete(UserId user, SituationKey situation, Stack stack);
}
