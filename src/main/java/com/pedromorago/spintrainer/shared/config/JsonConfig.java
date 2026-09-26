package com.pedromorago.spintrainer.shared.config;

import org.springframework.boot.jackson.autoconfigure.JsonFactoryBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.EnumFeature;

/**
 * JSON estricto: lo que la spec no admite es un 400, no se "arregla" en silencio. Jackson por defecto trunca
 * {@code "version": 0.9} a 0, acepta {@code "stack": "25"} o un índice numérico como enum, e ignora campos
 * desconocidos ({@code additionalProperties: false}).
 */
@Configuration(proxyBeanMethods = false)
class JsonConfig {

    /** Un rango completo (169 manos) ocupa unos 3 KB: 64 KB sobran y acotan la memoria de cada petición. */
    static final long MAX_DOCUMENT_BYTES = 64 * 1024;

    @Bean
    JsonMapperBuilderCustomizer strictDeserialization() {
        return builder -> builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
    }

    @Bean
    JsonFactoryBuilderCustomizer boundedDocuments() {
        return factory -> factory.streamReadConstraints(StreamReadConstraints.builder()
                .maxDocumentLength(MAX_DOCUMENT_BYTES)
                .build());
    }
}
