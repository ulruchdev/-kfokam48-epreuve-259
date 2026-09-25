package com.kfokam48.attendance.api;

import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #13 — the reviewer amends a rendered review until closure (EF11, RG9, DEC-1). */
class AmendReviewApiIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final long AUTHOR_ID = 1L;
    private static final long REVIEWER_ID = 5L;

    @Autowired private ReviewRepository reviews;

    @Test
    void should_return200_withNewGrade_when_amendingBeforeClosure_RG9() {
        Fixture f = renderedReview();

        ResponseEntity<Map> response = amend(f.reviewId(), 18, "Second look");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsOnlyKeys("id", "exerciceId", "relecteurId", "note", "commentaire", "rendue")
                .containsEntry("note", 18)
                .containsEntry("commentaire", "Second look")
                .containsEntry("rendue", true);
    }

    @Test
    void should_return409_SESSION_CLOTUREE_when_amendingAfterClosure_RG9() {
        Fixture f = renderedReview();
        rest.postForEntity("/api/sessions/" + f.sessionId() + "/cloture", null, Void.class);

        assertError(amend(f.reviewId(), 18, "Too late"), HttpStatus.CONFLICT, "SESSION_CLOTUREE");
    }

    @Test
    void should_return409_RELECTURE_NON_RENDUE_when_amendingAReviewNeverRendered() {
        Fixture f = assignedReview();

        assertError(amend(f.reviewId(), 18, "Nothing to amend"), HttpStatus.CONFLICT, "RELECTURE_NON_RENDUE");
    }

    @Test
    void should_return400_NOTE_INVALIDE_when_amendedGradeOutOfRange_RG8() {
        Fixture f = renderedReview();

        assertError(amend(f.reviewId(), 25, "Too high"), HttpStatus.BAD_REQUEST, "NOTE_INVALIDE");
    }

    @Test
    void should_return403_RELECTURE_NON_ASSIGNEE_when_callerIsNotTheAssignee() {
        Fixture f = renderedReview();

        ResponseEntity<Map> response = rest.exchange("/api/relectures/" + f.reviewId(), HttpMethod.PUT,
                new HttpEntity<>(Map.of("note", 10, "commentaire", "Hijack", "relecteurId", 3L)), Map.class);

        assertError(response, HttpStatus.FORBIDDEN, "RELECTURE_NON_ASSIGNEE");
    }

    @Test
    void should_return404_RELECTURE_INCONNUE_when_reviewUnknown() {
        assertError(amend(999_999L, 10, "Ghost"), HttpStatus.NOT_FOUND, "RELECTURE_INCONNUE");
    }

    private record Fixture(Object sessionId, Long reviewId) {}

    private Fixture assignedReview() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), AUTHOR_ID);
        markAttendance(session.get("code"), REVIEWER_ID);
        Number exerciseId = (Number) rest.postForEntity("/api/exercices", Map.of("sessionId", session.get("id"),
                "etudiantId", AUTHOR_ID, "lien", "https://github.com/a/amend"), Map.class).getBody().get("id");
        Review review = reviews.findByExerciseIdIn(List.of(exerciseId.longValue())).getFirst();
        return new Fixture(session.get("id"), review.getId());
    }

    private Fixture renderedReview() {
        Fixture f = assignedReview();
        rest.postForEntity("/api/relectures/" + f.reviewId(),
                Map.of("note", 12, "commentaire", "First look", "relecteurId", REVIEWER_ID), Map.class);
        return f;
    }

    private ResponseEntity<Map> amend(Long reviewId, int note, String comment) {
        return rest.exchange("/api/relectures/" + reviewId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("note", note, "commentaire", comment, "relecteurId", REVIEWER_ID)), Map.class);
    }

    private void assertError(ResponseEntity<Map> response, HttpStatus status, String code) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).containsOnlyKeys("code", "message");
        assertThat(response.getBody().get("code")).isEqualTo(code);
    }
}
