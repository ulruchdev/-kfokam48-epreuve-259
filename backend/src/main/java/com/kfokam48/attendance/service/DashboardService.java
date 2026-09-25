package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.Attendance;
import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.Review;
import com.kfokam48.attendance.domain.Student;
import com.kfokam48.attendance.repository.AttendanceRepository;
import com.kfokam48.attendance.repository.CourseSessionRepository;
import com.kfokam48.attendance.repository.ExerciseRepository;
import com.kfokam48.attendance.repository.PromotionRepository;
import com.kfokam48.attendance.repository.ReviewRepository;
import com.kfokam48.attendance.repository.StudentRepository;
import com.kfokam48.attendance.web.dto.Dto;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.PromotionUnknownException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final PromotionRepository promotions;
    private final StudentRepository students;
    private final CourseSessionRepository sessions;
    private final AttendanceRepository attendances;
    private final ExerciseRepository exercises;
    private final ReviewRepository reviews;

    public DashboardService(PromotionRepository promotions, StudentRepository students,
                            CourseSessionRepository sessions, AttendanceRepository attendances,
                            ExerciseRepository exercises, ReviewRepository reviews) {
        this.promotions = promotions;
        this.students = students;
        this.sessions = sessions;
        this.attendances = attendances;
        this.exercises = exercises;
        this.reviews = reviews;
    }

    /**
     * EF6 / Q16 — per student: attendance count, submitted exercises, average of the
     * current received grades (null when none, DEC-8), reviews still to do (Q11/Q16) and
     * attendance added by the trainer (EF7, Q14).
     * A fixed number of queries whatever the promotion size (ENF2); the API computes the
     * average, the frontend never does (F3).
     */
    @Transactional(readOnly = true)
    public List<Dto.DashboardRowResponse> build(Long promotionId) {
        requirePromotionExists(promotionId);
        List<Student> roster = students.findByPromotionIdOrderById(promotionId);
        List<Long> sessionIds = sessions.findByPromotionIdOrderByOpenedAtDesc(promotionId).stream()
                .map(CourseSession::getId).toList();

        List<Attendance> promotionAttendances = sessionIds.isEmpty() ? List.of() : attendances.findBySessionIdIn(sessionIds);
        List<Exercise> promotionExercises = sessionIds.isEmpty() ? List.of() : exercises.findBySessionIdIn(sessionIds);
        List<Review> promotionReviews = promotionExercises.isEmpty() ? List.of()
                : reviews.findByExerciseIdIn(promotionExercises.stream().map(Exercise::getId).toList());

        Map<Long, Long> attendanceByStudent = countBy(promotionAttendances, Attendance::getStudentId);
        Map<Long, Long> trainerAttendanceByStudent = countBy(promotionAttendances.stream()
                .filter(attendance -> attendance.getSource() == Attendance.Source.FORMATEUR).toList(), Attendance::getStudentId);
        Map<Long, Long> submissionsByStudent = countBy(promotionExercises, Exercise::getStudentId);
        Map<Long, Long> pendingByReviewer = countBy(
                promotionReviews.stream().filter(review -> !review.isRendered()).toList(), Review::getReviewerId);
        Map<Long, Double> averageByAuthor = averageGradeByAuthor(promotionExercises, promotionReviews);

        return roster.stream()
                .map(student -> new Dto.DashboardRowResponse(student.getId(), student.getNom(),
                        attendanceByStudent.getOrDefault(student.getId(), 0L).intValue(),
                        submissionsByStudent.getOrDefault(student.getId(), 0L).intValue(),
                        averageByAuthor.get(student.getId()),
                        pendingByReviewer.getOrDefault(student.getId(), 0L).intValue(),
                        trainerAttendanceByStudent.getOrDefault(student.getId(), 0L).intValue()))
                .toList();
    }

    // ---------- private steps ----------

    private void requirePromotionExists(Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw new PromotionUnknownException(promotionId);
        }
    }

    private static <T> Map<Long, Long> countBy(List<T> items, Function<T, Long> key) {
        return items.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
    }

    /** DEC-8 / RG9: average of the current rendered grades, per exercise author. */
    private static Map<Long, Double> averageGradeByAuthor(List<Exercise> exerciseList, List<Review> reviewList) {
        Map<Long, Long> authorByExercise = exerciseList.stream()
                .collect(Collectors.toMap(Exercise::getId, Exercise::getStudentId));
        return reviewList.stream()
                .filter(review -> review.isRendered() && review.getNote() != null)
                .collect(Collectors.groupingBy(review -> authorByExercise.get(review.getExerciseId()),
                        Collectors.averagingInt(Review::getNote)));
    }
}
