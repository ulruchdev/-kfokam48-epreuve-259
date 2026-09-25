package com.kfokam48.attendance.api;

import com.kfokam48.attendance.repository.AttendanceRepository;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Envelope bug — two students typing the code at the same time: only one appears.
 * Requests are fired truly in parallel (a latch releases both threads at once) and the
 * scenario is repeated, because a race does not show up on every run.
 */
class ConcurrentAttendanceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String LINK = "https://github.com/a/concurrent";

    @Autowired private AttendanceRepository attendances;

    @RepeatedTest(5)
    void should_recordBothStudents_when_theyMarkAttendanceSimultaneously_whileAnExerciseAwaitsAReviewer() throws Exception {
        Map<String, Object> session = openSession();
        markAttendance(session.get("code"), 1L);
        rest.postForEntity("/api/exercices",
                Map.of("sessionId", session.get("id"), "etudiantId", 1L, "lien", LINK), Map.class);   // alone: pending (RG15)

        List<ResponseEntity<Map>> responses = simultaneously(
                () -> markAttendance(session.get("code"), 2L),
                () -> markAttendance(session.get("code"), 3L));

        assertThat(responses).extracting(r -> r.getStatusCode().value()).containsOnly(201);
        assertThat(attendances.findBySessionId(((Number) session.get("id")).longValue())).hasSize(3);
    }

    @RepeatedTest(5)
    void should_recordBothStudents_when_theyMarkAttendanceSimultaneously_inAFreshSession() throws Exception {
        Map<String, Object> session = openSession();

        List<ResponseEntity<Map>> responses = simultaneously(
                () -> markAttendance(session.get("code"), 2L),
                () -> markAttendance(session.get("code"), 3L));

        assertThat(responses).extracting(r -> r.getStatusCode().value()).containsOnly(201);
        assertThat(attendances.findBySessionId(((Number) session.get("id")).longValue())).hasSize(2);
    }

    @RepeatedTest(5)
    void should_answer201Then409_neverA500_when_theSameStudentSubmitsTwiceAtOnce() throws Exception {
        Map<String, Object> session = openSession();

        List<ResponseEntity<Map>> responses = simultaneously(
                () -> markAttendance(session.get("code"), 4L),
                () -> markAttendance(session.get("code"), 4L));

        assertThat(responses).extracting(r -> r.getStatusCode().value()).containsExactlyInAnyOrder(201, 409);
    }

    @SafeVarargs
    private List<ResponseEntity<Map>> simultaneously(java.util.concurrent.Callable<ResponseEntity<Map>>... calls)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.length);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ResponseEntity<Map>>> futures = java.util.Arrays.stream(calls)
                    .map(call -> pool.submit(() -> {
                        start.await();
                        return call.call();
                    }))
                    .toList();
            start.countDown();
            List<ResponseEntity<Map>> results = new java.util.ArrayList<>();
            for (Future<ResponseEntity<Map>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
