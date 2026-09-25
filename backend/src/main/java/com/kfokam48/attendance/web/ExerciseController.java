package com.kfokam48.attendance.web;

import com.kfokam48.attendance.service.ExerciseService;
import com.kfokam48.attendance.web.dto.Dto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    /** [IMPOSÉ] POST /api/exercices → 201 {id, statut} / 400 / 404 / 409. */
    @PostMapping("/exercices")
    public ResponseEntity<Dto.ExerciseSubmittedResponse> submit(@Valid @RequestBody Dto.SubmitExerciseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciseService.submit(request));
    }

    /** [LIBRE] GET /api/sessions/{id}/exercices — trainer view. */
    @GetMapping("/sessions/{id}/exercices")
    public List<Dto.ExerciseResponse> sessionExercises(@PathVariable Long id) {
        return exerciseService.listBySession(id);
    }
}
