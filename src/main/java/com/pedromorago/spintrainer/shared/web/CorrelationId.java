package com.pedromorago.spintrainer.shared.web;

import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

/**
 * Identificador de la petición: llega en {@code X-Correlation-Id} o se genera, va a los logs (MDC), a la respuesta y a
 * {@code Problem.correlationId}.
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    // Solo se acepta un valor corto y seguro para logs y cabeceras; cualquier otro se sustituye.
    private static final Pattern ACCEPTED = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private CorrelationId() {}

    static String accept(@Nullable String incoming) {
        return incoming != null && ACCEPTED.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();
    }

    /** El de la petición en curso, o null fuera de una petición. */
    public static @Nullable String current() {
        return MDC.get(MDC_KEY);
    }
}
