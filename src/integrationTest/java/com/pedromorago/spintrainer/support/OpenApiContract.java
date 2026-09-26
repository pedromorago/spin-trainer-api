package com.pedromorago.spintrainer.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Valida respuestas reales contra {@code openapi.yaml}: el estado tiene que estar declarado para esa operación y el
 * cuerpo tiene que cumplir su schema (JSON Schema 2020-12, como OpenAPI 3.1). La API comprueba en su propio CI que
 * cumple el contrato que publica; spin-trainer-qa lo vuelve a hacer contra la API desplegada (ADR-0008).
 */
public final class OpenApiContract {

    private static final String SPEC_IRI = "https://spin-trainer.local/openapi.yaml";

    private final JsonNode spec;
    private final SchemaRegistry registry;

    private OpenApiContract(String specText) {
        spec = YAMLMapper.builder().build().readTree(specText);
        registry = SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_2020_12, builder -> builder.schemas(Map.of(SPEC_IRI, specText)));
    }

    public static OpenApiContract load() {
        try {
            return new OpenApiContract(Files.readString(Path.of("openapi.yaml"), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * La respuesta está declarada en la spec y su cuerpo cumple el schema.
     *
     * @param path plantilla de la spec, p. ej. {@code /ranges/user/{situation}/{stack}}
     */
    public void assertResponse(String method, String path, MvcTestResult result) {
        int status = result.getResponse().getStatus();
        String body = contentAsString(result);
        assertThat(violations(method, path, status, body))
                .as("%s %s → %d no cumple openapi.yaml:%n%s", method, path, status, body)
                .isEmpty();
    }

    /** Incumplimientos del contrato de una respuesta; vacío si la cumple. */
    public List<String> violations(String method, String path, int status, String body) {
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
        String mediaType = status >= 400 ? "application/problem+json" : "application/json";
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
