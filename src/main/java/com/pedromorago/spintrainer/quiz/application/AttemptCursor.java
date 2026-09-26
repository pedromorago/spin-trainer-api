package com.pedromorago.spintrainer.quiz.application;

import com.pedromorago.spintrainer.quiz.application.port.out.AttemptRepository.Position;
import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.shared.kernel.DomainException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Cursor de la paginación: la posición del último intento de la página, en Base64 URL-safe. Es opaco para el cliente
 * (solo lo reenvía), así que el formato puede cambiar sin tocar el contrato.
 */
final class AttemptCursor {

    private AttemptCursor() {}

    static String encode(QuizAttempt last) {
        String position = last.answeredAt() + "|" + last.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(position.getBytes(StandardCharsets.UTF_8));
    }

    static Position decode(String cursor) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|");
            if (parts.length != 2) {
                throw new IllegalArgumentException("formato");
            }
            return new Position(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw DomainException.validation("cursor", "cursor no válido");
        }
    }
}
