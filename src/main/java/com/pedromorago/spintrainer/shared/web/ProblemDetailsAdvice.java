package com.pedromorago.spintrainer.shared.web;

import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.DomainException.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Traduce todos los errores a Problem Details (RFC 9457) con los tipos de {@link ProblemType}, {@code correlationId}
 * y, en los 400, {@code errors} por campo. También recibe los 401 de Spring Security (ver SecurityConfig), así que
 * hay un único sitio que da forma a los errores.
 */
@RestControllerAdvice
class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsAdvice.class);
    private static final URI ABOUT_BLANK = URI.create("about:blank");

    @ExceptionHandler(DomainException.class)
    ResponseEntity<Object> domain(DomainException ex, WebRequest request) {
        ProblemType type = switch (ex.kind()) {
            case VALIDATION -> ProblemType.VALIDATION;
            case NOT_FOUND -> ProblemType.NOT_FOUND;
            case CONFLICT -> ProblemType.CONFLICT;
            case NO_RANGE -> ProblemType.NO_RANGE;
        };
        return problem(type, ex.getMessage(), ex.errors(), new HttpHeaders(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> unauthenticated(AuthenticationException ex, WebRequest request) {
        // RFC 6750: sin token → "Bearer"; token presente pero inválido → error="invalid_token".
        // El motivo concreto (caducado, audiencia...) se registra, pero no se devuelve.
        boolean invalidToken = ex instanceof OAuth2AuthenticationException;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, invalidToken ? "Bearer error=\"invalid_token\"" : "Bearer");
        if (invalidToken) {
            log.info("JWT rechazado: {}", ex.getMessage());
        }
        String detail = invalidToken
                ? "El token de acceso no es válido o ha caducado"
                : "Falta el token de acceso (Authorization: Bearer)";
        return problem(ProblemType.UNAUTHORIZED, detail, List.of(), headers, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception ex, WebRequest request) {
        log.error("Error no controlado", ex);
        return problem(
                ProblemType.INTERNAL,
                "Error interno. Si lo reportas, indica el correlationId.",
                List.of(),
                new HttpHeaders(),
                request);
    }

    // Cuerpo que no cumple las anotaciones de Bean Validation generadas desde la spec.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = new ArrayList<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(e -> errors.add(new FieldError(fieldPath(e.getField()), e.getDefaultMessage())));
        ex.getBindingResult()
                .getGlobalErrors()
                .forEach(e -> errors.add(new FieldError(e.getObjectName(), e.getDefaultMessage())));
        return problem(ProblemType.VALIDATION, invalidFields(errors), errors, headers, request);
    }

    // Parámetros de ruta o query que no cumplen la spec (patrón de situación, rango del stack, limit...).
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new FieldError(r.getMethodParameter().getParameterName(), message(e))))
                .toList();
        return problem(ProblemType.VALIDATION, invalidFields(errors), errors, headers, request);
    }

    // JSON mal formado, campos no permitidos (additionalProperties: false) o valores fuera de un enum.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        JacksonException jackson = findCause(ex, JacksonException.class);
        FieldError error;
        if (jackson == null || jackson.getPath().isEmpty()) {
            error = new FieldError("body", "JSON mal formado o de tipo incorrecto");
        } else if (jackson instanceof UnrecognizedPropertyException) {
            error = new FieldError(jsonPath(jackson), "campo no permitido");
        } else {
            error = new FieldError(jsonPath(jackson), "valor no válido");
        }
        return problem(
                ProblemType.VALIDATION, error.message() + ": " + error.field(), List.of(error), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        List<FieldError> errors = List.of(new FieldError(field, "valor no válido"));
        return problem(ProblemType.VALIDATION, invalidFields(errors), errors, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return problem(ProblemType.NOT_FOUND, "Ruta desconocida", List.of(), headers, request);
    }

    // Resto de errores de Spring MVC (405, 406, 415...): su ProblemDetail, con un type propio (la spec lo exige y
    // "about:blank" se omite al serializar), correlationId e instance.
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            if (problem.getType() == null || ABOUT_BLANK.equals(problem.getType())) {
                problem.setType(status.is5xxServerError() ? ProblemType.INTERNAL.uri() : ProblemType.UNSUPPORTED.uri());
            }
            decorate(problem, request);
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private ResponseEntity<Object> problem(
            ProblemType type, String detail, List<FieldError> errors, HttpHeaders headers, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.status(), detail);
        problem.setType(type.uri());
        problem.setTitle(type.title());
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        decorate(problem, request);
        return ResponseEntity.status(type.status())
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static void decorate(ProblemDetail problem, WebRequest request) {
        if (problem.getInstance() == null && request instanceof NativeWebRequest web) {
            HttpServletRequest servlet = web.getNativeRequest(HttpServletRequest.class);
            if (servlet != null) {
                problem.setInstance(URI.create(servlet.getRequestURI()));
            }
        }
        problem.setProperty("correlationId", CorrelationId.current());
    }

    private static String invalidFields(List<FieldError> errors) {
        return errors.size() == 1
                ? errors.getFirst().field() + ": " + errors.getFirst().message()
                : errors.size() + " campos no válidos: "
                        + errors.stream().map(FieldError::field).distinct().collect(Collectors.joining(", "));
    }

    private static String message(MessageSourceResolvable error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "valor no válido";
    }

    /** {@code hands[AAs]} → {@code hands.AAs}, como en el resto de la API. */
    private static String fieldPath(String springPath) {
        return springPath.replaceAll("\\[([^]]*)]", ".$1");
    }

    private static String jsonPath(JacksonException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : String.valueOf(ref.getIndex()))
                .collect(Collectors.joining("."));
    }

    private static <T extends Throwable> @Nullable T findCause(Throwable ex, Class<T> type) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
        }
        return null;
    }
}
