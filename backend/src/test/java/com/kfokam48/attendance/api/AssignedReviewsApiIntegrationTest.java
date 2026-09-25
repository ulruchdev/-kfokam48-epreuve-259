package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #16 — the reviewer lists assigned reviews, pending first (EF13). */
class AssignedReviewsApiIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final long AUTHOR_ID = 3L;
    private static final long REVIEWER_ID = 6L;
    private static final String LINK = "https://github.com/a/to-review";

    @Test
    void should_listAssignment_withLinkAndAuthor_pendingFirst_EF13() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), AUTHOR_ID);
        markAttendance(session.get("code"), REVIEWER_ID);
        rest.postForEntity("/api/exercices",
                Map.of("sessionId", session.get("id"), "etudiantId", AUTHOR_ID, "lien", LINK), Map.class);

        List<Map<String, Object>> assigned = assignedTo(REVIEWER_ID);

        assertThat(assigned).isNotEmpty();
        assertThat(assigned.getFirst()).containsEntry("rendue", false);
        assertThat(assigned).anySatisfy(review -> assertThat(review)
                .containsKeys("id", "exerciceId", "exerciceLien", "auteurNom", "rendue", "note", "commentaire")
                .containsEntry("exerciceLien", LINK)
                .containsEntry("auteurNom", "Djoumessi Paul"));
    }

    @Test
    void should_return400_PARAMETRE_MANQUANT_when_reviewerIdMissing() {
        ResponseEntity<Map> response = rest.getForEntity("/api/relectures", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("PARAMETRE_MANQUANT");
    }

    @Test
    void should_return404_ETUDIANT_INCONNU_when_reviewerUnknown() {
        ResponseEntity<Map> response = rest.getForEntity("/api/relectures?relecteurId=999999", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("ETUDIANT_INCONNU");
    }

    private List<Map<String, Object>> assignedTo(long reviewerId) {
        return rest.exchange("/api/relectures?relecteurId=" + reviewerId, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }
}
