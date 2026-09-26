package com.pedromorago.spintrainer.situation.application.port.out;

import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.List;

public interface SituationRepository {

    /** Todas las situaciones del seed, en orden de presentación. */
    List<Situation> findAll();
}
