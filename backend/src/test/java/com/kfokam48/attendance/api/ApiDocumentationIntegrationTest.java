package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #37 — the API is explorable in Swagger UI, next to the frozen contract. */
class ApiDocumentationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void should_exposeGeneratedOpenApi_withTheImposedOperations() {
        ResponseEntity<Map> response = rest.getForEntity("/v3/api-docs", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody().get("paths"))
                .containsKeys("/api/sessions", "/api/presences", "/api/exercices", "/api/relectures/{id}", "/api/tableau");
    }

    @Test
    void should_serveSwaggerUi() {
        ResponseEntity<String> response = rest.getForEntity("/swagger-ui/index.html", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("swagger-ui");
    }

    @Test
    void should_serveTheFrozenContract_asTheSourceOfTruth() {
        ResponseEntity<String> response = rest.getForEntity("/contrat.yaml", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("openapi: 3.0.3").contains("/api/tableau");
    }
}
