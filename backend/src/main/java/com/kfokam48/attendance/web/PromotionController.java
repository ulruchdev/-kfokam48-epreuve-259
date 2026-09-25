package com.kfokam48.attendance.web;

import com.kfokam48.attendance.repository.PromotionRepository;
import com.kfokam48.attendance.repository.StudentRepository;
import com.kfokam48.attendance.web.dto.Dto;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.PromotionUnknownException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PromotionController {

    private final PromotionRepository promotions;
    private final StudentRepository students;

    public PromotionController(PromotionRepository promotions, StudentRepository students) {
        this.promotions = promotions;
        this.students = students;
    }

    /** [LIBRE] GET /api/promotions — EF10 (Q1): identity by list. */
    @GetMapping("/promotions")
    public List<Dto.PromotionResponse> promotions() {
        return promotions.findAll().stream()
                .map(p -> new Dto.PromotionResponse(p.getId(), p.getNom()))
                .toList();
    }

    /** [LIBRE] GET /api/promotions/{promotionId}/etudiants — EF10 (Q1). */
    @GetMapping("/promotions/{promotionId}/etudiants")
    public List<Dto.StudentResponse> students(@PathVariable Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw new PromotionUnknownException(promotionId);
        }
        return students.findByPromotionIdOrderById(promotionId).stream()
                .map(s -> new Dto.StudentResponse(s.getId(), s.getNom()))
                .toList();
    }
}
