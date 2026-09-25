package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.Attendance;
import com.kfokam48.attendance.domain.Attendance.Source;
import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.domain.Student;
import com.kfokam48.attendance.repository.AttendanceRepository;
import com.kfokam48.attendance.repository.CourseSessionRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import com.kfokam48.attendance.domain.ExerciseStatus;
import com.kfokam48.attendance.repository.StudentRepository;
import com.kfokam48.attendance.web.dto.Dto;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AttendanceService {

    private final CourseSessionRepository sessions;
    private final StudentRepository students;
    private final AttendanceRepository attendances;
    private final ExerciseRepository exercises;
    private final ReviewAssignmentService assignment;

    public AttendanceService(CourseSessionRepository sessions, StudentRepository students,
                             AttendanceRepository attendances, ExerciseRepository exercises,
                             ReviewAssignmentService assignment) {
        this.sessions = sessions;
        this.students = students;
        this.attendances = attendances;
        this.exercises = exercises;
        this.assignment = assignment;
    }

    /** EF2 / RG1 / RG2 / RG3 / RG15 — the imposed POST /api/presences. */
    @Transactional
    public Dto.AttendanceResponse mark(String code, Long studentId) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new StudentUnknownException(studentId));

        if (isLocked(student)) {                          // RG3: 2-minute lock window
            throw new TooManyAttemptsException();
        }

        var session = sessions.findByCodeAndStatusNot(code.trim(), CourseSession.Status.CLOTUREE);
        if (session.isEmpty()) {
            registerFailure(student);                     // RG3: count, lock at the 5th
            throw new CodeUnknownException();
        }
        registerSuccess(student);                         // RG3: reset the counter

        CourseSession s = session.get();
        requireCodeStillValid(s);                         // RG1 + RG2 → 410 CODE_EXPIRE
        requireNotAlreadyPresent(s.getId(), studentId);   // 409 DEJA_PRESENT

        Attendance attendance = new Attendance();
        attendance.setSessionId(s.getId());
        attendance.setStudentId(studentId);
        attendance.setSource(Source.ETUDIANT);
        attendance.setMarkedAt(OffsetDateTime.now());
        attendance = attendances.save(attendance);

        retryPendingAssignments(s.getId());               // RG15: new attendee may unblock

        return new Dto.AttendanceResponse(attendance.getId(), attendance.getSessionId(),
                attendance.getStudentId(), attendance.getSource().name());
    }

    // ---------- private steps (one thing each — clean code) ----------

    private boolean isLocked(Student student) {
        return student.getLockedUntil() != null
                && OffsetDateTime.now().isBefore(student.getLockedUntil());
    }

    /** RG3 (Q4): 5 wrong attempts trigger a 2-minute lock. */
    private void registerFailure(Student student) {
        int attempts = student.getCodeAttempts() + 1;
        if (attempts >= 5) {
            student.setLockedUntil(OffsetDateTime.now().plusMinutes(2));
            student.setCodeAttempts(0);
        } else {
            student.setCodeAttempts(attempts);
        }
        students.save(student);
    }

    private void registerSuccess(Student student) {
        if (student.getCodeAttempts() != 0) {
            student.setCodeAttempts(0);
            students.save(student);
        }
    }

    private void requireCodeStillValid(CourseSession session) {
        OffsetDateTime now = OffsetDateTime.now();
        if (session.endedAt(now) || session.codeExpiredAt(now)) {
            throw new CodeExpiredException();
        }
    }

    private void requireNotAlreadyPresent(Long sessionId, Long studentId) {
        if (attendances.existsBySessionIdAndStudentId(sessionId, studentId)) {
            throw new AlreadyPresentException();
        }
    }

    /** RG15 (DEC-3): retry assignments for this session's pending exercises. */
    private void retryPendingAssignments(Long sessionId) {
        exercises.findBySessionIdAndStatus(sessionId, ExerciseStatus.PENDING_ASSIGNMENT)
                .forEach(assignment::tryAssign);
    }
}
