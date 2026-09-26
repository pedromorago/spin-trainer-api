package com.pedromorago.spintrainer.shared.security;

import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** User of the current request ({@code sub} of the JWT, already validated as a UUID). Used by the REST adapters. */
@Component
public class CurrentUser {

    public UserId id() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            return new UserId(UUID.fromString(token.getToken().getSubject()));
        }
        throw new IllegalStateException("No authenticated user in the request");
    }
}
