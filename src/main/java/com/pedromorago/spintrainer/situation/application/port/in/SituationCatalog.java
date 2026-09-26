package com.pedromorago.spintrainer.situation.application.port.in;

import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.List;

/** Catálogo de situaciones. Lo usan la API REST y los módulos range y quiz para validar la combinación. */
public interface SituationCatalog {

    /** Todas, en orden de presentación. */
    List<Situation> all();

    /**
     * La situación de una combinación (situación, stack) existente.
     *
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException NOT_FOUND si la situación o el stack no existen
     */
    Situation spot(SituationKey key, Stack stack);
}
