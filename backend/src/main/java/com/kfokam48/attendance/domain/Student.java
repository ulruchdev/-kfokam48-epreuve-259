package com.kfokam48.attendance.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "etudiant")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(nullable = false)
    private String nom;

    /** RG3 (Q4): failed code attempts, reset on success. */
    @Column(name = "tentatives_code", nullable = false)
    private int codeAttempts = 0;

    /** RG3 (Q4): lock window end, null when not locked. */
    @Column(name = "bloque_jusqu_a")
    private OffsetDateTime lockedUntil;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPromotionId() { return promotionId; }
    public void setPromotionId(Long promotionId) { this.promotionId = promotionId; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getCodeAttempts() { return codeAttempts; }
    public void setCodeAttempts(int codeAttempts) { this.codeAttempts = codeAttempts; }
    public OffsetDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(OffsetDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
}
