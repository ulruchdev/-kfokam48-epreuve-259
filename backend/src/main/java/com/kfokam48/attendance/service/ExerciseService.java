package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.ExerciseStatus;
import com.kfokam48.attendance.domain.Student;
import com.kfokam48.attendance.repository.AttendanceRepository;
import com.kfokam48.attendance.repository.CourseSessionRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import com.kfokam48.attendance.repository.StudentRepository;
import com.kfokam48.attendance.web.dto.Dto;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExerciseService {

    private final CourseSessionRepository sessions;
    private final StudentRepository students;
    private final AttendanceRepository attendances;
    private final ExerciseRepository exercises;
    private final ReviewAssignmentService assignment;

    public ExerciseService(CourseSessionRepository sessions, StudentRepository students,
                           AttendanceRepository attendances, ExerciseRepository exercises,
                           ReviewAssignmentService assignment) {
        this.sessions = sessions;
        this.students = students;
        this.attendances = attendances;
        this.exercises = exercises;
        this.assignment = assignment;
    }

    /** EF3 / RG10 / RG14 (DEC-4) / RG15 — the imposed POST /api/exercices. */
    @Transactional
    public Dto.ExerciseSubmittedResponse submit(Dto.SubmitExerciseRequest request) {
        requireValidLink(request.lien());
        CourseSession session = loadSession(request.sessionId());
        Student student = loadStudent(request.etudiantId());

        requireSessionNotClosed(session);                              // RG10: open until closure
        requireAttendance(session.getId(), student.getId());           // RG14
        requireNotAlreadySubmitted(session.getId(), student.getId());  // imposed 409

        Exercise exercise = new Exercise();
        exercise.setSessionId(session.getId());
        exercise.setStudentId(student.getId());
        exercise.setLien(request.lien().trim());
        exercise.setStatus(ExerciseStatus.PENDING_ASSIGNMENT);
        exercise.setSubmittedAt(OffsetDateTime.now());
        exercise = exercises.save(exercise);

        assignment.tryAssign(exercise);   // RG6 — may stay PENDING_ASSIGNMENT (RG15)

        return new Dto.ExerciseSubmittedResponse(exercise.getId(), exercise.getStatus().code());
    }

    /** Trainer view: every exercise of a session with its author and status. */
    @Transactional(readOnly = true)
    public List<Dto.ExerciseResponse> listBySession(Long sessionId) {
        loadSession(sessionId);
        List<Exercise> list = exercises.findBySessionId(sessionId);
        Map<Long, String> authorNames = students.findAllById(list.stream().map(Exercise::getStudentId).toList())
                .stream().collect(Collectors.toMap(Student::getId, Student::getNom));
        return list.stream()
                .map(e -> new Dto.ExerciseResponse(e.getId(), e.getSessionId(), e.getStudentId(),
                        authorNames.getOrDefault(e.getStudentId(), ""), e.getLien(), e.getStatus().code()))
                .toList();
    }

    // ---------- private steps ----------

    /** Only absolute http(s) links with a host are accepted (LIEN_INVALIDE). */
    private void requireValidLink(String link) {
        try {
            URI uri = URI.create(link.trim());
            String scheme = uri.getScheme();
            boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!httpScheme || uri.getHost() == null) {
                throw new InvalidLinkException();
            }
        } catch (IllegalArgumentException ex) {
            throw new InvalidLinkException();
        }
    }

    private void requireSessionNotClosed(CourseSession session) {
        if (session.getStatus() == CourseSession.Status.CLOTUREE) {
            throw new SessionClosedException();
        }
    }

    private void requireAttendance(Long sessionId, Long studentId) {
        if (!attendances.existsBySessionIdAndStudentId(sessionId, studentId)) {
            throw new AttendanceRequiredException();
        }
    }

    private void requireNotAlreadySubmitted(Long sessionId, Long studentId) {
        if (exercises.existsBySessionIdAndStudentId(sessionId, studentId)) {
            throw new ExerciseAlreadySubmittedException();
        }
    }

    private CourseSession loadSession(Long id) {
        return sessions.findById(id).orElseThrow(() -> new SessionUnknownException(id));
    }

    private Student loadStudent(Long id) {
        return students.findById(id).orElseThrow(() -> new StudentUnknownException(id));
    }
}
