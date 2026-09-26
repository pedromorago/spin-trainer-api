package com.pedromorago.spintrainer.shared.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Emisor de los JWT (Supabase Auth). En Supabase: {@code issuer = <SUPABASE_URL>/auth/v1}; el JWKS se deriva del emisor
 * si no se indica.
 */
@Validated
@ConfigurationProperties("spin-trainer.auth")
public record AuthProperties(
        @NotNull URI issuer,
        @Nullable URI jwkSetUri,
        @DefaultValue("authenticated") @NotBlank String audience) {

    public AuthProperties {
        if (jwkSetUri == null && issuer != null) {
            jwkSetUri = URI.create(issuer + "/.well-known/jwks.json");
        }
    }
}
