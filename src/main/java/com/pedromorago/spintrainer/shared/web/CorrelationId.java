package com.pedromorago.spintrainer.shared.web;

import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

/**
 * Request identifier: it arrives in {@code X-Correlation-Id} or is generated, and goes to the logs (MDC), to the
 * response and to {@code Problem.correlationId}.
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    // Only a short value that is safe for logs and headers is accepted; any other one is replaced.
    private static final Pattern ACCEPTED = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private CorrelationId() {}

    static String accept(@Nullable String incoming) {
        return incoming != null && ACCEPTED.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();
    }

    /** The one of the current request, or null outside a request. */
    public static @Nullable String current() {
        return MDC.get(MDC_KEY);
    }
}
