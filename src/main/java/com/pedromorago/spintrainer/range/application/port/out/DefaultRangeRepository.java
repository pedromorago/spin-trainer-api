package com.pedromorago.spintrainer.range.application.port.out;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.util.List;
import java.util.Optional;

public interface DefaultRangeRepository {

    List<Range> findAll();

    Optional<Range> find(SituationKey situation, Stack stack);
}
