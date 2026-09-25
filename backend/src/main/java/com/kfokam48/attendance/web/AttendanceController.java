package com.kfokam48.attendance.web;

import com.kfokam48.attendance.service.AttendanceService;
import com.kfokam48.attendance.web.dto.Dto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /** [IMPOSÉ] POST /api/presences → 201 / 400 / 409 / 410. */
    @PostMapping("/presences")
    public ResponseEntity<Dto.AttendanceResponse> mark(@Valid @RequestBody Dto.MarkAttendanceRequest request) {
        Dto.AttendanceResponse response = attendanceService.mark(request.code(), request.etudiantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
