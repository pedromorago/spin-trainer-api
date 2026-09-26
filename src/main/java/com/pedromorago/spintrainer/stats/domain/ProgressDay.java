package com.pedromorago.spintrainer.stats.domain;

import java.time.LocalDate;

/** Intentos y aciertos de un día (en la zona horaria pedida). Solo existen días con actividad. */
public record ProgressDay(LocalDate date, int attempts, int correct) {

    public ProgressDay {
        if (attempts < 1 || correct < 0 || correct > attempts) {
            throw new IllegalArgumentException("recuentos incoherentes: " + correct + "/" + attempts);
        }
    }
}
