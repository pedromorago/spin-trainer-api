package com.pedromorago.spintrainer.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Validates real responses against {@code openapi.yaml}: the status has to be declared for that operation and the body
 * has to comply with its schema (JSON Schema 2020-12, like OpenAPI 3.1). The API checks in its own CI that it complies
 * with the contract it publishes; spin-trainer-qa does it again against the deployed API (ADR-0008).
 */
public final class OpenApiContract {

    private static final String SPEC_IRI = "https://spin-trainer.local/openapi.yaml";

    private final JsonNode spec;
    private final SchemaRegistry registry;

    private OpenApiContract(String specText) {
        spec = YAMLMapper.builder().build().readTree(specText);
        // In 2020-12 "format" is only an annotation: enabled as an assertion (uuid, date, date-time, uri-reference).
        registry = SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_2020_12,
                builder -> builder.schemas(Map.of(SPEC_IRI, specText))
                        .schemaRegistryConfig(SchemaRegistryConfig.builder()
                                .formatAssertionsEnabled(true)
                                .locale(Locale.ENGLISH)
                                .build()));
    }

    public static OpenApiContract load() {
        try {
            return new OpenApiContract(Files.readString(Path.of("openapi.yaml"), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The response is declared in the spec and its body complies with the schema.
     *
     * @param path spec template, e.g. {@code /ranges/user/{situation}/{stack}}
     */
    public void assertResponse(String method, String path, MvcTestResult result) {
        int status = result.getResponse().getStatus();
        String body = contentAsString(result);
        assertThat(violations(method, path, status, result.getResponse().getContentType(), body))
                .as("%s %s → %d no cumple openapi.yaml:%n%s", method, path, status, body)
                .isEmpty();
    }

    /** Contract violations of a response (status, content type and body); empty if it complies. */
    public List<String> violations(String method, String path, int status, @Nullable String contentType, String body) {
        String responsePointer = "/paths/" + escape(path) + "/" + method.toLowerCase() + "/responses/" + status;
        JsonNode response = spec.at(responsePointer);
        if (response.isMissingNode()) {
            return List.of("openapi.yaml no declara " + status + " para " + method + " " + path);
        }
        if (response.has("$ref")) {
            responsePointer = response.get("$ref").asString().substring(1);
            response = spec.at(responsePointer);
        }
        if (!response.has("content")) {
            return body.isEmpty() ? List.of() : List.of("la respuesta " + status + " no debe tener cuerpo");
        }
        String mediaType = contentType == null ? "" : contentType.split(";")[0].trim();
        if (!response.get("content").has(mediaType)) {
            return List.of(
                    "Content-Type '" + mediaType + "' no declarado para " + status + " en " + method + " " + path);
        }
        Schema schema = registry.getSchema(
                SchemaLocation.of(SPEC_IRI + "#" + responsePointer + "/content/" + escape(mediaType) + "/schema"));
        return schema.validate(body, InputFormat.JSON).stream()
                .map(Error::toString)
                .toList();
    }

    private static String escape(String pointerToken) {
        return pointerToken.replace("~", "~0").replace("/", "~1");
    }

    private static String contentAsString(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
