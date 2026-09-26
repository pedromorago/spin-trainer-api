package com.pedromorago.spintrainer.situation.application.port.out;

import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.List;

public interface SituationRepository {

    /** All the situations of the seed, in presentation order. */
    List<Situation> findAll();
}
