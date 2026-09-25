package com.kfokam48.attendance.repository;

import com.kfokam48.attendance.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<Attendance> findBySessionId(Long sessionId);

    List<Attendance> findBySessionIdIn(List<Long> sessionIds);
}
