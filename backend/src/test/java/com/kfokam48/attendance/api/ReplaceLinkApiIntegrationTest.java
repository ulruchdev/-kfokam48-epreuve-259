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

/** Issue #14 — the author replaces the link while nobody has started reviewing (EF12, RG11, Q13). */
class ReplaceLinkApiIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final long AUTHOR_ID = 2L;
    private static final long REVIEWER_ID = 3L;
    private static final String NEW_LINK = "https://github.com/a/fixed";

    @Autowired private ReviewRepository reviews;

    @Test
    void should_return200_withNewLink_when_noReviewStarted_RG11() {
        Fixture f = submitted();

        ResponseEntity<Map> response = replace(f.exerciseId(), NEW_LINK);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("lien", NEW_LINK).containsEntry("statut", "EN_ATTENTE_AFFECTATION");   // one peer: second reviewer missing
    }

    @Test
    void should_return409_RELECTURE_COMMENCEE_when_reviewAlreadyRendered_RG11() {
        Fixture f = submitted();
        Review review = reviews.findByExerciseIdIn(List.of(f.exerciseId())).getFirst();
        rest.postForEntity("/api/relectures/" + review.getId(), Map.of("note", 11, "commentaire", "Read"), Map.class);

        assertError(replace(f.exerciseId(), NEW_LINK), HttpStatus.CONFLICT, "RELECTURE_COMMENCEE");
    }

    @Test
    void should_return409_SESSION_CLOTUREE_when_sessionClosed() {
        Fixture f = submitted();
        rest.postForEntity("/api/sessions/" + f.sessionId() + "/cloture", null, Void.class);

        assertError(replace(f.exerciseId(), NEW_LINK), HttpStatus.CONFLICT, "SESSION_CLOTUREE");
    }

    @Test
    void should_return400_LIEN_INVALIDE_when_newLinkInvalid() {
        Fixture f = submitted();

        assertError(replace(f.exerciseId(), "ftp://nope"), HttpStatus.BAD_REQUEST, "LIEN_INVALIDE");
    }

    @Test
    void should_return404_EXERCICE_INCONNU_when_exerciseUnknown() {
        assertError(replace(999_999L, NEW_LINK), HttpStatus.NOT_FOUND, "EXERCICE_INCONNU");
    }

    private record Fixture(Object sessionId, Long exerciseId) {}

    private Fixture submitted() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), AUTHOR_ID);
        markAttendance(session.get("code"), REVIEWER_ID);
        Number id = (Number) rest.postForEntity("/api/exercices", Map.of("sessionId", session.get("id"),
                "etudiantId", AUTHOR_ID, "lien", "https://github.com/a/first"), Map.class).getBody().get("id");
        return new Fixture(session.get("id"), id.longValue());
    }

    private ResponseEntity<Map> replace(Long exerciseId, String link) {
        return rest.exchange("/api/exercices/" + exerciseId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("lien", link)), Map.class);
    }

    private void assertError(ResponseEntity<Map> response, HttpStatus status, String code) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).containsOnlyKeys("code", "message");
        assertThat(response.getBody().get("code")).isEqualTo(code);
    }
}
