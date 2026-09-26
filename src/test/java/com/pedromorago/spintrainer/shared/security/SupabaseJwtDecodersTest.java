package com.pedromorago.spintrainer.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.testsupport.TestJwtIssuer;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/** El decoder contra un JWKS real (servido por {@link TestJwtIssuer}): qué acepta y qué rechaza. */
class SupabaseJwtDecodersTest {

    static TestJwtIssuer issuer;
    static JwtDecoder decoder;
    final UUID user = UUID.randomUUID();

    @BeforeAll
    static void startIssuer() {
        issuer = TestJwtIssuer.start();
        decoder = SupabaseJwtDecoders.create(new AuthProperties(URI.create(issuer.issuer()), null, "authenticated"));
    }

    @AfterAll
    static void stopIssuer() {
        issuer.close();
    }

    @Test
    void acceptsASupabaseSessionToken() {
        Jwt jwt = decoder.decode(issuer.tokenFor(user));

        assertThat(jwt.getSubject()).isEqualTo(user.toString());
        assertThat(jwt.getAudience()).containsExactly("authenticated");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("authenticated");
    }

    @Test
    void derivesTheJwksFromTheIssuer() {
        AuthProperties properties =
                new AuthProperties(URI.create("https://abc.supabase.co/auth/v1"), null, "authenticated");

        assertThat(properties.jwkSetUri()).hasToString("https://abc.supabase.co/auth/v1/.well-known/jwks.json");
    }

    @Test
    void rejectsAnExpiredToken() {
        String expired = issuer.token(
                user,
                claims -> claims.issueTime(Date.from(Instant.now().minusSeconds(7200)))
                        .expirationTime(Date.from(Instant.now().minusSeconds(3600))));

        assertThatThrownBy(() -> decoder.decode(expired))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rejectsATokenThatNeverExpires() {
        String eternal = issuer.token(user, claims -> claims.expirationTime(null));

        assertThatThrownBy(() -> decoder.decode(eternal))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("exp");
    }

    @Test
    void rejectsAnotherIssuer() {
        String foreign = issuer.token(user, claims -> claims.issuer("https://other.supabase.co/auth/v1"));

        assertThatThrownBy(() -> decoder.decode(foreign))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("iss");
    }

    @Test
    void rejectsAnotherAudience() {
        String otherAudience = issuer.token(user, claims -> claims.audience("service"));

        assertThatThrownBy(() -> decoder.decode(otherAudience))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("aud");
    }

    @Test
    void rejectsAnonAndServiceRoleTokens() {
        for (String role : new String[] {"anon", "service_role"}) {
            String token = issuer.token(user, claims -> claims.claim("role", role));

            assertThatThrownBy(() -> decoder.decode(token))
                    .as(role)
                    .isInstanceOf(JwtValidationException.class)
                    .hasMessageContaining("role");
        }
    }

    @Test
    void rejectsATokenWhoseSubjectIsNotAUserId() {
        String token = issuer.token(user, claims -> claims.subject("admin"));

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("sub");
    }

    @Test
    void rejectsAKeyThatIsNotInTheJwks() {
        assertThatThrownBy(() -> decoder.decode(issuer.tokenSignedByUnknownKey(user)))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    void rejectsTheLegacySharedSecretAlgorithmAndUnsignedTokens() {
        assertThatThrownBy(() -> decoder.decode(issuer.hs256Token(user))).isInstanceOf(BadJwtException.class);
        assertThatThrownBy(() -> decoder.decode(issuer.unsignedToken(user))).isInstanceOf(BadJwtException.class);
    }
}
