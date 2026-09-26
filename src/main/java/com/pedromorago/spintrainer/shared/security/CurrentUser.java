package com.pedromorago.spintrainer.shared.security;

import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Usuario de la petición en curso ({@code sub} del JWT, ya validado como UUID). Lo usan los adaptadores REST. */
@Component
public class CurrentUser {

    public UserId id() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            return new UserId(UUID.fromString(token.getToken().getSubject()));
        }
        throw new IllegalStateException("No hay un usuario autenticado en la petición");
    }
}
