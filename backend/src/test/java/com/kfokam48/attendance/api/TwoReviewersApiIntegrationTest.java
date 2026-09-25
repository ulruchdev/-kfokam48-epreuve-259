package com.kfokam48.attendance.api;

import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #62 — step-3 change of need: two distinct reviewers per exercise (RG5 revised),
 * retained grade = average, provisional while only one is rendered (RG16).
 */
class TwoReviewersApiIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final long AUTHOR = 1L;
    private static final long PEER_B = 2L;
    private static final long PEER_C = 3L;
    private static final long PEER_D = 4L;

    @Autowired private ReviewRepository reviews;

    @Test
    void should_drawTwoDistinctReviewers_neverTheAuthor_neverAThird_RG5() {
        Map<String, Object> session = openSession();
        for (long student : List.of(AUTHOR, PEER_B, PEER_C, PEER_D)) {
            markAttendance(session.get("code"), student);
        }

        Long exerciseId = submit(session, AUTHOR);

        List<Review> assigned = reviews.findByExerciseIdIn(List.of(exerciseId));
        assertThat(assigned).hasSize(2);
        assertThat(assigned).extracting(Review::getReviewerId).doesNotHaveDuplicates().doesNotContain(AUTHOR);
        assertThat(statusOf(session, exerciseId)).isEqualTo("EN_ATTENTE_RELECTURE");
    }

    @Test
    void should_drawTheMissingReviewers_onLaterAttendances_RG15() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), AUTHOR);
        Long exerciseId = submit(session, AUTHOR);

        markAttendance(session.get("code"), PEER_B);
        assertThat(reviews.findByExerciseIdIn(List.of(exerciseId))).hasSize(1);
        assertThat(statusOf(session, exerciseId)).isEqualTo("EN_ATTENTE_AFFECTATION");

        markAttendance(session.get("code"), PEER_C);
        markAttendance(session.get("code"), PEER_D);
        assertThat(reviews.findByExerciseIdIn(List.of(exerciseId)))
                .extracting(Review::getReviewerId).containsExactlyInAnyOrder(PEER_B, PEER_C);
        assertThat(statusOf(session, exerciseId)).isEqualTo("EN_ATTENTE_RELECTURE");
    }

    @Test
    void should_showAProvisionalGrade_thenTheAverage_onceBothReviewsAreRendered_RG16() {
        Map<String, Object> session = openSession();
        for (long student : List.of(AUTHOR, PEER_B, PEER_C)) {
            markAttendance(session.get("code"), student);
        }
        Long exerciseId = submit(session, AUTHOR);
        List<Review> assigned = reviews.findByExerciseIdIn(List.of(exerciseId));

        render(assigned.get(0), 12, "Readable");
        Map<String, Object> afterFirst = evaluationOf(session, exerciseId);
        assertThat(afterFirst).containsEntry("provisoire", true);
        assertThat(((Number) afterFirst.get("note")).doubleValue()).isEqualTo(12.0);
        assertThat(statusOf(session, exerciseId)).isEqualTo("EN_ATTENTE_RELECTURE");

        render(assigned.get(1), 15, "Solid tests");
        Map<String, Object> afterBoth = evaluationOf(session, exerciseId);
        assertThat(afterBoth).containsOnlyKeys("note", "provisoire", "commentaires");
        assertThat(afterBoth).containsEntry("provisoire", false);
        assertThat(((Number) afterBoth.get("note")).doubleValue()).isEqualTo(13.5);
        assertThat((List<String>) afterBoth.get("commentaires")).containsExactlyInAnyOrder("Readable", "Solid tests");
        assertThat(statusOf(session, exerciseId)).isEqualTo("RELU");
    }

    @Test
    void should_returnNullEvaluation_when_noReviewRendered() {
        Map<String, Object> session = openSession();
        for (long student : List.of(AUTHOR, PEER_B, PEER_C)) {
            markAttendance(session.get("code"), student);
        }
        Long exerciseId = submit(session, AUTHOR);

        Map<String, Object> exercise = exerciseOf(session, exerciseId);
        assertThat(exercise).containsKey("evaluation").doesNotContainKey("relecture");
        assertThat(exercise.get("evaluation")).isNull();
    }

    // ---------- helpers ----------

    private Long submit(Map<String, Object> session, long authorId) {
        Number id = (Number) rest.postForEntity("/api/exercices", Map.of("sessionId", session.get("id"),
                "etudiantId", authorId, "lien", "https://github.com/a/two-reviewers"), Map.class).getBody().get("id");
        return id.longValue();
    }

    private void render(Review review, int note, String comment) {
        rest.postForEntity("/api/relectures/" + review.getId(),
                Map.of("note", note, "commentaire", comment, "relecteurId", review.getReviewerId()), Map.class);
    }

    private Map<String, Object> exerciseOf(Map<String, Object> session, Long exerciseId) {
        List<Map<String, Object>> list = rest.exchange("/api/etudiants/" + AUTHOR + "/exercices?sessionId=" + session.get("id"),
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
        return list.stream().filter(e -> ((Number) e.get("id")).longValue() == exerciseId).findFirst().orElseThrow();
    }

    private Map<String, Object> evaluationOf(Map<String, Object> session, Long exerciseId) {
        return (Map<String, Object>) exerciseOf(session, exerciseId).get("evaluation");
    }

    private Object statusOf(Map<String, Object> session, Long exerciseId) {
        return exerciseOf(session, exerciseId).get("statut");
    }
}
