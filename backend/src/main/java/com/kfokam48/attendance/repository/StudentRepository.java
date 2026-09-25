package com.kfokam48.attendance.repository;

import com.kfokam48.attendance.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByPromotionIdOrderById(Long promotionId);
}
