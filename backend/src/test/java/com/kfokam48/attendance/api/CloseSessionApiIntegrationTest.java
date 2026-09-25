package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #12 — the trainer closes the session: everything freezes (EF8, RG9, RG10). */
class CloseSessionApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void should_return204_andStatusCloturee_when_closingOpenSession_EF8() {
        Object sessionId = openSession().get("id");

        ResponseEntity<Void> response = close(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.getForEntity("/api/sessions/" + sessionId, Map.class).getBody().get("statut"))
                .isEqualTo("CLOTUREE");
    }

    @Test
    void should_return409_SESSION_DEJA_CLOTUREE_when_closingTwice() {
        Object sessionId = openSession().get("id");
        close(sessionId);

        ResponseEntity<Map> response = rest.postForEntity("/api/sessions/" + sessionId + "/cloture", null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("SESSION_DEJA_CLOTUREE");
    }

    @Test
    void should_return404_SESSION_INCONNUE_when_closingUnknownSession() {
        ResponseEntity<Map> response = rest.postForEntity("/api/sessions/999999/cloture", null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("SESSION_INCONNUE");
    }

    @Test
    void should_return409_SESSION_CLOTUREE_when_submittingAfterClosure_RG10() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), 1L);
        close(session.get("id"));

        ResponseEntity<Map> response = rest.postForEntity("/api/exercices",
                Map.of("sessionId", session.get("id"), "etudiantId", 1L, "lien", "https://github.com/a/b"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("SESSION_CLOTUREE");
    }

    @Test
    void should_return409_SESSION_CLOTUREE_when_trainerAddsAttendanceAfterClosure() {
        Object sessionId = openSession().get("id");
        close(sessionId);

        ResponseEntity<Map> response = rest.postForEntity("/api/sessions/" + sessionId + "/presences",
                Map.of("etudiantId", 2L), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("SESSION_CLOTUREE");
    }

    @Test
    void should_return409_SESSION_CLOTUREE_when_adjustingEndAfterClosure() {
        Map<String, Object> session = openSession();
        close(session.get("id"));
        String finAt = OffsetDateTime.parse((String) session.get("ouvertureAt")).plusHours(1).toString();

        ResponseEntity<Map> response = rest.exchange("/api/sessions/" + session.get("id"), HttpMethod.PUT,
                new HttpEntity<>(Map.of("finAt", finAt)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("SESSION_CLOTUREE");
    }

    @Test
    void should_return410_CODE_EXPIRE_when_usingTheCodeOfAClosedSession_RG2() {
        Map<String, Object> session = openSession();
        close(session.get("id"));

        ResponseEntity<Map> response = markAttendance(session.get("code"), 3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody().get("code")).isEqualTo("CODE_EXPIRE");
    }

    private ResponseEntity<Void> close(Object sessionId) {
        return rest.postForEntity("/api/sessions/" + sessionId + "/cloture", null, Void.class);
    }
}
