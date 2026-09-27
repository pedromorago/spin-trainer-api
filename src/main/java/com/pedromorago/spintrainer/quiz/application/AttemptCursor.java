package com.pedromorago.spintrainer.quiz.application;

import com.pedromorago.spintrainer.quiz.application.port.out.AttemptRepository.Position;
import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.shared.kernel.DomainException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Pagination cursor: the position of the last attempt of the page, in URL-safe Base64. It is opaque to the client
 * (which only sends it back), so the format can change without touching the contract.
 */
final class AttemptCursor {

    // Instant.parse accepts years the database cannot store (up to ±1,000,000,000): a hand-made cursor with one of
    // them reached the query and ended in a 500. No attempt is answered outside these years.
    private static final Instant MIN = Instant.parse("0001-01-01T00:00:00Z");
    private static final Instant MAX = Instant.parse("9999-12-31T23:59:59.999999999Z");

    private AttemptCursor() {}

    static String encode(QuizAttempt last) {
        String position = last.answeredAt() + "|" + last.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(position.getBytes(StandardCharsets.UTF_8));
    }

    static Position decode(String cursor) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|");
            if (parts.length != 2) {
                throw new IllegalArgumentException("format");
            }
            Instant answeredAt = Instant.parse(parts[0]);
            if (answeredAt.isBefore(MIN) || answeredAt.isAfter(MAX)) {
                throw new IllegalArgumentException("year out of range");
            }
            return new Position(answeredAt, UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw DomainException.validation("cursor", "invalid cursor");
        }
    }
}
