package com.pedromorago.spintrainer.shared.config;

import org.springframework.boot.jackson.autoconfigure.JsonFactoryBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.type.LogicalType;

/**
 * Strict JSON: what the spec does not allow is a 400, it is not silently "fixed". By default Jackson truncates
 * {@code "version": 0.9} to 0, accepts {@code "stack": "25"} or a numeric index as an enum, turns
 * {@code "situation": 5} or {@code true} into text, and ignores unknown fields ({@code additionalProperties: false}).
 */
@Configuration(proxyBeanMethods = false)
class JsonConfig {

    /** A full range (169 hands) takes about 3 KB: 64 KB is plenty and bounds the memory of each request. */
    static final long MAX_DOCUMENT_BYTES = 64 * 1024;

    @Bean
    JsonMapperBuilderCustomizer strictDeserialization() {
        return builder -> builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                // ALLOW_COERCION_OF_SCALARS does not cover a number or a boolean where a string goes.
                .withCoercionConfig(
                        LogicalType.Textual,
                        text -> text.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }

    @Bean
    JsonFactoryBuilderCustomizer boundedDocuments() {
        return factory -> factory.streamReadConstraints(StreamReadConstraints.builder()
                .maxDocumentLength(MAX_DOCUMENT_BYTES)
                .build());
    }
}
