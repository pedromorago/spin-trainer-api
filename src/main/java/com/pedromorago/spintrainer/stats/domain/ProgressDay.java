package com.pedromorago.spintrainer.stats.domain;

import java.time.LocalDate;

/** Attempts and correct answers of a day (in the requested time zone). Only days with activity exist. */
public record ProgressDay(LocalDate date, int attempts, int correct) {

    public ProgressDay {
        if (attempts < 1 || correct < 0 || correct > attempts) {
            throw new IllegalArgumentException("recuentos incoherentes: " + correct + "/" + attempts);
        }
    }
}
