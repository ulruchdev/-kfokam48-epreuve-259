package com.kfokam48.attendance.repository;

import com.kfokam48.attendance.domain.CourseSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {

    List<CourseSession> findByPromotionIdOrderByOpenedAtDesc(Long promotionId);

    /** DEC-5: the code is unique among non-closed sessions. */
    Optional<CourseSession> findByCodeAndStatusNot(String code, CourseSession.Status status);

    boolean existsByCodeAndStatusNot(String code, CourseSession.Status status);
}
