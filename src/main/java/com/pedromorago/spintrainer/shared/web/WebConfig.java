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

    /** The API controllers (not Actuator) are mounted under /api/v1, as {@code servers} in the spec says. */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                ApiPaths.BASE,
                HandlerTypePredicate.forAnnotation(RestController.class)
                        .and(HandlerTypePredicate.forBasePackage("com.pedromorago.spintrainer")));
    }

    /** {@code Accept: application/json} is enough to receive the errors too (see {@link JsonAcceptsProblemDetails}). */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.strategies(List.of(new JsonAcceptsProblemDetails()));
    }

    /**
     * {@code ETag} (hash of the body) and {@code 304} with {@code If-None-Match} on what only changes with a migration:
     * catalog and reference ranges. It runs after Spring Security: it is only computed for authenticated requests.
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
