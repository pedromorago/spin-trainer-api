package com.pedromorago.spintrainer.shared.kernel;

import java.util.Objects;
import java.util.UUID;

/** Authenticated user: the {@code sub} of the Supabase JWT. */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
