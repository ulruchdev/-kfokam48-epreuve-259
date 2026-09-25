package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #41 — « ajouté par le formateur » is visible in the dashboard (EF7, RG12, Q14). */
class TrainerAttendanceVisibleIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final long STUDENT_ID = 2L;

    @Test
    void should_countManualAttendanceSeparately_inTheDashboard_RG12() {
        Map<String, Object> before = row(STUDENT_ID);
        Object sessionId = openSession().get("id");

        rest.postForEntity("/api/sessions/" + sessionId + "/presences", Map.of("etudiantId", STUDENT_ID), Map.class);

        Map<String, Object> after = row(STUDENT_ID);
        assertThat(delta(after, before, "presences")).isEqualTo(1);
        assertThat(delta(after, before, "presencesFormateur")).isEqualTo(1);
    }

    @Test
    void should_notCountSelfMarkedAttendance_asAddedByTrainer() {
        Map<String, Object> before = row(STUDENT_ID);
        Map<String, Object> session = openSession();

        markAttendance(session.get("code"), STUDENT_ID);

        Map<String, Object> after = row(STUDENT_ID);
        assertThat(delta(after, before, "presences")).isEqualTo(1);
        assertThat(delta(after, before, "presencesFormateur")).isZero();
    }

    private Map<String, Object> row(long studentId) {
        List<Map<String, Object>> rows = rest.exchange("/api/tableau?promotionId=" + DEMO_PROMOTION_ID, HttpMethod.GET,
                null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
        return rows.stream().filter(r -> ((Number) r.get("etudiantId")).longValue() == studentId).findFirst().orElseThrow();
    }

    private int delta(Map<String, Object> after, Map<String, Object> before, String field) {
        return ((Number) after.get(field)).intValue() - ((Number) before.get(field)).intValue();
    }
}
