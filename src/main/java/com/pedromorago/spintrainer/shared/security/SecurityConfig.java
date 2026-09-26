package com.pedromorago.spintrainer.shared.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
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
        // Los errores de autenticación pasan por el mismo @RestControllerAdvice que el resto: un único formato Problem.
        // También los fallos del propio emisor (JWKS inalcanzable), que Spring relanzaría y acabarían en /error como un
        // 401 engañoso ("falta el token") cuando el problema es de Supabase o de SUPABASE_URL.
        AuthenticationEntryPoint problemEntryPoint =
                (request, response, ex) -> resolver.resolveException(request, response, null, ex);
        AuthenticationFailureHandler problemFailureHandler =
                (request, response, ex) -> resolver.resolveException(request, response, null, ex);
        http.authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(problemEntryPoint)
                        .withObjectPostProcessor(new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                            @Override
                            public <F extends BearerTokenAuthenticationFilter> F postProcess(F filter) {
                                filter.setAuthenticationFailureHandler(problemFailureHandler);
                                return filter;
                            }
                        }))
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
