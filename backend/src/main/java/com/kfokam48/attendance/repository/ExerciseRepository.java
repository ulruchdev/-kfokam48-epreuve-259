package com.kfokam48.attendance.repository;

import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.ExerciseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<Exercise> findBySessionId(Long sessionId);

    List<Exercise> findBySessionIdIn(List<Long> sessionIds);

    List<Exercise> findBySessionIdAndStatus(Long sessionId, ExerciseStatus status);

    List<Exercise> findByStudentId(Long studentId);
}
