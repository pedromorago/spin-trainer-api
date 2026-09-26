package com.pedromorago.spintrainer.shared.security;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Valida los JWT de sesión de Supabase Auth: firma ES256 contra el JWKS (claves asimétricas; el secreto HS256 heredado no
 * se acepta), caducidad ({@code exp} obligatoria), emisor, audiencia {@code authenticated}, {@code role = authenticated} (rechaza los tokens
 * {@code anon} y {@code service_role}) y {@code sub} con forma de UUID (identifica al usuario).
 */
public final class SupabaseJwtDecoders {

    static final String ROLE_CLAIM = "role";
    static final String AUTHENTICATED = "authenticated";

    private SupabaseJwtDecoders() {}

    public static JwtDecoder create(AuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                        properties.jwkSetUri().toString())
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();
        decoder.setJwtValidator(validator(properties));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validator(AuthProperties properties) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer().toString()),
                // El validador por defecto acepta tokens sin exp: un token de sesión siempre caduca.
                new JwtClaimValidator<@Nullable Instant>(JwtClaimNames.EXP, Objects::nonNull),
                new JwtClaimValidator<@Nullable Collection<String>>(
                        JwtClaimNames.AUD, aud -> aud != null && aud.contains(properties.audience())),
                new JwtClaimValidator<@Nullable String>(ROLE_CLAIM, AUTHENTICATED::equals),
                new JwtClaimValidator<@Nullable String>(JwtClaimNames.SUB, SupabaseJwtDecoders::isUuid));
    }

    private static boolean isUuid(@Nullable String value) {
        if (value == null) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
