package com.kfokam48.attendance.repository;

import com.kfokam48.attendance.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByReviewerId(Long reviewerId);

    List<Review> findByExerciseIdIn(List<Long> exerciseIds);
}
