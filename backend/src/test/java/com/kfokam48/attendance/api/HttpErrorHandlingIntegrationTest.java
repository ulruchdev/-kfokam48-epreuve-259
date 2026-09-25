package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENF4 — every protocol-level error keeps its real HTTP status AND the imposed
 * {"code", "message"} body: never a 500 for a client mistake, never a Spring default page.
 */
class HttpErrorHandlingIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void should_return405_METHODE_NON_AUTORISEE_when_verbNotSupported() {
        ResponseEntity<Map> response = rest.exchange("/api/sessions", HttpMethod.DELETE, null, Map.class);

        assertError(response, HttpStatus.METHOD_NOT_ALLOWED, "METHODE_NON_AUTORISEE");
    }

    @Test
    void should_return415_TYPE_NON_SUPPORTE_when_bodyIsNotJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        ResponseEntity<Map> response = rest.postForEntity("/api/sessions",
                new HttpEntity<>("titre=x", headers), Map.class);

        assertError(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "TYPE_NON_SUPPORTE");
    }

    @Test
    void should_return400_CORPS_INVALIDE_when_jsonIsMalformed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = rest.postForEntity("/api/sessions",
                new HttpEntity<>("{\"titre\": ", headers), Map.class);

        assertError(response, HttpStatus.BAD_REQUEST, "CORPS_INVALIDE");
    }

    @Test
    void should_return400_PARAMETRE_INVALIDE_when_pathIdIsNotANumber() {
        ResponseEntity<Map> response = rest.getForEntity("/api/sessions/abc", Map.class);

        assertError(response, HttpStatus.BAD_REQUEST, "PARAMETRE_INVALIDE");
    }

    @Test
    void should_return404_SESSION_INCONNUE_when_sessionIdUnknown() {
        ResponseEntity<Map> response = rest.getForEntity("/api/sessions/999999", Map.class);

        assertError(response, HttpStatus.NOT_FOUND, "SESSION_INCONNUE");
    }

    private void assertError(ResponseEntity<Map> response, HttpStatus status, String code) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).containsOnlyKeys("code", "message");
        assertThat(response.getBody().get("code")).isEqualTo(code);
    }
}
