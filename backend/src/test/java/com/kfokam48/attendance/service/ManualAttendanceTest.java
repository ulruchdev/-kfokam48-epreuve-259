package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.domain.Student;
import com.kfokam48.attendance.repository.AttendanceRepository;
import com.kfokam48.attendance.repository.CourseSessionRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import com.kfokam48.attendance.repository.StudentRepository;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.SessionClosedException;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.StudentNotInPromotionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** EF7 / RG12 — guards of the trainer's manual attendance. */
@ExtendWith(MockitoExtension.class)
@Tag("RG12")
class ManualAttendanceTest {

    @Mock private CourseSessionRepository sessions;
    @Mock private StudentRepository students;
    @Mock private AttendanceRepository attendances;
    @Mock private ExerciseRepository exercises;
    @Mock private ReviewAssignmentService assignment;

    private AttendanceService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(sessions, students, attendances, exercises, assignment);
    }

    @Test
    void should_reject_when_studentBelongsToAnotherPromotion() {
        when(sessions.findById(1L)).thenReturn(Optional.of(session(CourseSession.Status.OUVERTE)));
        when(students.findById(9L)).thenReturn(Optional.of(student(9L, 2L)));

        assertThatThrownBy(() -> service.addManually(1L, 9L))
                .isInstanceOf(StudentNotInPromotionException.class);
    }

    @Test
    void should_reject_when_sessionIsClosed() {
        when(sessions.findById(1L)).thenReturn(Optional.of(session(CourseSession.Status.CLOTUREE)));
        when(students.findById(9L)).thenReturn(Optional.of(student(9L, 1L)));

        assertThatThrownBy(() -> service.addManually(1L, 9L))
                .isInstanceOf(SessionClosedException.class);
    }

    private CourseSession session(CourseSession.Status status) {
        CourseSession session = new CourseSession();
        session.setId(1L);
        session.setPromotionId(1L);
        session.setStatus(status);
        return session;
    }

    private Student student(Long id, Long promotionId) {
        Student student = new Student();
        student.setId(id);
        student.setPromotionId(promotionId);
        return student;
    }
}
