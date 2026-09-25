package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #11 — the trainer adds an attendance by hand, visibly (EF7, RG12, Q14). */
class ManualAttendanceApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void should_return201_withSourceFormateur_when_trainerAddsAttendance_RG12() {
        Object sessionId = openSession().get("id");

        ResponseEntity<Map> response = addManually(sessionId, 4L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("source")).isEqualTo("FORMATEUR");
    }

    @Test
    void should_return409_DEJA_PRESENT_when_studentAlreadyPresent() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), 4L);

        ResponseEntity<Map> response = addManually(session.get("id"), 4L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("DEJA_PRESENT");
    }

    @Test
    void should_return404_SESSION_INCONNUE_when_sessionUnknown() {
        ResponseEntity<Map> response = addManually(999_999, 4L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("SESSION_INCONNUE");
    }

    @Test
    void should_return404_ETUDIANT_INCONNU_when_studentUnknown() {
        Object sessionId = openSession().get("id");

        ResponseEntity<Map> response = addManually(sessionId, 999_999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("ETUDIANT_INCONNU");
    }

    private ResponseEntity<Map> addManually(Object sessionId, long studentId) {
        return rest.postForEntity("/api/sessions/" + sessionId + "/presences",
                Map.of("etudiantId", studentId), Map.class);
    }
}
