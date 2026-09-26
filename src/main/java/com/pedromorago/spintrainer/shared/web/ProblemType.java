package com.pedromorago.spintrainer.shared.web;

import java.net.URI;
import org.springframework.http.HttpStatus;

/** Tipos de Problem Details de la API ({@code urn:spin-trainer:<slug>}), los de openapi.yaml más el error interno. */
public enum ProblemType {
    VALIDATION("validation", "Validation failed", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("unauthorized", "Unauthorized", HttpStatus.UNAUTHORIZED),
    NOT_FOUND("not-found", "Not found", HttpStatus.NOT_FOUND),
    CONFLICT("conflict", "Conflict", HttpStatus.CONFLICT),
    NO_RANGE("no-range", "No range", HttpStatus.UNPROCESSABLE_CONTENT),
    /** Método, tipo de contenido o formato de respuesta no soportados (405, 406, 415); conserva su estado y título. */
    UNSUPPORTED("unsupported", "Unsupported request", HttpStatus.BAD_REQUEST),
    /** Una dependencia no responde (p. ej. el JWKS de Supabase): no es culpa del cliente y puede reintentar. */
    UNAVAILABLE("unavailable", "Service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL("internal", "Internal error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final URI uri;
    private final String title;
    private final HttpStatus status;

    ProblemType(String slug, String title, HttpStatus status) {
        this.uri = URI.create("urn:spin-trainer:" + slug);
        this.title = title;
        this.status = status;
    }

    public URI uri() {
        return uri;
    }

    public String title() {
        return title;
    }

    public HttpStatus status() {
        return status;
    }
}
