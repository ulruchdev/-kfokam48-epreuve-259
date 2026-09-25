package com.kfokam48.attendance.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "session")
public class CourseSession {

    /** Imposed contract values (statut): OUVERTE, TERMINEE, CLOTUREE. */
    public enum Status { OUVERTE, TERMINEE, CLOTUREE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String code;

    @Column(name = "ouverture_at", nullable = false)
    private OffsetDateTime openedAt;

    /** RG1 (Q2): openedAt + 15 minutes. */
    @Column(name = "expiration_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** DEC-2: openedAt + durationMinutes (default 120). */
    @Column(name = "fin_at", nullable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private Status status = Status.OUVERTE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPromotionId() { return promotionId; }
    public void setPromotionId(Long promotionId) { this.promotionId = promotionId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public OffsetDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(OffsetDateTime openedAt) { this.openedAt = openedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime endsAt) { this.endsAt = endsAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    /** RG1: the code works only during the first 15 minutes. */
    public boolean codeExpiredAt(OffsetDateTime now) { return now.isAfter(expiresAt); }

    /** RG2: no attendance after the session end. */
    public boolean endedAt(OffsetDateTime now) { return now.isAfter(endsAt); }
}
