package com.pedromorago.spintrainer.shared.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Resource server sin estado: cada petición trae su JWT de Supabase (ADR-0003). Sin sesiones ni cookies, así que no
 * hay CSRF. Solo la salud de Actuator es pública.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
            throws Exception {
        // Los 401 pasan por el mismo @RestControllerAdvice que el resto de errores: un único formato Problem.
        AuthenticationEntryPoint problemEntryPoint =
                (request, response, ex) -> resolver.resolveException(request, response, null, ex);
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(
                        oauth -> oauth.jwt(Customizer.withDefaults()).authenticationEntryPoint(problemEntryPoint))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problemEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties properties) {
        return SupabaseJwtDecoders.create(properties);
    }
}
