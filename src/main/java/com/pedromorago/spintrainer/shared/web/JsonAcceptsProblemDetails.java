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
 * {@code Accept} negotiation in which whoever accepts {@code application/json} also accepts Problem Details
 * ({@code application/problem+json} is JSON; RFC 9457). Without this, an operation whose only representation is a
 * Problem (the DELETE of a range: 204 with no body and errors in problem+json) responded 406 to a client with
 * {@code Accept: application/json}, without even being executed. The problem type goes last: data still comes out as
 * {@code application/json}.
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
        // With the same preference (q) as the JSON the client asked for.
        withProblems.add(MediaType.APPLICATION_PROBLEM_JSON.copyQualityValue(json.get()));
        return withProblems;
    }
}
