package com.kfokam48.attendance.web;

import com.kfokam48.attendance.service.DashboardService;
import com.kfokam48.attendance.web.dto.Dto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** [IMPOSÉ] GET /api/tableau?promotionId= → 200 [6 fields per student] / 400 / 404. */
    @GetMapping("/tableau")
    public List<Dto.DashboardRowResponse> dashboard(@RequestParam Long promotionId) {
        return dashboardService.build(promotionId);
    }
}
