package com.pedromorago.spintrainer.shared.kernel;

import java.util.List;

/**
 * Business error. The modules throw it with a type and the web layer translates it into Problem Details (RFC 9457); the
 * domain knows nothing about HTTP.
 */
public final class DomainException extends RuntimeException {

    public enum Kind {
        VALIDATION,
        NOT_FOUND,
        CONFLICT,
        NO_RANGE
    }

    /** Per-field detail of a validation error ({@code hands.AAs}, {@code version}...). */
    public record FieldError(String field, String message) {}

    private final Kind kind;
    private final List<FieldError> errors;

    private DomainException(Kind kind, String detail, List<FieldError> errors) {
        super(detail);
        this.kind = kind;
        this.errors = List.copyOf(errors);
    }

    public static DomainException validation(String detail, List<FieldError> errors) {
        return new DomainException(Kind.VALIDATION, detail, errors);
    }

    public static DomainException validation(String field, String message) {
        return validation(message, List.of(new FieldError(field, message)));
    }

    public static DomainException notFound(String detail) {
        return new DomainException(Kind.NOT_FOUND, detail, List.of());
    }

    public static DomainException conflict(String detail) {
        return new DomainException(Kind.CONFLICT, detail, List.of());
    }

    public static DomainException noRange(String detail) {
        return new DomainException(Kind.NO_RANGE, detail, List.of());
    }

    public Kind kind() {
        return kind;
    }

    public List<FieldError> errors() {
        return errors;
    }
}
