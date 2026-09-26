package com.pedromorago.spintrainer.shared.web;

import java.net.URI;
import org.springframework.http.HttpStatus;

/** API Problem Details types ({@code urn:spin-trainer:<slug>}): those of openapi.yaml plus the internal error. */
public enum ProblemType {
    VALIDATION("validation", "Validation failed", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("unauthorized", "Unauthorized", HttpStatus.UNAUTHORIZED),
    NOT_FOUND("not-found", "Not found", HttpStatus.NOT_FOUND),
    CONFLICT("conflict", "Conflict", HttpStatus.CONFLICT),
    NO_RANGE("no-range", "No range", HttpStatus.UNPROCESSABLE_CONTENT),
    /** Unsupported method, content type or response format (405, 406, 415); keeps its status and title. */
    UNSUPPORTED("unsupported", "Unsupported request", HttpStatus.BAD_REQUEST),
    /** A dependency does not respond (e.g. the Supabase JWKS): it is not the client's fault and it may retry. */
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
