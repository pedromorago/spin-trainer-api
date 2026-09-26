package com.pedromorago.spintrainer.shared.kernel;

import java.util.Objects;
import java.util.UUID;

/** Usuario autenticado: el {@code sub} del JWT de Supabase. */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
