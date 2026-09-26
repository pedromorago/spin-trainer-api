package com.pedromorago.spintrainer.situation.application.port.in;

import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.List;

/** Situation catalog. Used by the REST API and by the range and quiz modules to validate the combination. */
public interface SituationCatalog {

    /** All of them, in presentation order. */
    List<Situation> all();

    /**
     * The situation of an existing (situation, stack) combination.
     *
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException NOT_FOUND if the situation or the stack does
     *     not exist
     */
    Situation spot(SituationKey key, Stack stack);
}
