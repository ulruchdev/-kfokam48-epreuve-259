package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #7 — imposed POST /api/exercices (EF3, RG14, RG15) and the session exercise list. */
class ExerciseApiIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String LINK = "https://github.com/student/exercise-1";

    @Test
    void should_return201_pendingAssignment_when_authorIsAloneInSession_RG15() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), 1L);

        ResponseEntity<Map> response = submit(session.get("id"), 1L, LINK);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKeys("id", "statut");
        assertThat(response.getBody().get("statut")).isEqualTo("EN_ATTENTE_AFFECTATION");
    }

    @Test
    void should_assignReviewer_when_anotherStudentAttendsLater_RG15() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), 1L);
        submit(session.get("id"), 1L, LINK);

        markAttendance(session.get("code"), 2L);

        assertThat(exercisesOf(session.get("id")))
                .singleElement()
                .satisfies(exercise -> assertThat(exercise.get("statut")).isEqualTo("EN_ATTENTE_RELECTURE"));
    }

    @Test
    void should_return400_PRESENCE_REQUISE_when_studentDidNotAttend_RG14() {
        Object sessionId = openSession().get("id");

        ResponseEntity<Map> response = submit(sessionId, 3L, LINK);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("PRESENCE_REQUISE");
    }

    @Test
    void should_return400_LIEN_INVALIDE_when_linkIsNotAnHttpUrl() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), 1L);

        ResponseEntity<Map> response = submit(session.get("id"), 1L, "not a link");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("LIEN_INVALIDE");
    }

    @Test
    void should_return409_EXERCICE_DEJA_DEPOSE_when_submittingTwice() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), 1L);
        submit(session.get("id"), 1L, LINK);

        ResponseEntity<Map> response = submit(session.get("id"), 1L, LINK);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("EXERCICE_DEJA_DEPOSE");
    }

    @Test
    void should_return404_SESSION_INCONNUE_when_sessionUnknown() {
        ResponseEntity<Map> response = submit(999_999, 1L, LINK);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("SESSION_INCONNUE");
    }

    @Test
    void should_return404_ETUDIANT_INCONNU_when_studentUnknown() {
        Object sessionId = openSession().get("id");

        ResponseEntity<Map> response = submit(sessionId, 999_999L, LINK);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("ETUDIANT_INCONNU");
    }

    @Test
    void should_return400_CHAMP_MANQUANT_when_linkMissing() {
        Object sessionId = openSession().get("id");

        ResponseEntity<Map> response = rest.postForEntity("/api/exercices",
                Map.of("sessionId", sessionId, "etudiantId", 1L), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("CHAMP_MANQUANT");
    }

    @Test
    void should_return404_SESSION_INCONNUE_when_listingExercisesOfUnknownSession() {
        ResponseEntity<Map> response = rest.getForEntity("/api/sessions/999999/exercices", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("SESSION_INCONNUE");
    }

    private ResponseEntity<Map> submit(Object sessionId, long studentId, String link) {
        return rest.postForEntity("/api/exercices",
                Map.of("sessionId", sessionId, "etudiantId", studentId, "lien", link), Map.class);
    }

    private List<Map<String, Object>> exercisesOf(Object sessionId) {
        return rest.exchange("/api/sessions/" + sessionId + "/exercices", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }
}
