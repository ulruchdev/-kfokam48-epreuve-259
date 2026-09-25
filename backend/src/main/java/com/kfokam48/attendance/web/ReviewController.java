package com.kfokam48.attendance.web;

import com.kfokam48.attendance.service.ReviewService;
import com.kfokam48.attendance.web.dto.Dto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** [IMPOSÉ] POST /api/relectures/{id} → 200 / 400 / 403 / 404 / 409. */
    @PostMapping("/relectures/{id}")
    public void render(@PathVariable Long id, @Valid @RequestBody Dto.SubmitReviewRequest request) {
        reviewService.render(id, request);
    }

    /** [LIBRE] PUT /api/relectures/{id} → 200 Relecture (EF11, RG9, DEC-1). */
    @PutMapping("/relectures/{id}")
    public Dto.ReviewResponse amend(@PathVariable Long id, @Valid @RequestBody Dto.SubmitReviewRequest request) {
        return reviewService.amend(id, request);
    }
}
