package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.domain.Student;
import com.kfokam48.attendance.repository.AttendanceRepository;
import com.kfokam48.attendance.repository.CourseSessionRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import com.kfokam48.attendance.repository.StudentRepository;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.CodeExpiredException;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.CodeUnknownException;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.TooManyAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * B6 unit test — business rule RG3 (Q4): five wrong code attempts trigger a
 * two-minute lock. Pure unit test: no Spring context, mocks only.
 */
@ExtendWith(MockitoExtension.class)
@Tag("RG3")
class AttendanceLockTest {

    @Mock private CourseSessionRepository sessions;
    @Mock private StudentRepository students;
    @Mock private AttendanceRepository attendances;
    @Mock private ExerciseRepository exercises;
    @Mock private ReviewAssignmentService assignment;

    private AttendanceService service;

    private final Long studentId = 7L;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(sessions, students, attendances, exercises, assignment);
    }

    private Student unlockedStudent() {
        Student s = new Student();
        s.setId(studentId);
        s.setPromotionId(1L);
        s.setNom("Test Student");
        return s;
    }

    private CourseSession openSession() {
        CourseSession s = new CourseSession();
        s.setId(1L);
        s.setPromotionId(1L);
        s.setCode("ABC234");
        s.setOpenedAt(OffsetDateTime.now().minusMinutes(1));
        s.setExpiresAt(OffsetDateTime.now().plusMinutes(14));
        s.setEndsAt(OffsetDateTime.now().plusHours(1));
        s.setStatus(CourseSession.Status.OUVERTE);
        return s;
    }

    @Test
    void should_notLock_when_wrongCode_attemptCount_isBelowFive() {
        Student student = unlockedStudent();
        student.setCodeAttempts(3);
        when(students.findById(studentId)).thenReturn(Optional.of(student));
        when(sessions.findByCodeAndStatusNot("WRONG", CourseSession.Status.CLOTUREE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.mark("WRONG", studentId))
                .isInstanceOf(CodeUnknownException.class);

        assertThat(student.getLockedUntil()).isNull();       // 4th failure: not locked yet
        assertThat(student.getCodeAttempts()).isEqualTo(4);
    }

    @Test
    void should_lockForTwoMinutes_when_wrongCode_attemptCount_reachesFive_RG3() {
        Student student = unlockedStudent();
        student.setCodeAttempts(4);
        when(students.findById(studentId)).thenReturn(Optional.of(student));
        when(sessions.findByCodeAndStatusNot("WRONG", CourseSession.Status.CLOTUREE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.mark("WRONG", studentId))
                .isInstanceOf(CodeUnknownException.class);

        assertThat(student.getLockedUntil()).isNotNull();    // 5th failure: locked
        assertThat(student.getLockedUntil()).isAfter(OffsetDateTime.now().plusMinutes(1));
        assertThat(student.getLockedUntil()).isBefore(OffsetDateTime.now().plusMinutes(3));
        assertThat(student.getCodeAttempts()).isZero();      // counter reset while locked
    }

    @Test
    void should_rejectWithTooManyAttempts_when_student_isCurrentlyLocked_RG3() {
        Student student = unlockedStudent();
        student.setLockedUntil(OffsetDateTime.now().plusMinutes(1));
        when(students.findById(studentId)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.mark("ABC234", studentId))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void should_resetCounter_when_codeEventuallySucceeds() {
        Student student = unlockedStudent();
        student.setCodeAttempts(3);
        when(students.findById(studentId)).thenReturn(Optional.of(student));
        when(sessions.findByCodeAndStatusNot("ABC234", CourseSession.Status.CLOTUREE))
                .thenReturn(Optional.of(openSession()));
        when(attendances.existsBySessionIdAndStudentId(any(), any())).thenReturn(false);
        when(attendances.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(exercises.findBySessionIdAndStatus(any(), any())).thenReturn(java.util.List.of());

        service.mark("ABC234", studentId);

        assertThat(student.getCodeAttempts()).isZero();      // success resets RG3 counter
    }

    @Test
    void should_return410_when_code_isExpired_RG1() {
        Student student = unlockedStudent();
        when(students.findById(studentId)).thenReturn(Optional.of(student));
        CourseSession expired = openSession();
        expired.setExpiresAt(OffsetDateTime.now().minusMinutes(1));    // RG1: past 15 min
        when(sessions.findByCodeAndStatusNot("ABC234", CourseSession.Status.CLOTUREE))
                .thenReturn(Optional.of(expired));
        lenient().when(exercises.findBySessionIdAndStatus(any(), any())).thenReturn(java.util.List.of());

        assertThatThrownBy(() -> service.mark("ABC234", studentId))
                .isInstanceOf(CodeExpiredException.class);
    }
}
