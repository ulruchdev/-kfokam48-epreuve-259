package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.ExerciseStatus;
import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.domain.Student;
import com.kfokam48.attendance.repository.AttendanceRepository;
import com.kfokam48.attendance.repository.CourseSessionRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import com.kfokam48.attendance.repository.ReviewRepository;
import com.kfokam48.attendance.repository.StudentRepository;
import com.kfokam48.attendance.web.dto.Dto;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExerciseService {

    private final CourseSessionRepository sessions;
    private final StudentRepository students;
    private final AttendanceRepository attendances;
    private final ExerciseRepository exercises;
    private final ReviewRepository reviews;
    private final ReviewAssignmentService assignment;

    public ExerciseService(CourseSessionRepository sessions, StudentRepository students,
                           AttendanceRepository attendances, ExerciseRepository exercises,
                           ReviewRepository reviews, ReviewAssignmentService assignment) {
        this.sessions = sessions;
        this.students = students;
        this.attendances = attendances;
        this.exercises = exercises;
        this.reviews = reviews;
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

    /** EF12 / RG11 (Q13): the link is replaceable as long as no review has been rendered. */
    @Transactional
    public Dto.ExerciseResponse replaceLink(Long exerciseId, Dto.ReplaceLinkRequest request) {
        requireValidLink(request.lien());
        Exercise exercise = exercises.findById(exerciseId)
                .orElseThrow(() -> new ExerciseUnknownException(exerciseId));
        requireSessionNotClosed(loadSession(exercise.getSessionId()));
        requireReviewNotStarted(exercise.getId());

        exercise.setLien(request.lien().trim());
        exercise = exercises.save(exercise);
        String authorName = loadStudent(exercise.getStudentId()).getNom();
        return new Dto.ExerciseResponse(exercise.getId(), exercise.getSessionId(), exercise.getStudentId(),
                authorName, exercise.getLien(), exercise.getStatus().code());
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

    /** EF9 / RG7 (Q8): the author sees grade and comment, never who reviewed. */
    @Transactional(readOnly = true)
    public List<Dto.ExerciseWithReviewResponse> listByStudent(Long studentId, Long sessionId) {
        Student author = loadStudent(studentId);
        List<Exercise> own = exercises.findByStudentId(studentId).stream()
                .filter(e -> sessionId == null || e.getSessionId().equals(sessionId))
                .toList();
        Map<Long, List<Review>> reviewsByExercise = own.isEmpty() ? Map.of()
                : reviews.findByExerciseIdIn(own.stream().map(Exercise::getId).toList()).stream()
                        .collect(Collectors.groupingBy(Review::getExerciseId));
        return own.stream()
                .map(e -> new Dto.ExerciseWithReviewResponse(e.getId(), e.getSessionId(), author.getId(),
                        author.getNom(), e.getLien(), e.getStatus().code(),
                        evaluation(reviewsByExercise.getOrDefault(e.getId(), List.of()))))
                .toList();
    }

    // ---------- private steps ----------

    /** RG7 / RG16: retained grade and comments of the rendered reviews, never who wrote them. */
    private Dto.EvaluationResponse evaluation(List<Review> exerciseReviews) {
        List<Review> rendered = exerciseReviews.stream()
                .filter(Review::isRendered)
                .sorted(Comparator.comparing(Review::getRank))
                .toList();
        if (rendered.isEmpty()) return null;
        double retained = rendered.stream().mapToInt(Review::getNote).average().orElseThrow();
        boolean provisional = rendered.size() < ReviewAssignmentService.REVIEWERS_PER_EXERCISE;
        return new Dto.EvaluationResponse(retained, provisional, rendered.stream().map(Review::getCommentaire).toList());
    }

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

    /** RG11: a review has started once its reviewer has rendered it. */
    private void requireReviewNotStarted(Long exerciseId) {
        boolean started = reviews.findByExerciseIdIn(List.of(exerciseId)).stream().anyMatch(Review::isRendered);
        if (started) {
            throw new ReviewStartedException();
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
