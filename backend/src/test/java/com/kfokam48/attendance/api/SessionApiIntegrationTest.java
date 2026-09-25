package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #5 — imposed POST /api/sessions (EF1, RG1, DEC-2, DEC-5) and free session endpoints.
 * Runs on a virgin machine: PostgreSQL comes from Testcontainers (B6, ENF5).
 */
class SessionApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void should_return201_withImposedFields_when_openingSession_EF1() {
        ResponseEntity<Map> response = open(Map.of("titre", "Integration test", "promotionId", DEMO_PROMOTION_ID));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKeys("id", "code", "ouvertureAt", "expirationAt", "finAt", "statut");
        assertThat((String) response.getBody().get("code")).hasSize(6);
    }

    @Test
    void should_expireCodeFifteenMinutesAfterOpening_RG1() {
        Map body = open(Map.of("titre", "RG1", "promotionId", DEMO_PROMOTION_ID)).getBody();

        assertThat(minutesBetween(body.get("ouvertureAt"), body.get("expirationAt"))).isEqualTo(15);
    }

    @Test
    void should_endSessionAfterRequestedDuration_when_dureeMinutesGiven_DEC2() {
        Map body = open(Map.of("titre", "DEC-2", "promotionId", DEMO_PROMOTION_ID, "dureeMinutes", 45)).getBody();

        assertThat(minutesBetween(body.get("ouvertureAt"), body.get("finAt"))).isEqualTo(45);
    }

    @Test
    void should_return400_CHAMP_MANQUANT_inImposedFormat_when_titreMissing() {
        ResponseEntity<Map> response = open(Map.of("promotionId", DEMO_PROMOTION_ID));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsOnlyKeys("code", "message");
        assertThat(response.getBody().get("code")).isEqualTo("CHAMP_MANQUANT");
    }

    @Test
    void should_return400_PROMOTION_INCONNUE_when_promotionDoesNotExist() {
        ResponseEntity<Map> response = open(Map.of("titre", "Ghost", "promotionId", 999));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("PROMOTION_INCONNUE");
    }

    @Test
    void should_return400_PARAMETRE_MANQUANT_when_listingSessionsWithoutPromotionId() {
        ResponseEntity<Map> response = rest.getForEntity("/api/sessions", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsOnlyKeys("code", "message");
        assertThat(response.getBody().get("code")).isEqualTo("PARAMETRE_MANQUANT");
    }

    @Test
    void should_adjustEndTime_when_finAtAfterOpening_DEC2() {
        Map opened = open(Map.of("titre", "Adjust", "promotionId", DEMO_PROMOTION_ID)).getBody();
        String newEnd = OffsetDateTime.parse((String) opened.get("ouvertureAt")).plusMinutes(30).toString();

        ResponseEntity<Map> response = adjustEnd(opened.get("id"), newEnd);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(minutesBetween(opened.get("ouvertureAt"), response.getBody().get("finAt"))).isEqualTo(30);
    }

    @Test
    void should_return400_FIN_AVANT_OUVERTURE_when_finAtBeforeOpening_DEC2() {
        Map opened = open(Map.of("titre", "Adjust KO", "promotionId", DEMO_PROMOTION_ID)).getBody();
        String before = OffsetDateTime.parse((String) opened.get("ouvertureAt")).minusMinutes(1).toString();

        ResponseEntity<Map> response = adjustEnd(opened.get("id"), before);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("FIN_AVANT_OUVERTURE");
    }

    private ResponseEntity<Map> open(Map<String, Object> request) {
        return rest.postForEntity("/api/sessions", request, Map.class);
    }

    private ResponseEntity<Map> adjustEnd(Object sessionId, String finAt) {
        return rest.exchange("/api/sessions/" + sessionId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("finAt", finAt)), Map.class);
    }

    private long minutesBetween(Object from, Object to) {
        return Duration.between(OffsetDateTime.parse((String) from), OffsetDateTime.parse((String) to)).toMinutes();
    }
}
