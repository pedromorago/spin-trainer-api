package com.pedromorago.spintrainer.stats.application.port.in;

import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import com.pedromorago.spintrainer.stats.domain.HandStat;
import com.pedromorago.spintrainer.stats.domain.ProgressDay;
import java.util.List;
import java.util.Optional;

/** Consultas agregadas sobre los intentos del usuario (el lado de lectura de quiz). */
public interface ReadStats {

    /** Una fila por (situación, stack, mano) con intentos, en orden de catálogo, stack descendente y mano. */
    List<HandStat> byHand(UserId user, Optional<SituationKey> situation, Optional<Stack> stack);

    /**
     * Días con actividad de los últimos {@code days} (hoy incluido) en la zona {@code timeZone}, del más antiguo al más
     * reciente.
     *
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException VALIDATION si days o la zona no son válidos
     */
    List<ProgressDay> byDay(UserId user, int days, String timeZone);
}
