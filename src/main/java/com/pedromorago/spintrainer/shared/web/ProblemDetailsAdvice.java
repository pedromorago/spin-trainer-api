package com.pedromorago.spintrainer.shared.web;

import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.DomainException.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationServiceException;
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
 * Translates all errors into Problem Details (RFC 9457) with the types of {@link ProblemType}, {@code correlationId}
 * and, in 400s, per-field {@code errors}. It also receives the 401s of Spring Security (see SecurityConfig), so there
 * is a single place that shapes the errors.
 */
@RestControllerAdvice
@ImportRuntimeHints(ProblemDetailsHints.class)
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

    // Path or query parameters that do not comply with the spec: the generated interfaces carry @Validated, so Spring
    // validates them with an AOP proxy (ConstraintViolationException) and not with MVC's own validation.
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> constraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<FieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldError(parameterPath(v.getPropertyPath()), v.getMessage()))
                .sorted(Comparator.comparing(FieldError::field))
                .toList();
        return problem(ProblemType.VALIDATION, invalidFields(errors), errors, new HttpHeaders(), request);
    }

    // The JWT issuer does not respond (Supabase down or SUPABASE_URL misconfigured): 503, not "missing token".
    @ExceptionHandler(AuthenticationServiceException.class)
    ResponseEntity<Object> authenticationUnavailable(AuthenticationServiceException ex, WebRequest request) {
        log.error("Could not validate the JWT: the issuer is not responding", ex);
        return problem(
                ProblemType.UNAVAILABLE,
                "No se puede validar el token ahora mismo; inténtalo de nuevo",
                List.of(),
                new HttpHeaders(),
                request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> unauthenticated(AuthenticationException ex, WebRequest request) {
        // RFC 6750: no token → "Bearer"; token present but invalid → error="invalid_token".
        // The specific reason (expired, audience...) is logged, but not returned.
        boolean invalidToken = ex instanceof OAuth2AuthenticationException;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, invalidToken ? "Bearer error=\"invalid_token\"" : "Bearer");
        if (invalidToken) {
            log.info("JWT rejected: {}", ex.getMessage());
        }
        String detail = invalidToken
                ? "El token de acceso no es válido o ha caducado"
                : "Falta el token de acceso (Authorization: Bearer)";
        return problem(ProblemType.UNAUTHORIZED, detail, List.of(), headers, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception ex, WebRequest request) {
        log.error("Unhandled error", ex);
        return problem(
                ProblemType.INTERNAL,
                "Error interno. Si lo reportas, indica el correlationId.",
                List.of(),
                new HttpHeaders(),
                request);
    }

    // Body that does not comply with the Bean Validation annotations generated from the spec.
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

    // Path or query parameters that do not comply with the spec (situation pattern, stack range, limit...).
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new FieldError(r.getMethodParameter().getParameterName(), message(e))))
                .toList();
        return problem(ProblemType.VALIDATION, invalidFields(errors), errors, headers, request);
    }

    // Malformed JSON, disallowed fields (additionalProperties: false) or values outside an enum.
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

    // Other Spring MVC errors (405, 406, 415...): their ProblemDetail, with a type of its own (the spec requires it and
    // "about:blank" is omitted when serializing), correlationId and instance.
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

    /** {@code getDefaultRange.situation} → {@code situation}: without the method name. */
    private static String parameterPath(Path path) {
        List<String> names = new ArrayList<>();
        for (Path.Node node : path) {
            if (node.getKind() != ElementKind.METHOD && node.getName() != null) {
                names.add(node.getName());
            }
        }
        return String.join(".", names);
    }

    /** {@code hands[AAs]} → {@code hands.AAs}, as in the rest of the API. */
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
