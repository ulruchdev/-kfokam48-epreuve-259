package com.kfokam48.attendance.api;

import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.ExerciseStatus;
import com.kfokam48.attendance.repository.CourseSessionRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D4 / D2: every exercise status must be storable under the CHECK constraint of the
 * EXERCICE table (EN_ATTENTE_AFFECTATION, EN_ATTENTE_RELECTURE, RELU).
 */
class ExercisePersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final long DEMO_STUDENT_ID = 1L;

    @Autowired private CourseSessionRepository sessions;
    @Autowired private ExerciseRepository exercises;

    @Test
    void should_persistEveryExerciseStatus_underTheSchemaCheckConstraint_D4() {
        CourseSession session = sessions.save(openSession());

        for (ExerciseStatus status : ExerciseStatus.values()) {
            Exercise saved = exercises.saveAndFlush(exercise(session.getId(), status));
            assertThat(exercises.findById(saved.getId())).get()
                    .extracting(Exercise::getStatus).isEqualTo(status);
            exercises.delete(saved);
        }
    }

    private CourseSession openSession() {
        OffsetDateTime now = OffsetDateTime.now();
        CourseSession session = new CourseSession();
        session.setPromotionId(DEMO_PROMOTION_ID);
        session.setTitre("Persistence test");
        session.setCode("PST" + (System.nanoTime() % 1000));
        session.setOpenedAt(now);
        session.setExpiresAt(now.plusMinutes(15));
        session.setEndsAt(now.plusMinutes(120));
        return session;
    }

    private Exercise exercise(Long sessionId, ExerciseStatus status) {
        Exercise exercise = new Exercise();
        exercise.setSessionId(sessionId);
        exercise.setStudentId(DEMO_STUDENT_ID);
        exercise.setLien("https://github.com/demo/exercise");
        exercise.setStatus(status);
        exercise.setSubmittedAt(OffsetDateTime.now());
        return exercise;
    }
}
