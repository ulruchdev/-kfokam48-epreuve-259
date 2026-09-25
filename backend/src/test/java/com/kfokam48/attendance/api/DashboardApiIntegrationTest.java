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

/**
 * Issue #10 — imposed GET /api/tableau (EF6, Q16). The database is shared by all test
 * classes, so counters are asserted as deltas around the actions of each test.
 */
class DashboardApiIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final long AUTHOR_ID = 5L;
    private static final long REVIEWER_ID = 4L;
    private static final long NEVER_GRADED_ID = 6L;

    @Autowired private ReviewRepository reviews;

    @Test
    void should_return200_withTheSixImposedFields_forEveryStudent_EF6() {
        List<Map<String, Object>> rows = dashboard();

        assertThat(rows).hasSize(6);
        assertThat(rows).allSatisfy(row -> assertThat(row).containsOnlyKeys(
                "etudiantId", "nom", "presences", "exercicesDeposes", "moyenne", "relecturesEnAttente"));
    }

    @Test
    void should_countAttendance_submission_pendingReview_thenAverage_Q16() {
        Map<String, Object> session = openSession();
        Map<String, Object> authorBefore = row(AUTHOR_ID);
        Map<String, Object> reviewerBefore = row(REVIEWER_ID);

        markAttendance(session.get("code"), AUTHOR_ID);
        markAttendance(session.get("code"), REVIEWER_ID);
        Number exerciseId = (Number) rest.postForEntity("/api/exercices", Map.of("sessionId", session.get("id"),
                "etudiantId", AUTHOR_ID, "lien", "https://github.com/a/dashboard"), Map.class).getBody().get("id");

        assertThat(delta(row(AUTHOR_ID), authorBefore, "presences")).isEqualTo(1);
        assertThat(delta(row(AUTHOR_ID), authorBefore, "exercicesDeposes")).isEqualTo(1);
        assertThat(delta(row(REVIEWER_ID), reviewerBefore, "relecturesEnAttente")).isEqualTo(1);

        Review review = reviews.findByExerciseIdIn(List.of(exerciseId.longValue())).getFirst();
        rest.postForEntity("/api/relectures/" + review.getId(),
                Map.of("note", 14, "commentaire", "Good", "relecteurId", REVIEWER_ID), Map.class);

        assertThat(delta(row(REVIEWER_ID), reviewerBefore, "relecturesEnAttente")).isZero();
        assertThat(row(AUTHOR_ID).get("moyenne")).isEqualTo(14.0);
    }

    @Test
    void should_returnNullAverage_notZero_when_studentHasNoGrade() {
        assertThat(row(NEVER_GRADED_ID)).containsEntry("moyenne", null);
    }

    @Test
    void should_return404_PROMOTION_INCONNUE_when_promotionUnknown() {
        ResponseEntity<Map> response = rest.getForEntity("/api/tableau?promotionId=999", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("PROMOTION_INCONNUE");
    }

    @Test
    void should_return400_PARAMETRE_MANQUANT_when_promotionIdMissing() {
        ResponseEntity<Map> response = rest.getForEntity("/api/tableau", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("PARAMETRE_MANQUANT");
    }

    private List<Map<String, Object>> dashboard() {
        return rest.exchange("/api/tableau?promotionId=" + DEMO_PROMOTION_ID, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }

    private Map<String, Object> row(long studentId) {
        return dashboard().stream()
                .filter(r -> ((Number) r.get("etudiantId")).longValue() == studentId)
                .findFirst().orElseThrow();
    }

    private int delta(Map<String, Object> after, Map<String, Object> before, String field) {
        return ((Number) after.get(field)).intValue() - ((Number) before.get(field)).intValue();
    }
}
