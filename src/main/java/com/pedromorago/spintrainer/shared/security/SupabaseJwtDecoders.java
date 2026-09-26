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
 * Validates the Supabase Auth session JWTs: ES256 signature against the JWKS (asymmetric keys; the legacy HS256 secret
 * is not accepted), expiration ({@code exp} required), issuer, audience {@code authenticated},
 * {@code role = authenticated} (rejects {@code anon} and {@code service_role} tokens) and a UUID-shaped {@code sub}
 * (identifies the user).
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
                // The default validator accepts tokens without exp: a session token always expires.
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
