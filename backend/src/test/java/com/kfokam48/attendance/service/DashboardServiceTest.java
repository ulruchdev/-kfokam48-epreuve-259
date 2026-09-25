package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.domain.Student;
import com.kfokam48.attendance.repository.*;
import com.kfokam48.attendance.web.dto.Dto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/** ENF2 — the dashboard issues a fixed number of queries, whatever the promotion size. */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private PromotionRepository promotions;
    @Mock private StudentRepository students;
    @Mock private CourseSessionRepository sessions;
    @Mock private AttendanceRepository attendances;
    @Mock private ExerciseRepository exercises;
    @Mock private ReviewRepository reviews;

    @Test
    void should_notQueryPerStudent_when_buildingTheDashboardOfSixtyStudents_ENF2() {
        when(promotions.existsById(1L)).thenReturn(true);
        when(students.findByPromotionIdOrderById(1L)).thenReturn(roster(60));
        when(sessions.findByPromotionIdOrderByOpenedAtDesc(1L)).thenReturn(List.of(session(10L)));
        when(attendances.findBySessionIdIn(anyList())).thenReturn(List.of());
        when(exercises.findBySessionIdIn(anyList())).thenReturn(List.of(exercise(100L, 1L)));
        when(reviews.findByExerciseIdIn(anyList())).thenReturn(List.of(pendingReview(100L, 2L)));

        List<Dto.DashboardRowResponse> rows = service().build(1L);

        assertThat(rows).hasSize(60);
        assertThat(rows.get(1).relecturesEnAttente()).isEqualTo(1);   // student 2 must review exercise 100
        assertThat(rows.get(0).relecturesEnAttente()).isZero();       // the author has nothing to review
        verify(reviews, times(1)).findByExerciseIdIn(anyList());
        verify(reviews, never()).findByReviewerId(any());
    }

    private DashboardService service() {
        return new DashboardService(promotions, students, sessions, attendances, exercises, reviews);
    }

    private List<Student> roster(int size) {
        return LongStream.rangeClosed(1, size).mapToObj(id -> {
            Student student = new Student();
            student.setId(id);
            student.setPromotionId(1L);
            student.setNom("Student " + id);
            return student;
        }).toList();
    }

    private CourseSession session(Long id) {
        CourseSession session = new CourseSession();
        session.setId(id);
        return session;
    }

    private Exercise exercise(Long id, Long authorId) {
        Exercise exercise = new Exercise();
        exercise.setId(id);
        exercise.setStudentId(authorId);
        return exercise;
    }

    private Review pendingReview(Long exerciseId, Long reviewerId) {
        Review review = new Review();
        review.setExerciseId(exerciseId);
        review.setReviewerId(reviewerId);
        return review;
    }
}
