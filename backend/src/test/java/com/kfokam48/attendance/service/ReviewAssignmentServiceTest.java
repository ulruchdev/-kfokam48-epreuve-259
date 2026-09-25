package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.Attendance;
import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.ExerciseStatus;
import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.repository.AttendanceRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import com.kfokam48.attendance.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the reviewer-draw business rules:
 * RG4 (never the author), RG5 (one reviewer), RG6 (among attendees),
 * RG15 (may stay pending when the author is the sole attendee).
 */
@ExtendWith(MockitoExtension.class)
@Tag("RG4")
@Tag("RG5")
@Tag("RG6")
@Tag("RG15")
class ReviewAssignmentServiceTest {

    @Mock private AttendanceRepository attendances;
    @Mock private ExerciseRepository exercises;
    @Mock private ReviewRepository reviews;

    private ReviewAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new ReviewAssignmentService(attendances, exercises, reviews);
    }

    private Exercise pendingExercise(Long sessionId, Long authorId) {
        Exercise exercise = new Exercise();
        exercise.setId(10L);
        exercise.setSessionId(sessionId);
        exercise.setStudentId(authorId);
        exercise.setStatus(ExerciseStatus.PENDING_ASSIGNMENT);
        return exercise;
    }

    private Attendance attendee(Long sessionId, Long studentId) {
        Attendance attendance = new Attendance();
        attendance.setSessionId(sessionId);
        attendance.setStudentId(studentId);
        return attendance;
    }

    @Test
    void should_keepExercisePending_when_authorIsTheSoleAttendee_RG15() {
        Exercise exercise = pendingExercise(1L, 5L);            // author = student 5
        when(attendances.findBySessionId(1L)).thenReturn(List.of(attendee(1L, 5L)));

        service.tryAssign(exercise);

        verify(reviews, never()).save(any());                    // no reviewer drawn
        assertThat(exercise.getStatus()).isEqualTo(ExerciseStatus.PENDING_ASSIGNMENT);
    }

    @Test
    void should_assignExactlyOneReviewer_amongAttendees_excludingTheAuthor_RG5_RG6_RG4() {
        Exercise exercise = pendingExercise(1L, 5L);            // author = 5
        when(attendances.findBySessionId(1L)).thenReturn(
                List.of(attendee(1L, 5L), attendee(1L, 6L), attendee(1L, 7L)));
        when(reviews.findByReviewerId(any())).thenReturn(List.of());   // everybody unloaded
        when(reviews.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exercises.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.tryAssign(exercise);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviews, times(1)).save(captor.capture());        // RG5: exactly one review row
        Long drawn = captor.getValue().getReviewerId();
        assertThat(drawn).isIn(6L, 7L);                          // RG6/RG4: attendees, author excluded
        assertThat(exercise.getStatus()).isEqualTo(ExerciseStatus.PENDING_REVIEW);
    }

    @Test
    void should_skipAssignment_when_exerciseIsNotPendingAssignment() {
        Exercise exercise = pendingExercise(1L, 5L);
        exercise.setStatus(ExerciseStatus.REVIEWED);

        service.tryAssign(exercise);

        verify(attendances, never()).findBySessionId(any());
        verify(reviews, never()).save(any());
    }
}
