package com.pedromorago.spintrainer.shared.web;

import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class WebConfig implements WebMvcConfigurer {

    /** Los controllers de la API (no Actuator) cuelgan de /api/v1, como dice {@code servers} en la spec. */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                ApiPaths.BASE,
                HandlerTypePredicate.forAnnotation(RestController.class)
                        .and(HandlerTypePredicate.forBasePackage("com.pedromorago.spintrainer")));
    }

    /** {@code Accept: application/json} basta para recibir también los errores (ver {@link JsonAcceptsProblemDetails}). */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.strategies(List.of(new JsonAcceptsProblemDetails()));
    }

    /**
     * {@code ETag} (hash del cuerpo) y {@code 304} con {@code If-None-Match} en lo que solo cambia con una migración:
     * catálogo y rangos de referencia. Va detrás de Spring Security: solo se calcula para peticiones autenticadas.
     */
    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns(
                ApiPaths.BASE + "/situations", ApiPaths.BASE + "/ranges/default", ApiPaths.BASE + "/ranges/default/*");
        return registration;
    }
}
