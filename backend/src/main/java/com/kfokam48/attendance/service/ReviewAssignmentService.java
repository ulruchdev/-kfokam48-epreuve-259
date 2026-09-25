package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.Attendance;
import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.ExerciseStatus;
import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.repository.AttendanceRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import com.kfokam48.attendance.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RG4/RG5/RG6/RG15/DEC-7 — the SINGLE source of truth for reviewer assignment.
 * Called on exercise submission and on every new attendance (RG15 retry).
 */
@Service
public class ReviewAssignmentService {

    private final AttendanceRepository attendances;
    private final ExerciseRepository exercises;
    private final ReviewRepository reviews;

    public ReviewAssignmentService(AttendanceRepository attendances, ExerciseRepository exercises,
                                   ReviewRepository reviews) {
        this.attendances = attendances;
        this.exercises = exercises;
        this.reviews = reviews;
    }

    /** Try to assign a reviewer to one pending-assignment exercise. Idempotent. */
    @Transactional
    public void tryAssign(Exercise exercise) {
        if (exercise.getStatus() != ExerciseStatus.PENDING_ASSIGNMENT) return;

        List<Long> attendeeIds = attendeeIdsOf(exercise.getSessionId());

        List<Long> eligible = attendeeIds.stream()
                .filter(id -> !id.equals(exercise.getStudentId()))   // RG4/RG6: author excluded
                .toList();
        if (eligible.isEmpty()) return;                              // RG15: stays pending

        Long reviewerId = drawReviewer(eligible);

        Review review = new Review();
        review.setExerciseId(exercise.getId());                      // RG5: one review per exercise
        review.setReviewerId(reviewerId);
        reviews.save(review);

        exercise.setStatus(ExerciseStatus.PENDING_REVIEW);
        exercises.save(exercise);
    }

    /**
     * RG6 (Q7): random draw among eligible attendees.
     * DEC-7: least-loaded first (fewest pending reviews); ties broken at random
     * so the draw remains random among equally loaded candidates.
     */
    Long drawReviewer(List<Long> eligibleIds) {
        List<Long> leastLoaded = leastLoadedCandidates(eligibleIds);
        int index = ThreadLocalRandom.current().nextInt(leastLoaded.size());
        return leastLoaded.get(index);
    }

    private List<Long> leastLoadedCandidates(List<Long> eligibleIds) {
        int minLoad = Integer.MAX_VALUE;
        List<Long> candidates = new ArrayList<>();
        for (Long candidateId : eligibleIds) {
            int load = pendingReviewCount(candidateId);
            if (load < minLoad) {
                minLoad = load;
                candidates.clear();
                candidates.add(candidateId);
            } else if (load == minLoad) {
                candidates.add(candidateId);
            }
        }
        return candidates;
    }

    private int pendingReviewCount(Long reviewerId) {
        return (int) reviews.findByReviewerId(reviewerId).stream()
                .filter(review -> !review.isRendered())
                .count();
    }

    private List<Long> attendeeIdsOf(Long sessionId) {
        return attendances.findBySessionId(sessionId).stream()
                .map(Attendance::getStudentId)
                .toList();
    }
}
