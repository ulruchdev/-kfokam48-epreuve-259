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
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * EF2 / RG1 / RG2 / RG3 / RG15 — the imposed POST /api/presences.
     * RG3: the failed-attempt counter must be committed even though CODE_INCONNU is thrown.
     */
    @Transactional(noRollbackFor = CodeUnknownException.class)
    public Dto.AttendanceResponse mark(String code, Long studentId) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new StudentUnknownException(studentId));

        if (isLocked(student)) {                          // RG3: 2-minute lock window
            throw new TooManyAttemptsException();
        }

        String typedCode = code.trim();
        var session = sessions.findByCodeAndStatusNot(typedCode, CourseSession.Status.CLOTUREE);
        if (session.isEmpty()) {
            requireCodeNotFromClosedSession(typedCode);   // RG2: a real but closed code → 410
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
        attendance = insertOnce(attendance);

        retryPendingAssignments(s.getId());               // RG15: new attendee may unblock

        return new Dto.AttendanceResponse(attendance.getId(), attendance.getSessionId(),
                attendance.getStudentId(), attendance.getSource().name());
    }

    /** EF7 / RG12 (Q14): the trainer adds an attendance entry by hand. */
    @Transactional
    public Dto.AttendanceResponse addManually(Long sessionId, Long studentId) {
        CourseSession s = sessions.findById(sessionId)
                .orElseThrow(() -> new SessionUnknownException(sessionId));
        Student student = students.findById(studentId)
                .orElseThrow(() -> new StudentUnknownException(studentId));

        requireSessionNotClosed(s);
        requireStudentBelongsToSessionPromotion(s, student);
        requireNotAlreadyPresent(sessionId, studentId);

        Attendance attendance = new Attendance();
        attendance.setSessionId(sessionId);
        attendance.setStudentId(studentId);
        attendance.setSource(Source.FORMATEUR);           // RG12: "il faut que ça se voie"
        attendance.setMarkedAt(OffsetDateTime.now());
        attendance = insertOnce(attendance);

        retryPendingAssignments(sessionId);

        return new Dto.AttendanceResponse(attendance.getId(), attendance.getSessionId(),
                attendance.getStudentId(), attendance.getSource().name());
    }

    // ---------- private steps (one thing each — clean code) ----------

    /**
     * #59: two simultaneous requests for the same student both pass the existence check;
     * UNIQUE (session_id, etudiant_id) rejects the second one, which is a 409, not a 500.
     */
    private Attendance insertOnce(Attendance attendance) {
        try {
            return attendances.saveAndFlush(attendance);
        } catch (DataIntegrityViolationException alreadyInserted) {
            throw new AlreadyPresentException();
        }
    }

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

    /** A code that existed but whose session is closed "no longer works" (Q3): not a guess. */
    private void requireCodeNotFromClosedSession(String code) {
        if (sessions.existsByCodeAndStatus(code, CourseSession.Status.CLOTUREE)) {
            throw new CodeExpiredException();
        }
    }

    private void requireNotAlreadyPresent(Long sessionId, Long studentId) {
        if (attendances.existsBySessionIdAndStudentId(sessionId, studentId)) {
            throw new AlreadyPresentException();
        }
    }

    private void requireSessionNotClosed(CourseSession session) {
        if (session.getStatus() == CourseSession.Status.CLOTUREE) {
            throw new SessionClosedException();
        }
    }

    private void requireStudentBelongsToSessionPromotion(CourseSession session, Student student) {
        if (!student.getPromotionId().equals(session.getPromotionId())) {
            throw new StudentNotInPromotionException(student.getId());
        }
    }

    /** RG15 (DEC-3): retry assignments for this session's pending exercises. */
    private void retryPendingAssignments(Long sessionId) {
        exercises.findBySessionIdAndStatus(sessionId, ExerciseStatus.PENDING_ASSIGNMENT)
                .forEach(assignment::tryAssign);
    }
}
