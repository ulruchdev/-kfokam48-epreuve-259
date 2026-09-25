package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.ExerciseStatus;
import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.domain.Student;
import com.kfokam48.attendance.repository.CourseSessionRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import com.kfokam48.attendance.repository.ReviewRepository;
import com.kfokam48.attendance.repository.StudentRepository;
import com.kfokam48.attendance.web.dto.Dto;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final ExerciseRepository exercises;
    private final CourseSessionRepository sessions;
    private final StudentRepository students;

    public ReviewService(ReviewRepository reviews, ExerciseRepository exercises,
                         CourseSessionRepository sessions, StudentRepository students) {
        this.reviews = reviews;
        this.exercises = exercises;
        this.sessions = sessions;
        this.students = students;
    }

    /**
     * EF5 / RG4 / RG8 / RG9 — the imposed POST /api/relectures/{id}.
     * {@code relecteurId} is optional (additive extension): when given, the caller must be
     * the assignee and never the author; when absent the review is rendered by its assignee.
     */
    @Transactional
    public void render(Long reviewId, Dto.SubmitReviewRequest request) {
        Review review = loadReview(reviewId);
        Exercise exercise = loadExercise(review.getExerciseId());

        if (request.relecteurId() != null) {
            requireCallerIsNotAuthor(exercise, request.relecteurId());   // RG4 → 403 AUTO_RELECTURE
            requireCallerIsAssignee(review, request.relecteurId());      // 403 RELECTURE_NON_ASSIGNEE
        }
        requireNotAlreadyRendered(review);                               // imposed 409
        requireSessionNotClosed(exercise);                               // RG9 → 409

        review.setNote(validGrade(request.note()));                      // RG8
        review.setCommentaire(request.commentaire().trim());
        review.setRendered(true);
        review.setRenderedAt(OffsetDateTime.now());
        reviews.save(review);

        markReviewedWhenBothRendered(exercise);                          // RG16
    }

    /** EF11 / RG9 / DEC-1 (Q10 over Q15): a rendered review is amendable until closure. */
    @Transactional
    public Dto.ReviewResponse amend(Long reviewId, Dto.SubmitReviewRequest request) {
        Review review = loadReview(reviewId);
        Exercise exercise = loadExercise(review.getExerciseId());

        if (request.relecteurId() != null) {
            requireCallerIsAssignee(review, request.relecteurId());
        }
        requireRendered(review);
        requireSessionNotClosed(exercise);

        review.setNote(validGrade(request.note()));
        review.setCommentaire(request.commentaire().trim());
        review.setAmendedAt(OffsetDateTime.now());
        review = reviews.save(review);

        return new Dto.ReviewResponse(review.getId(), review.getExerciseId(), review.getReviewerId(),
                review.getNote(), review.getCommentaire(), review.isRendered());
    }

    /** EF13: the reviewer's assignments with link and author, pending first. Three queries. */
    @Transactional(readOnly = true)
    public List<Dto.AssignedReviewResponse> listForReviewer(Long reviewerId) {
        if (!students.existsById(reviewerId)) {
            throw new StudentUnknownException(reviewerId);
        }
        List<Review> assigned = reviews.findByReviewerId(reviewerId);
        Map<Long, Exercise> exerciseById = exercises.findAllById(assigned.stream().map(Review::getExerciseId).toList())
                .stream().collect(Collectors.toMap(Exercise::getId, exercise -> exercise));
        Map<Long, String> authorName = students.findAllById(exerciseById.values().stream().map(Exercise::getStudentId).toList())
                .stream().collect(Collectors.toMap(Student::getId, Student::getNom));

        return assigned.stream()
                .sorted(Comparator.comparing(Review::isRendered))
                .map(review -> {
                    Exercise exercise = exerciseById.get(review.getExerciseId());
                    return new Dto.AssignedReviewResponse(review.getId(), review.getExerciseId(), exercise.getLien(),
                            authorName.get(exercise.getStudentId()), review.isRendered(),
                            review.getNote(), review.getCommentaire());
                })
                .toList();
    }

    // ---------- private steps ----------

    /** RG16: the exercise is RELU once its two reviews are rendered; before, its grade is provisional. */
    private void markReviewedWhenBothRendered(Exercise exercise) {
        List<Review> all = reviews.findByExerciseIdIn(List.of(exercise.getId()));
        boolean bothRendered = all.size() == ReviewAssignmentService.REVIEWERS_PER_EXERCISE
                && all.stream().allMatch(Review::isRendered);
        if (bothRendered) {
            exercise.setStatus(ExerciseStatus.REVIEWED);
            exercises.save(exercise);
        }
    }

    private void requireRendered(Review review) {
        if (!review.isRendered()) {
            throw new ReviewNotRenderedException();
        }
    }


    private void requireCallerIsNotAuthor(Exercise exercise, Long callerId) {
        if (exercise.getStudentId().equals(callerId)) {
            throw new SelfReviewException();
        }
    }

    private void requireCallerIsAssignee(Review review, Long callerId) {
        if (!review.getReviewerId().equals(callerId)) {
            throw new ReviewNotAssignedException();
        }
    }

    private void requireNotAlreadyRendered(Review review) {
        if (review.isRendered()) {
            throw new ReviewAlreadyRenderedException();
        }
    }

    private void requireSessionNotClosed(Exercise exercise) {
        CourseSession session = sessions.findById(exercise.getSessionId())
                .orElseThrow(() -> new SessionUnknownException(exercise.getSessionId()));
        if (session.getStatus() == CourseSession.Status.CLOTUREE) {
            throw new SessionClosedException();
        }
    }

    /** RG8 (Q9): integer between 0 and 20 (non-integers are rejected at deserialization). */
    private Integer validGrade(Integer note) {
        if (note < 0 || note > 20) {
            throw new InvalidGradeException();
        }
        return note;
    }

    private Review loadReview(Long id) {
        return reviews.findById(id).orElseThrow(() -> new ReviewUnknownException(id));
    }

    private Exercise loadExercise(Long id) {
        return exercises.findById(id).orElseThrow(() -> new ExerciseUnknownException(id));
    }
}
