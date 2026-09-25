package com.kfokam48.attendance.api;

import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #15 — the author sees grades and comments, never the reviewer (EF9, RG7, Q8). */
class StudentExercisesApiIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final long AUTHOR_ID = 3L;
    private static final long REVIEWER_ID = 4L;

    @Autowired private ReviewRepository reviews;

    @Test
    void should_showGradeAndComment_withoutReviewerIdentity_RG7() {
        Map<String, Object> session = openSession();
        Long exerciseId = submittedAndAssigned(session);
        Review review = reviews.findByExerciseIdIn(List.of(exerciseId)).getFirst();
        rest.postForEntity("/api/relectures/" + review.getId(),
                Map.of("note", 17, "commentaire", "Well done", "relecteurId", REVIEWER_ID), Map.class);

        Map<String, Object> exercise = exercisesOf(AUTHOR_ID, session.get("id")).getFirst();

        assertThat(exercise).containsOnlyKeys("id", "sessionId", "etudiantId", "etudiantNom", "lien", "statut", "relecture");
        assertThat(exercise.get("statut")).isEqualTo("RELU");
        assertThat((Map<String, Object>) exercise.get("relecture"))
                .containsOnlyKeys("note", "commentaire")
                .containsEntry("note", 17)
                .containsEntry("commentaire", "Well done");
    }

    @Test
    void should_returnNullReview_when_notRenderedYet() {
        Map<String, Object> session = openSession();
        submittedAndAssigned(session);

        Map<String, Object> exercise = exercisesOf(AUTHOR_ID, session.get("id")).getFirst();

        assertThat(exercise.get("statut")).isEqualTo("EN_ATTENTE_RELECTURE");
        assertThat(exercise).containsEntry("relecture", null);
    }

    @Test
    void should_return404_ETUDIANT_INCONNU_when_studentUnknown() {
        ResponseEntity<Map> response = rest.getForEntity("/api/etudiants/999999/exercices", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("ETUDIANT_INCONNU");
    }

    private Long submittedAndAssigned(Map<String, Object> session) {
        markAttendance(session.get("code"), AUTHOR_ID);
        markAttendance(session.get("code"), REVIEWER_ID);
        Number id = (Number) rest.postForEntity("/api/exercices", Map.of("sessionId", session.get("id"),
                "etudiantId", AUTHOR_ID, "lien", "https://github.com/a/mine"), Map.class).getBody().get("id");
        return id.longValue();
    }

    private List<Map<String, Object>> exercisesOf(long studentId, Object sessionId) {
        return rest.exchange("/api/etudiants/" + studentId + "/exercices?sessionId=" + sessionId, HttpMethod.GET,
                null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }
}
