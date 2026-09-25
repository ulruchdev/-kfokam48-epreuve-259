package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.ExerciseStatus;
import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.repository.CourseSessionRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import com.kfokam48.attendance.repository.ReviewRepository;
import com.kfokam48.attendance.web.dto.Dto;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final ExerciseRepository exercises;
    private final CourseSessionRepository sessions;

    public ReviewService(ReviewRepository reviews, ExerciseRepository exercises,
                         CourseSessionRepository sessions) {
        this.reviews = reviews;
        this.exercises = exercises;
        this.sessions = sessions;
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

        exercise.setStatus(ExerciseStatus.REVIEWED);
        exercises.save(exercise);
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

    // ---------- private steps ----------

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
