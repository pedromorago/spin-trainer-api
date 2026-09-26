package com.pedromorago.spintrainer.testsupport;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Emisor de JWT para tests y desarrollo local que imita a Supabase Auth: clave ES256 propia, JWKS publicado por HTTP en
 * {@code <issuer>/.well-known/jwks.json} y tokens de sesión con las mismas claims ({@code aud}, {@code role}, {@code
 * sub}...). La API lo valida exactamente igual que a Supabase: solo cambia {@code spin-trainer.auth.issuer}.
 */
public final class TestJwtIssuer implements AutoCloseable {

    private static TestJwtIssuer shared;

    private final ECKey key;
    private final HttpServer server;
    private final String issuer;

    private TestJwtIssuer() {
        try {
            key = newKey();
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException | JOSEException e) {
            throw new IllegalStateException("No se pudo arrancar el emisor de JWT de prueba", e);
        }
        byte[] jwks = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        server.createContext("/auth/v1/.well-known/jwks.json", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(jwks);
            }
        });
        server.start();
        issuer = "http://127.0.0.1:" + server.getAddress().getPort() + "/auth/v1";
    }

    /** Emisor nuevo; el llamante lo cierra. */
    public static TestJwtIssuer start() {
        return new TestJwtIssuer();
    }

    /** Emisor compartido por toda la JVM de tests (se para al salir). */
    public static synchronized TestJwtIssuer shared() {
        if (shared == null) {
            shared = start();
        }
        return shared;
    }

    public String issuer() {
        return issuer;
    }

    /** Token de sesión válido durante una hora, como el que obtiene la web al hacer login. */
    public String tokenFor(UUID user) {
        return token(user, claims -> {});
    }

    /** Token válido con las claims modificadas (caducado, otra audiencia, otro rol...). */
    public String token(UUID user, Consumer<JWTClaimsSet.Builder> customizer) {
        return sign(claims(user, customizer), key);
    }

    /** Mismas claims, firmado con una clave que no está en el JWKS. */
    public String tokenSignedByUnknownKey(UUID user) {
        try {
            return sign(claims(user, claims -> {}), newKey());
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Mismas claims firmadas con HS256 (el secreto compartido heredado de Supabase), que la API no acepta. */
    public String hs256Token(UUID user) {
        try {
            byte[] secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims(user, claims -> {}));
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Mismas claims sin firma ({@code alg: none}). */
    public String unsignedToken(UUID user) {
        return new PlainJWT(claims(user, claims -> {})).serialize();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private JWTClaimsSet claims(UUID user, Consumer<JWTClaimsSet.Builder> customizer) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(user.toString())
                .audience("authenticated")
                .claim("role", "authenticated")
                .claim("email", "qa+" + user + "@example.com")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofHours(1))));
        customizer.accept(claims);
        return claims.build();
    }

    private static String sign(JWTClaimsSet claims, ECKey key) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(key.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new ECDSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ECKey newKey() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256)
                .keyID(UUID.randomUUID().toString())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.ES256)
                .generate();
    }
}
