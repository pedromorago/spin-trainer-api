package com.pedromorago.spintrainer.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Primer filtro de la cadena (antes que Spring Security): también los 401 llevan correlation id. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String ATTRIBUTE = CorrelationIdFilter.class.getName();

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // En un redespacho a /error se reutiliza el de la petición original.
        String id = request.getAttribute(ATTRIBUTE) instanceof String original
                ? original
                : CorrelationId.accept(request.getHeader(CorrelationId.HEADER));
        request.setAttribute(ATTRIBUTE, id);
        response.setHeader(CorrelationId.HEADER, id);
        MDC.put(CorrelationId.MDC_KEY, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
