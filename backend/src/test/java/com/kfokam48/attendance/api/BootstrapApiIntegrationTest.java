package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #17 — the app starts on a virgin database with demo data and the imposed error format. */
class BootstrapApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void should_listDemoPromotion_when_appStartsOnVirginDatabase() {
        ResponseEntity<List> response = rest.getForEntity("/api/promotions", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void should_listSixDemoStudents_when_promotionIsTheDemoOne() {
        ResponseEntity<List> response =
                rest.getForEntity("/api/promotions/" + DEMO_PROMOTION_ID + "/etudiants", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(6);
    }

    @Test
    void should_return404_PROMOTION_INCONNUE_inImposedFormat_when_promotionUnknown() {
        ResponseEntity<Map> response = rest.getForEntity("/api/promotions/999/etudiants", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsOnlyKeys("code", "message");
        assertThat(response.getBody().get("code")).isEqualTo("PROMOTION_INCONNUE");
    }

    @Test
    void should_return404_inImposedFormat_when_routeUnknown() {
        ResponseEntity<Map> response = rest.getForEntity("/api/nothing-here", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsOnlyKeys("code", "message");
    }
}
