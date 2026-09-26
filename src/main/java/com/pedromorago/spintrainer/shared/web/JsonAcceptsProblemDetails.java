package com.pedromorago.spintrainer.shared.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Negociación por {@code Accept} en la que quien acepta {@code application/json} acepta también Problem Details
 * ({@code application/problem+json} es JSON; RFC 9457). Sin esto, una operación cuya única representación es un
 * Problem (el DELETE de un rango: 204 sin cuerpo y errores en problem+json) respondía 406 a un cliente con
 * {@code Accept: application/json}, sin llegar a ejecutarse. El tipo de problema va detrás: los datos siguen
 * saliendo como {@code application/json}.
 */
final class JsonAcceptsProblemDetails implements ContentNegotiationStrategy {

    private final ContentNegotiationStrategy header = new HeaderContentNegotiationStrategy();

    @Override
    public List<MediaType> resolveMediaTypes(NativeWebRequest request) throws HttpMediaTypeNotAcceptableException {
        List<MediaType> accepted = header.resolveMediaTypes(request);
        Optional<MediaType> json = accepted.stream()
                .filter(type -> type.equalsTypeAndSubtype(MediaType.APPLICATION_JSON))
                .findFirst();
        boolean problem = accepted.stream().anyMatch(MediaType.APPLICATION_PROBLEM_JSON::equalsTypeAndSubtype);
        if (json.isEmpty() || problem) {
            return accepted;
        }
        List<MediaType> withProblems = new ArrayList<>(accepted);
        // Con la misma preferencia (q) que el JSON que pidió el cliente.
        withProblems.add(MediaType.APPLICATION_PROBLEM_JSON.copyQualityValue(json.get()));
        return withProblems;
    }
}
