package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #38 — the app opens on a realistic past session (demo seed, V3). */
class SeedDataIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String SEED_TITLE = "Séance 1 — Introduction à Spring Boot";

    @Test
    void should_offerAClosedPastSession_whoseExercisesAwaitTheirSecondReviewer_DEC14() {
        Map<String, Object> seeded = list("/api/sessions?promotionId=" + DEMO_PROMOTION_ID).stream()
                .filter(session -> SEED_TITLE.equals(session.get("titre")))
                .findFirst().orElseThrow();

        assertThat(seeded.get("statut")).isEqualTo("CLOTUREE");
        assertThat(list("/api/sessions/" + seeded.get("id") + "/exercices"))
                .extracting(exercise -> exercise.get("statut"))
                .containsExactly("EN_ATTENTE_AFFECTATION", "EN_ATTENTE_AFFECTATION", "EN_ATTENTE_AFFECTATION");
    }

    @Test
    void should_showGradesAndAPendingReview_onTheDashboard_Q16() {
        List<Map<String, Object>> rows = list("/api/tableau?promotionId=" + DEMO_PROMOTION_ID);

        assertThat(rows).anySatisfy(row -> assertThat(row.get("moyenne")).isNotNull());
        assertThat(rows).anySatisfy(row -> assertThat((Integer) row.get("relecturesEnAttente")).isPositive());
        assertThat(rows).anySatisfy(row -> assertThat((Integer) row.get("presences")).isPositive());
    }

    private List<Map<String, Object>> list(String url) {
        return rest.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }
}
