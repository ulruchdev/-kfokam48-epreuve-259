package com.kfokam48.attendance.repository;

import com.kfokam48.attendance.domain.Exercise;
import com.kfokam48.attendance.domain.ExerciseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<Exercise> findBySessionId(Long sessionId);

    List<Exercise> findBySessionIdIn(List<Long> sessionIds);

    List<Exercise> findBySessionIdAndStatus(Long sessionId, ExerciseStatus status);

    List<Exercise> findByStudentId(Long studentId);

    /**
     * #59: locks the exercise row until the end of the transaction and returns its current
     * status as stored, so concurrent reviewer assignments are serialized (RG5).
     */
    @Query(value = "SELECT statut FROM exercice WHERE id = :id FOR UPDATE", nativeQuery = true)
    String lockAndReadStatus(@Param("id") Long id);
}
