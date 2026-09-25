package com.kfokam48.attendance.web;

import com.kfokam48.attendance.service.AttendanceService;
import com.kfokam48.attendance.service.SessionService;
import com.kfokam48.attendance.web.dto.Dto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SessionController {

    private final SessionService sessionService;
    private final AttendanceService attendanceService;

    public SessionController(SessionService sessionService, AttendanceService attendanceService) {
        this.sessionService = sessionService;
        this.attendanceService = attendanceService;
    }

    /** [IMPOSÉ] POST /api/sessions → 201 {id, code, ouvertureAt, expirationAt}(+finAt, statut). */
    @PostMapping("/sessions")
    public ResponseEntity<Dto.SessionOpenedResponse> open(@Valid @RequestBody Dto.OpenSessionRequest request) {
        Dto.SessionOpenedResponse response = sessionService.open(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    /** [LIBRE] GET /api/sessions?promotionId= */
    @GetMapping("/sessions")
    public List<Dto.SessionSummaryResponse> list(@RequestParam Long promotionId) {
        return sessionService.listByPromotion(promotionId);
    }

    /** [LIBRE] GET /api/sessions/{id} */
    @GetMapping("/sessions/{id}")
    public Dto.SessionDetailResponse detail(@PathVariable Long id) {
        return sessionService.getDetail(id);
    }

    /** [LIBRE] PUT /api/sessions/{id} (DEC-2: adjust end time). */
    @PutMapping("/sessions/{id}")
    public Dto.SessionDetailResponse adjustEnd(@PathVariable Long id,
                                               @Valid @RequestBody Dto.AdjustEndTimeRequest request) {
        return sessionService.adjustEnd(id, request);
    }

    /** [LIBRE] POST /api/sessions/{id}/presences → 201 source=FORMATEUR (EF7/RG12). */
    @PostMapping("/sessions/{id}/presences")
    public ResponseEntity<Dto.AttendanceResponse> addManualAttendance(
            @PathVariable Long id, @Valid @RequestBody Dto.AddManualAttendanceRequest request) {
        Dto.AttendanceResponse response = attendanceService.addManually(id, request.etudiantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
