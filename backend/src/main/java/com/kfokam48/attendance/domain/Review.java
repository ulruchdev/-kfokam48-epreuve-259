package com.kfokam48.attendance.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "relecture")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** RG5 (Q6): unique per exercise. */
    @Column(name = "exercice_id", nullable = false, unique = true)
    private Long exerciseId;

    /** RG4 (Q5): must differ from the exercise author (service-level check). */
    @Column(name = "relecteur_id", nullable = false)
    private Long reviewerId;

    /** RG8 (Q9): integer 0-20, null until rendered. */
    @Column(nullable = true)
    private Integer note;

    @Column(nullable = true)
    private String commentaire;

    @Column(nullable = false)
    private boolean rendue = false;

    @Column(name = "rendue_a")
    private OffsetDateTime renderedAt;

    /** RG9 (DEC-1): last amendment timestamp, until session closure. */
    @Column(name = "maj_a")
    private OffsetDateTime amendedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getExerciseId() { return exerciseId; }
    public void setExerciseId(Long exerciseId) { this.exerciseId = exerciseId; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public Integer getNote() { return note; }
    public void setNote(Integer note) { this.note = note; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public boolean isRendered() { return rendue; }
    public void setRendered(boolean rendue) { this.rendue = rendue; }
    public OffsetDateTime getRenderedAt() { return renderedAt; }
    public void setRenderedAt(OffsetDateTime renderedAt) { this.renderedAt = renderedAt; }
    public OffsetDateTime getAmendedAt() { return amendedAt; }
    public void setAmendedAt(OffsetDateTime amendedAt) { this.amendedAt = amendedAt; }
}
