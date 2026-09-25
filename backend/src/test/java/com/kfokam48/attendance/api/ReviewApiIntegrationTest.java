package com.kfokam48.attendance.api;

import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #9 — imposed POST /api/relectures/{id} (EF5, RG4, RG8, RG9). */
class ReviewApiIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final long AUTHOR_ID = 1L;
    private static final long REVIEWER_ID = 2L;
    private static final long OUTSIDER_ID = 3L;

    @Autowired private ReviewRepository reviews;

    @Test
    void should_return200_andMarkExerciseRelu_when_assigneeRendersReview_EF5() {
        Assignment a = assignedReview();

        ResponseEntity<Map> response = render(a.reviewId(), body(15, "Clean code", REVIEWER_ID));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusOfExercises(a.sessionId())).containsExactly("RELU");
    }

    @Test
    void should_return200_when_bodyHasOnlyTheImposedFields() {
        Assignment a = assignedReview();

        ResponseEntity<Map> response = render(a.reviewId(), Map.of("note", 12, "commentaire", "OK"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void should_return400_NOTE_INVALIDE_when_gradeAboveTwenty_RG8() {
        assertNoteInvalid(21);
    }

    @Test
    void should_return400_NOTE_INVALIDE_when_gradeNegative_RG8() {
        assertNoteInvalid(-1);
    }

    @Test
    void should_return400_NOTE_INVALIDE_when_gradeIsNotAnInteger_RG8() {
        assertNoteInvalid(12.5);
    }

    @Test
    void should_return400_CHAMP_MANQUANT_when_commentMissing() {
        Assignment a = assignedReview();

        ResponseEntity<Map> response = render(a.reviewId(), Map.of("note", 12));

        assertError(response, HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT");
    }

    @Test
    void should_return403_AUTO_RELECTURE_when_authorReviewsOwnExercise_RG4() {
        Assignment a = assignedReview();

        ResponseEntity<Map> response = render(a.reviewId(), body(20, "Mine", AUTHOR_ID));

        assertError(response, HttpStatus.FORBIDDEN, "AUTO_RELECTURE");
    }

    @Test
    void should_return403_RELECTURE_NON_ASSIGNEE_when_callerIsNotTheAssignee() {
        Assignment a = assignedReview();

        ResponseEntity<Map> response = render(a.reviewId(), body(10, "Not mine", OUTSIDER_ID));

        assertError(response, HttpStatus.FORBIDDEN, "RELECTURE_NON_ASSIGNEE");
    }

    @Test
    void should_return409_RELECTURE_DEJA_RENDUE_when_renderingTwice() {
        Assignment a = assignedReview();
        render(a.reviewId(), body(15, "First", REVIEWER_ID));

        ResponseEntity<Map> response = render(a.reviewId(), body(16, "Second", REVIEWER_ID));

        assertError(response, HttpStatus.CONFLICT, "RELECTURE_DEJA_RENDUE");
    }

    @Test
    void should_return409_SESSION_CLOTUREE_when_sessionClosed_RG9() {
        Assignment a = assignedReview();
        rest.postForEntity("/api/sessions/" + a.sessionId() + "/cloture", null, Void.class);

        ResponseEntity<Map> response = render(a.reviewId(), body(15, "Late", REVIEWER_ID));

        assertError(response, HttpStatus.CONFLICT, "SESSION_CLOTUREE");
    }

    @Test
    void should_return404_RELECTURE_INCONNUE_when_reviewUnknown() {
        ResponseEntity<Map> response = render(999_999L, body(15, "Ghost", REVIEWER_ID));

        assertError(response, HttpStatus.NOT_FOUND, "RELECTURE_INCONNUE");
    }

    // ---------- fixtures ----------

    private record Assignment(Object sessionId, Long reviewId) {}

    /** Author and reviewer attend, the author submits: the only eligible reviewer is drawn (RG6). */
    private Assignment assignedReview() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), AUTHOR_ID);
        markAttendance(session.get("code"), REVIEWER_ID);
        Number exerciseId = (Number) rest.postForEntity("/api/exercices", Map.of("sessionId", session.get("id"),
                "etudiantId", AUTHOR_ID, "lien", "https://github.com/a/review"), Map.class).getBody().get("id");
        Review review = reviews.findByExerciseIdIn(List.of(exerciseId.longValue())).getFirst();
        return new Assignment(session.get("id"), review.getId());
    }

    private void assertNoteInvalid(Object note) {
        Assignment a = assignedReview();
        assertError(render(a.reviewId(), body(note, "Grade", REVIEWER_ID)), HttpStatus.BAD_REQUEST, "NOTE_INVALIDE");
    }

    private Map<String, Object> body(Object note, String comment, long reviewerId) {
        Map<String, Object> body = new HashMap<>();
        body.put("note", note);
        body.put("commentaire", comment);
        body.put("relecteurId", reviewerId);
        return body;
    }

    private ResponseEntity<Map> render(Long reviewId, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity("/api/relectures/" + reviewId, new HttpEntity<>(body, headers), Map.class);
    }

    private List<Object> statusOfExercises(Object sessionId) {
        List<Map<String, Object>> list = rest.getForObject("/api/sessions/" + sessionId + "/exercices", List.class);
        return list.stream().map(e -> e.get("statut")).toList();
    }

    private void assertError(ResponseEntity<Map> response, HttpStatus status, String code) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).containsOnlyKeys("code", "message");
        assertThat(response.getBody().get("code")).isEqualTo(code);
    }
}
