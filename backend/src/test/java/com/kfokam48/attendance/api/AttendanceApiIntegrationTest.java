package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Issue #6 — imposed POST /api/presences (EF2, RG1, RG3). */
class AttendanceApiIntegrationTest extends AbstractPostgresIntegrationTest {

    /** Dedicated to the lock test: RG3 locks this student for two minutes. */
    private static final long LOCKED_STUDENT_ID = 6L;

    @Test
    void should_return201_withSourceEtudiant_when_codeIsValid_EF2() {
        Map<String, Object> session = openSession();

        ResponseEntity<Map> response = markAttendance(session.get("code"), 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKeys("id", "sessionId", "etudiantId", "source");
        assertThat(response.getBody().get("source")).isEqualTo("ETUDIANT");
    }

    @Test
    void should_return409_DEJA_PRESENT_when_markingTwice() {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), 2L);

        ResponseEntity<Map> response = markAttendance(session.get("code"), 2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("DEJA_PRESENT");
    }

    @Test
    void should_return400_CODE_INCONNU_inImposedFormat_when_codeUnknown() {
        ResponseEntity<Map> response = markAttendance("ZZZZZZ", 3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsOnlyKeys("code", "message");
        assertThat(response.getBody().get("code")).isEqualTo("CODE_INCONNU");
    }

    @Test
    void should_lockStudent_evenWithValidCode_afterFiveWrongCodes_RG3() {
        Map<String, Object> session = openSession();
        for (int attempt = 0; attempt < 5; attempt++) {
            markAttendance("WRONG" + attempt, LOCKED_STUDENT_ID);
        }

        ResponseEntity<Map> response = markAttendance(session.get("code"), LOCKED_STUDENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("TOO_MANY_ATTEMPTS");
    }
}
